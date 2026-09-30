package com.fc.v2.service.flog;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fc.v2.common.exception.biz.FlogBizException;
import com.fc.v2.mapper.auto.TFlogYieldRuleMapper;
import com.fc.v2.model.auto.TFlogYieldRule;
import com.fc.v2.service.ITFlogYieldRuleService;
import com.fc.v2.service.impl.TFlogYieldRuleServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 界册取界口径自测：H2 内存库 + 真实 MyBatis-Plus mapper。
 * 守的是“取界只走一处”：调查日取在场界、交棒日不含、并线唯一、让位条不现、空界不设档、等号取高。
 */
class FlogYieldRuleServiceTest {

    private DataSource ds;
    private TFlogYieldRuleMapper mapper;
    private TFlogYieldRuleServiceImpl service;

    /** 商品林年初旧版：1/1 开口、9/1 交棒（不含）；新版 9/1 开口至今 */
    private Long idOld;
    private Long idNew;

    @BeforeEach
    void setUp() throws Exception {
        org.h2.Driver.load();
        ds = org.h2.jdbcx.JdbcConnectionPool.create(
                "jdbc:h2:mem:yieldrule;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("DROP TABLE IF EXISTS t_flog_yield_rule");
            st.execute("CREATE TABLE t_flog_yield_rule ("
                    + "id bigint PRIMARY KEY, rule_code varchar(64), rule_name varchar(128),"
                    + "scope_kind int, th1_max decimal(12,2), th2_max decimal(12,2), th3_max decimal(12,2),"
                    + "eff_start datetime, eff_end datetime, priority int, status int, del_flag int DEFAULT 0,"
                    + "create_by varchar(64), create_time datetime, update_by varchar(64),"
                    + "update_time datetime, remark varchar(500))");
        }
        MybatisConfiguration cfg = new MybatisConfiguration();
        cfg.addMapper(TFlogYieldRuleMapper.class);
        MybatisSqlSessionFactoryBean fb = new MybatisSqlSessionFactoryBean();
        fb.setDataSource(ds);
        fb.setConfiguration(cfg);
        fb.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig()));
        SqlSessionFactory sf = fb.getObject();
        SqlSession session = sf.openSession(true);
        mapper = session.getMapper(TFlogYieldRuleMapper.class);

        service = new TFlogYieldRuleServiceImpl();
        java.lang.reflect.Field f = TFlogYieldRuleServiceImpl.class.getDeclaredField("flogYieldRuleMapper");
        f.setAccessible(true);
        f.set(service, mapper);

        idOld = insertRule("SP-2026A", "商品林年初版", 1, "20", "40", "80",
                "2026-01-01 00:00:00", "2026-09-01 00:00:00", 100, 1);
        idNew = insertRule("SP-2026B", "商品林秋换版", 1, "25", "50", "100",
                "2026-09-01 00:00:00", null, 100, 0);
        // 公益林低改：重界原本空着（纸质簿录入口径）
        insertRule("GY-DG-2026", "公益林低改", 4, "10", "20", null,
                "2026-01-01 00:00:00", null, 100, 0);
        // 归类补录前的旧线：scope_kind 空，不参与取界
        insertRule("LEGACY-1", "未归类旧线", null, "1", "2", "3",
                "2026-01-01 00:00:00", null, 999, 0);
    }

    @AfterEach
    void tearDown() {
        ((org.h2.jdbcx.JdbcConnectionPool) ds).dispose();
    }

    private Long insertRule(String code, String name, Integer scope, String th1, String th2, String th3,
                            String start, String end, int priority, int status) throws Exception {
        TFlogYieldRule r = new TFlogYieldRule();
        r.setRuleCode(code);
        r.setRuleName(name);
        r.setScopeKind(scope);
        r.setTh1Max(th1 == null ? null : new BigDecimal(th1));
        r.setTh2Max(th2 == null ? null : new BigDecimal(th2));
        r.setTh3Max(th3 == null ? null : new BigDecimal(th3));
        r.setEffStart(Timestamp.valueOf(start));
        r.setEffEnd(end == null ? null : Timestamp.valueOf(end));
        r.setPriority(priority);
        r.setStatus(status);
        r.setDelFlag(0);
        mapper.insert(r);
        return r.getId();
    }

    private Timestamp t(String s) {
        return Timestamp.valueOf(s);
    }

    /** 1. 取界认调查设计那一日：去年秋天的设计今年补结论，请的是当时在场那条，不认今天。 */
    @Test
    void testRulePickedBySurveyDateNotToday() {
        TFlogYieldRule aug = service.pickAvailable(1, t("2026-08-31 12:00:00"));
        assertNotNull(aug);
        assertEquals("SP-2026A", aug.getRuleCode());
        TFlogYieldRule sep = service.pickAvailable(1, t("2026-09-01 00:00:00"));
        assertNotNull(sep);
        assertEquals("SP-2026B", sep.getRuleCode());
    }

    /** 2. 交棒之日不含：8/31 23:59:59 仍是旧条，9/1 00:00:00 整已是新条。 */
    @Test
    void testHandoverDayExclusive() {
        assertEquals(2, service.evaluate("SP-2026A", new BigDecimal("20"), t("2026-08-31 23:59:59")));
        // 与轻界 20 一样高 -> 中
        assertEquals(ITFlogYieldRuleService.GRADE_NONE,
                service.evaluate("SP-2026A", new BigDecimal("20"), t("2026-09-01 00:00:00")),
                "交棒当日旧条不在场，按代号请旧线须请不到");
        assertTrue(service.usable(idOld, t("2026-08-31 23:59:59")));
        assertFalse(service.usable(idOld, t("2026-09-01 00:00:00")));
    }

    /** 3. 让位条只供回查：现行求值、在场列表、角标计数里再见不到它。 */
    @Test
    void testHandedOverHiddenFromEvaluation() {
        // 8/31 商品林在场的是年初旧版一条
        List<TFlogYieldRule> aug = service.listAvailable(1, t("2026-08-31 00:00:00"));
        assertEquals(1, aug.size());
        assertEquals("SP-2026A", aug.get(0).getRuleCode());
        // 9/2 在场的只有新版一条；旧版在册（selectById 翻得着来历）但不在场
        List<TFlogYieldRule> sep = service.listAvailable(1, t("2026-09-02 00:00:00"));
        assertEquals(1, sep.size());
        assertEquals("SP-2026B", sep.get(0).getRuleCode());
        // 全场在场条数：商品林新版 + 公益林低改一条 = 2；让位条与未归类旧线都不计
        assertEquals(2, service.countAvailable(t("2026-09-02 00:00:00")));
        assertNotNull(service.selectTFlogYieldRuleById(idOld));
    }

    /** 4. 两条界同时靠上：序位大的说话。 */
    @Test
    void testPriorityWins() throws Exception {
        insertRule("SP-HI", "高序位商品林界", 1, "30", "60", "120",
                "2026-06-01 00:00:00", null, 200, 0);
        // 7/1 三条线名义在场（旧版 + 新版未开口不算，实际旧版+SP-HI）；序位 200 的 SP-HI 说话
        TFlogYieldRule picked = service.pickAvailable(1, t("2026-07-01 00:00:00"));
        assertEquals("SP-HI", picked.getRuleCode());
        // 50 在 SP-HI 是中度（<60），在旧版是重（>=40）；开口的是 SP-HI -> 中
        assertEquals(ITFlogYieldRuleService.GRADE_MIDDLE,
                service.evaluate(1, new BigDecimal("50"), t("2026-07-01 00:00:00")));
    }

    /** 5. 序位也分不出高下：认代号排在后头（字典序靠后）那条。 */
    @Test
    void testCodeBreaksPriorityTie() throws Exception {
        insertRule("SP-A", "并列低代号", 1, "20", "40", "80",
                "2026-06-01 00:00:00", null, 100, 0);
        insertRule("SP-B", "并列高代号", 1, "20", "40", "80",
                "2026-06-01 00:00:00", null, 100, 0);
        // 7/1 在场：旧版SP-2026A(priority100) + SP-A + SP-B，三者 priority 都是 100，代号字典序 SP-B 最大
        assertEquals("SP-B", service.pickAvailable(1, t("2026-07-01 00:00:00")).getRuleCode());
    }

    /** 6. 挨着界往高里数：等于中界落重；越过重界统统算重，不开第 4 档。 */
    @Test
    void testBoundaryRoundsUp() {
        assertEquals(1, service.evaluate(1, new BigDecimal("24.99"), t("2026-09-01 00:00:00")));
        assertEquals(2, service.evaluate(1, new BigDecimal("25"), t("2026-09-01 00:00:00")),
                "与轻界一样高落中");
        assertEquals(2, service.evaluate(1, new BigDecimal("49.99"), t("2026-09-01 00:00:00")));
        assertEquals(3, service.evaluate(1, new BigDecimal("50"), t("2026-09-01 00:00:00")),
                "与中界一样高落重");
        assertEquals(3, service.evaluate(1, new BigDecimal("100"), t("2026-09-01 00:00:00")));
        assertEquals(3, service.evaluate(1, new BigDecimal("999"), t("2026-09-01 00:00:00")),
                "越过重界也统统算重");
    }

    /** 7. 重界空着 = 不设重档上界，跟着中界往下落：到中界即重。 */
    @Test
    void testNullHeavyThresholdFallsThrough() {
        assertEquals(2, service.evaluate(4, new BigDecimal("15"), t("2026-09-01 00:00:00")));
        assertEquals(3, service.evaluate(4, new BigDecimal("20"), t("2026-09-01 00:00:00")),
                "重界空着，与中界一样高即落重");
        assertEquals(3, service.evaluate(4, new BigDecimal("500"), t("2026-09-01 00:00:00")),
                "重界不设上界，越过多少都算重");
    }

    /** 8. 翻不出在场界：返回 0 / null，写明无线可依，不拿邻近日子的线凑。 */
    @Test
    void testNoRuleDoesNotFallBackToNearbyDate() throws Exception {
        // 公益林抚育在 2025 年（任何界都还没启用）：无线可依
        insertRule("GY-FY-2026", "公益林抚育", 2, "15", "30", "60",
                "2026-01-01 00:00:00", null, 100, 0);
        assertNull(service.pickAvailable(2, t("2025-12-31 23:59:59")));
        assertEquals(ITFlogYieldRuleService.GRADE_NONE,
                service.evaluate(2, new BigDecimal("20"), t("2025-12-31 23:59:59")));
        // 未归类旧线：scope 过滤下请不出来，宁可以为这一类无线可依
        assertNull(service.pickAvailable(3, t("2026-05-01 00:00:00")),
                "公益林更新无线；未归类旧线不许拿来凑");
    }

    /** 9. 交棒维护口：旧条置让位、钉交棒日（不含）；交棒日前仍在场，交棒日起不在场；重复交棒、日子倒置当场挡。 */
    @Test
    void testHandoverMaintenance() {
        TFlogYieldRule moved = service.handover(idNew, t("2027-01-01 00:00:00"));
        assertEquals(1, moved.getStatus().intValue());
        assertTrue(service.usable(idNew, t("2026-12-31 23:59:59")), "交棒日之前仍管得到");
        assertFalse(service.usable(idNew, t("2027-01-01 00:00:00")), "交棒日当天起不在场");
        assertThrows(FlogBizException.class, () -> service.handover(idNew, t("2027-02-01 00:00:00")));
        assertThrows(FlogBizException.class, () -> service.handover(idOld, t("2025-01-01 00:00:00")),
                "交棒日早于启用日挡下");
    }

    /** 10. 立线校验：类别/启用日缺不得，界倒挂立不住。 */
    @Test
    void testRegisterValidation() {
        TFlogYieldRule bad = new TFlogYieldRule();
        bad.setRuleCode("X");
        assertThrows(FlogBizException.class, () -> service.registerRule(bad));
        TFlogYieldRule inverted = new TFlogYieldRule();
        inverted.setRuleCode("X1");
        inverted.setRuleName("倒挂");
        inverted.setScopeKind(1);
        inverted.setEffStart(t("2026-01-01 00:00:00"));
        inverted.setTh1Max(new BigDecimal("50"));
        inverted.setTh2Max(new BigDecimal("20"));
        assertThrows(FlogBizException.class, () -> service.registerRule(inverted));

        TFlogYieldRule ok = new TFlogYieldRule();
        ok.setRuleCode("X2");
        ok.setRuleName("成");
        ok.setScopeKind(3);
        ok.setEffStart(t("2026-03-01 00:00:00"));
        ok.setTh1Max(new BigDecimal("18"));
        ok.setPriority(1);
        TFlogYieldRule saved = service.registerRule(ok);
        assertEquals(0, saved.getStatus().intValue(), "新界一律现行入场");
    }
}
