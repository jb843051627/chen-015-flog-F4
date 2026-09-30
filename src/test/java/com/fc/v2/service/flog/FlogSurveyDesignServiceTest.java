package com.fc.v2.service.flog;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fc.v2.common.exception.biz.FlogBizException;
import com.fc.v2.mapper.auto.TFlogSurveyDesignMapper;
import com.fc.v2.mapper.auto.TFlogSurveyTreeMapper;
import com.fc.v2.mapper.auto.TFlogVolumeTableMapper;
import com.fc.v2.mapper.auto.TFlogYieldRuleMapper;
import com.fc.v2.model.auto.TFlogSurveyDesign;
import com.fc.v2.model.auto.TFlogSurveyTree;
import com.fc.v2.model.auto.TFlogVolumeTable;
import com.fc.v2.model.auto.TFlogYieldRule;
import com.fc.v2.model.custom.flog.FlogSurveyDesignDto;
import com.fc.v2.model.custom.flog.FlogSurveyDesignVo;
import com.fc.v2.model.custom.flog.FlogSurveyTreeDto;
import com.fc.v2.model.custom.flog.FlogYieldEvalVo;
import com.fc.v2.service.impl.TFlogSurveyDesignServiceImpl;
import com.fc.v2.service.impl.TFlogVolumeTableServiceImpl;
import com.fc.v2.service.impl.TFlogYieldRuleServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 三级链唯一求值口自测：检尺→材积表→蓄积→每公顷出材→按调查日取界定档→钉单/复验升版。
 */
class FlogSurveyDesignServiceTest {

    private DataSource ds;
    private TFlogYieldRuleMapper ruleMapper;
    private TFlogVolumeTableMapper volumeMapper;
    private TFlogSurveyDesignMapper designMapper;
    private TFlogSurveyTreeMapper treeMapper;
    private TFlogSurveyDesignServiceImpl service;
    private TFlogVolumeTableServiceImpl volumeService;

    @BeforeEach
    void setUp() throws Exception {
        org.h2.Driver.load();
        ds = org.h2.jdbcx.JdbcConnectionPool.create(
                "jdbc:h2:mem:sdesign;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("DROP TABLE IF EXISTS t_flog_yield_rule");
            st.execute("DROP TABLE IF EXISTS t_flog_volume_table");
            st.execute("DROP TABLE IF EXISTS t_flog_survey_design");
            st.execute("DROP TABLE IF EXISTS t_flog_survey_tree");
            st.execute("CREATE TABLE t_flog_yield_rule ("
                    + "id bigint PRIMARY KEY, rule_code varchar(64), rule_name varchar(128),"
                    + "scope_kind int, th1_max decimal(12,2), th2_max decimal(12,2), th3_max decimal(12,2),"
                    + "eff_start datetime, eff_end datetime, priority int, status int, del_flag int DEFAULT 0,"
                    + "create_by varchar(64), create_time datetime, update_by varchar(64),"
                    + "update_time datetime, remark varchar(500))");
            st.execute("CREATE TABLE t_flog_volume_table ("
                    + "id bigint PRIMARY KEY, table_code varchar(64), table_ver varchar(32),"
                    + "dbh_min decimal(6,2), dbh_max decimal(6,2), height_min decimal(6,2), height_max decimal(6,2),"
                    + "tree_volume decimal(12,6), status int, del_flag int DEFAULT 0,"
                    + "create_by varchar(64), create_time datetime, update_by varchar(64),"
                    + "update_time datetime, remark varchar(500))");
            st.execute("CREATE TABLE t_flog_survey_design ("
                    + "id bigint PRIMARY KEY, design_no varchar(64), node_no int, revise_of_id bigint,"
                    + "site_no varchar(64), subcomp_no varchar(64), cut_mode int, forest_kind int,"
                    + "scope_kind int, tree_count int, area decimal(12,2), yield_rate decimal(7,4),"
                    + "volume_code varchar(64), volume_ver varchar(32), total_volume decimal(14,4),"
                    + "yield_per_ha decimal(14,4), grade_level int, rule_code varchar(64),"
                    + "rule_eff_start datetime, rule_basis varchar(255), survey_date datetime,"
                    + "survey_by varchar(64), current_flag int, status int, del_flag int DEFAULT 0,"
                    + "create_by varchar(64), create_time datetime, update_by varchar(64),"
                    + "update_time datetime, remark varchar(500), CONSTRAINT uk_sdesign_no_node UNIQUE(design_no, node_no))");
            st.execute("CREATE TABLE t_flog_survey_tree ("
                    + "id bigint PRIMARY KEY, design_id bigint, design_no varchar(64), node_no int,"
                    + "tree_seq int, dbh decimal(6,2), tree_height decimal(6,2), tree_volume decimal(12,6),"
                    + "del_flag int DEFAULT 0, create_by varchar(64), create_time datetime)");
        }
        MybatisConfiguration cfg = new MybatisConfiguration();
        cfg.addMapper(TFlogYieldRuleMapper.class);
        cfg.addMapper(TFlogVolumeTableMapper.class);
        cfg.addMapper(TFlogSurveyDesignMapper.class);
        cfg.addMapper(TFlogSurveyTreeMapper.class);
        MybatisSqlSessionFactoryBean fb = new MybatisSqlSessionFactoryBean();
        fb.setDataSource(ds);
        fb.setConfiguration(cfg);
        fb.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig()));
        SqlSessionFactory sf = fb.getObject();
        SqlSession session = sf.openSession(true);
        ruleMapper = session.getMapper(TFlogYieldRuleMapper.class);
        volumeMapper = session.getMapper(TFlogVolumeTableMapper.class);
        designMapper = session.getMapper(TFlogSurveyDesignMapper.class);
        treeMapper = session.getMapper(TFlogSurveyTreeMapper.class);

        TFlogYieldRuleServiceImpl ruleService = new TFlogYieldRuleServiceImpl();
        inject(ruleService, "flogYieldRuleMapper", ruleMapper);
        volumeService = new TFlogVolumeTableServiceImpl();
        inject(volumeService, "volumeTableMapper", volumeMapper);
        service = new TFlogSurveyDesignServiceImpl();
        inject(service, "designMapper", designMapper);
        inject(service, "treeMapper", treeMapper);
        inject(service, "volumeTableService", volumeService);
        inject(service, "yieldRuleService", ruleService);

        seedFixtures();
    }

    @AfterEach
    void tearDown() {
        ((org.h2.jdbcx.JdbcConnectionPool) ds).dispose();
    }

    private static void inject(Object target, String field, Object value) throws Exception {
        java.lang.reflect.Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private void seedFixtures() {
        insertRule("SP-2026A", 1, "20", "40", "80", "2026-01-01 00:00:00", "2026-09-01 00:00:00", 100, 1);
        insertRule("SP-2026B", 1, "25", "50", "100", "2026-09-01 00:00:00", null, 100, 0);
        insertRule("GY-FY-2026", 2, "15", "30", "60", "2026-01-01 00:00:00", null, 100, 0);
        insertRule("GY-DG-2026", 4, "10", "20", null, "2026-01-01 00:00:00", null, 100, 0);

        insertBin("VT-ZJ", "2024", "5", "14", "6", "10", "0.038");
        insertBin("VT-ZJ", "2024", "5", "14", "10", "16", "0.056");
        insertBin("VT-ZJ", "2024", "14", "40", "6", "10", "0.092");
        insertBin("VT-ZJ", "2024", "14", "40", "10", "16", "0.135");
    }

    private void insertRule(String code, int scope, String th1, String th2, String th3,
                            String start, String end, int priority, int status) {
        TFlogYieldRule r = new TFlogYieldRule();
        r.setRuleCode(code);
        r.setRuleName(code);
        r.setScopeKind(scope);
        r.setTh1Max(new BigDecimal(th1));
        r.setTh2Max(new BigDecimal(th2));
        r.setTh3Max(th3 == null ? null : new BigDecimal(th3));
        r.setEffStart(Timestamp.valueOf(start));
        r.setEffEnd(end == null ? null : Timestamp.valueOf(end));
        r.setPriority(priority);
        r.setStatus(status);
        r.setDelFlag(0);
        ruleMapper.insert(r);
    }

    private void insertBin(String code, String ver, String dmin, String dmax,
                           String hmin, String hmax, String vol) {
        TFlogVolumeTable b = new TFlogVolumeTable();
        b.setTableCode(code);
        b.setTableVer(ver);
        b.setDbhMin(new BigDecimal(dmin));
        b.setDbhMax(new BigDecimal(dmax));
        b.setHeightMin(new BigDecimal(hmin));
        b.setHeightMax(new BigDecimal(hmax));
        b.setTreeVolume(new BigDecimal(vol));
        b.setStatus(0);
        b.setDelFlag(0);
        volumeMapper.insert(b);
    }

    private Timestamp t(String s) {
        return Timestamp.valueOf(s);
    }

    /** n 株胸径20、树高12：每株落 [14,40)x[10,16)=0.135；n 株蓄积 0.135*n。 */
    private FlogSurveyDesignDto dto(int n, String area, String rate, String surveyDay,
                                   int cutMode, int forestKind) {
        FlogSurveyDesignDto d = new FlogSurveyDesignDto();
        d.setDesignNo("SJ-TEST-" + Math.abs((surveyDay + ":" + area + ":" + n).hashCode()));
        d.setSiteNo("DC00");
        d.setSubcompNo("S-" + surveyDay.replaceAll("[^0-9]", ""));
        d.setCutMode(cutMode);
        d.setForestKind(forestKind);
        d.setArea(new BigDecimal(area));
        d.setYieldRate(new BigDecimal(rate));
        d.setVolumeCode("VT-ZJ");
        d.setVolumeVer("2024");
        d.setSurveyDate(t(surveyDay + " 00:00:00"));
        List<FlogSurveyTreeDto> trees = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            FlogSurveyTreeDto tr = new FlogSurveyTreeDto();
            tr.setDbh(new BigDecimal("20"));
            tr.setTreeHeight(new BigDecimal("12"));
            trees.add(tr);
        }
        d.setTrees(trees);
        return d;
    }

    /** 1. 三级链一处算清：500 株 ×0.135=67.5；67.5×0.6/1=40.5；秋版界 25/50/100 落中度。 */
    @Test
    void testChainEvaluatesAndPins() {
        FlogSurveyDesignDto d = dto(500, "1", "0.6", "2026-10-01", 4, 2);
        FlogYieldEvalVo vo = service.evaluate(d);
        assertEquals(0, vo.getTotalVolume().compareTo(new BigDecimal("67.5000")));
        assertEquals(0, vo.getYieldPerHa().compareTo(new BigDecimal("40.5000")));
        assertEquals(2, vo.getGradeLevel().intValue());
        assertEquals("SP-2026B", vo.getRuleCode());
        assertEquals(t("2026-09-01 00:00:00").getTime(), vo.getRuleEffStart().getTime());
        // 同回折出的株数与中间行齐
        assertEquals(500, vo.getTreeLines().size());
        assertEquals(0, vo.getTreeLines().get(0).getTreeVolume().compareTo(new BigDecimal("0.1350")));

        FlogSurveyDesignVo saved = service.submit(d);
        TFlogSurveyDesign pin = saved.getDesign();
        assertEquals(1, pin.getNodeNo().intValue());
        assertEquals(1, pin.getCurrentFlag().intValue());
        assertEquals(1, pin.getStatus().intValue(), "已钉");
        assertEquals(2, pin.getGradeLevel().intValue());
        assertEquals("SP-2026B", pin.getRuleCode());
        // 检尺株账随版钉了 500 行，材积与自核同回一致
        List<TFlogSurveyTree> rows = service.listTrees(pin.getId());
        assertEquals(500, rows.size());
        assertEquals(0, rows.get(7).getTreeVolume().compareTo(new BigDecimal("0.1350")));
        assertEquals(1, service.listCurrent(null, null, null, null).size());
    }

    /** 2. 同一份检尺数，调查日在换版前/后落不同档——正是去年两张纸差一格的来路，如今取界只一处。 */
    @Test
    void testSameTallyDifferentSurveyDatePicksDifferentRule() {
        // 40.5：旧版 20/40/80 里 >=40 落重；新版 25/50/100 里 <50 落中
        FlogYieldEvalVo aug = service.evaluate(dto(500, "1", "0.6", "2026-08-31", 4, 2));
        assertEquals(3, aug.getGradeLevel().intValue());
        assertEquals("SP-2026A", aug.getRuleCode());
        FlogYieldEvalVo oct = service.evaluate(dto(500, "1", "0.6", "2026-10-01", 4, 2));
        assertEquals(2, oct.getGradeLevel().intValue());
        assertEquals("SP-2026B", oct.getRuleCode());
    }

    /** 3. 挨着界往高里数：每公顷出材与轻界一样高落中，与中界一样高落重，越重界也重。 */
    @Test
    void testGradeBoundaryInChain() {
        // 400 株 ×0.135=54；54/2.16=25 整（出材率1），恰等新版轻界
        assertEquals(2, service.evaluate(dto(400, "2.16", "1", "2026-10-01", 4, 2)).getGradeLevel().intValue());
        // 54/1.08=50 整，恰等中界 -> 重
        assertEquals(3, service.evaluate(dto(400, "1.08", "1", "2026-10-01", 4, 2)).getGradeLevel().intValue());
        // 54/0.5=108 越重界 -> 仍重，没有第 4 档
        assertEquals(3, service.evaluate(dto(400, "0.5", "1", "2026-10-01", 4, 2)).getGradeLevel().intValue());
    }

    /** 4. 重界空着不设上界：公益林低改与中界一样高即落重。 */
    @Test
    void testNullHeavyOnWelfareReform() {
        // 200 株 ×0.135=27；27/1.35=20 恰等中界，重界空着 -> 重
        FlogYieldEvalVo vo = service.evaluate(dto(200, "1.35", "1", "2026-10-01", 6, 1));
        assertEquals(4, vo.getScopeKind().intValue());
        assertEquals(3, vo.getGradeLevel().intValue());
        assertEquals("GY-DG-2026", vo.getRuleCode());
    }

    /** 5. 公益林走主伐：翻不出合法界类别，退回重录。 */
    @Test
    void testWelfareMainCutRejected() {
        assertThrows(FlogBizException.class,
                () -> service.evaluate(dto(10, "1", "0.6", "2026-10-01", 1, 1)));
    }

    /** 6. 调查日那天这一类一条在场界都没有：出单写明无线可依，档位空着，不拿邻近日子的线凑。 */
    @Test
    void testNoRuleStillIssuesButGradeEmpty() {
        // 公益林抚育 2025 年设计：GY-FY-2026 还没启用
        FlogSurveyDesignDto d = dto(500, "1", "0.6", "2025-11-01", 4, 1);
        FlogYieldEvalVo vo = service.evaluate(d);
        assertTrue(vo.isNoRule());
        assertNull(vo.getGradeLevel());
        FlogSurveyDesignVo saved = service.submit(d);
        assertEquals(2, saved.getDesign().getStatus().intValue());
        assertNull(saved.getDesign().getGradeLevel());
        assertEquals("无线可依", saved.getDesign().getRuleCode());
        assertEquals(500, service.listTrees(saved.getDesign().getId()).size(), "账照记，档空着");
    }

    /** 7. 检尺两数来路不对不进链：负数、胸径四位九、空档、面积零、出材率越界、株数零。 */
    @Test
    void testBadTallyRejected() {
        FlogSurveyDesignDto neg = dto(3, "1", "0.6", "2026-10-01", 4, 2);
        neg.getTrees().get(0).setDbh(new BigDecimal("-8"));
        assertThrows(FlogBizException.class, () -> service.evaluate(neg));

        FlogSurveyDesignDto absurd = dto(3, "1", "0.6", "2026-10-01", 4, 2);
        absurd.getTrees().get(0).setDbh(new BigDecimal("499"));
        assertThrows(FlogBizException.class, () -> service.evaluate(absurd), "胸径四位九一类当场识破");

        FlogSurveyDesignDto gap = dto(3, "1", "0.6", "2026-10-01", 4, 2);
        gap.getTrees().get(0).setDbh(new BigDecimal("45")); // 表只到 [14,40)
        assertThrows(FlogBizException.class, () -> service.evaluate(gap), "翻不到档子不进链");

        assertThrows(FlogBizException.class,
                () -> service.evaluate(dto(3, "0", "0.6", "2026-10-01", 4, 2)), "面积为零");
        assertThrows(FlogBizException.class,
                () -> service.evaluate(dto(3, "1", "1.2", "2026-10-01", 4, 2)), "出材率越界");

        FlogSurveyDesignDto empty = dto(0, "1", "0.6", "2026-10-01", 4, 2);
        assertThrows(FlogBizException.class, () -> service.evaluate(empty), "株数为零");

        // 坏数一概没落库
        assertEquals(0, designMapper.selectList(null).size());
        assertEquals(0, treeMapper.selectList(null).size());
    }

    /** 8. 钉过不再改口：界事后被换了数，钉在单上的档位/代号/启用之日照旧。 */
    @Test
    void testPinnedResultImmutableWhenRuleLaterChanges() {
        FlogSurveyDesignDto d = dto(500, "1", "0.6", "2026-10-01", 4, 2);
        FlogSurveyDesignVo v1 = service.submit(d);
        Long pinId = v1.getDesign().getId();
        // 事后把现行界三数改了（模拟又有人动界册）
        TFlogYieldRule current = ruleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TFlogYieldRule>()
                        .eq("rule_code", "SP-2026B")).get(0);
        ruleMapper.update(null, new UpdateWrapper<TFlogYieldRule>()
                .eq("id", current.getId())
                .set("th2_max", new BigDecimal("90")));
        TFlogSurveyDesign pin = service.selectById(pinId);
        assertEquals(2, pin.getGradeLevel().intValue(), "钉的中度不改口");
        assertEquals("SP-2026B", pin.getRuleCode());
        assertEquals(0, new BigDecimal("40.5000").compareTo(pin.getYieldPerHa()));
    }

    /** 9. 整改复验重算：旧版降历史留检，新版另立，两回结论各留各的代号，合成一条不算对。 */
    @Test
    void testRectificationCreatesNewVersionKeepsOld() {
        FlogSurveyDesignDto d1 = dto(500, "1", "0.6", "2026-08-15", 4, 2);
        d1.setDesignNo("SJ-2026-0001");
        FlogSurveyDesignVo v1 = service.submit(d1);
        assertEquals(3, v1.getDesign().getGradeLevel().intValue(), "头版按秋前旧界落重");
        assertEquals("SP-2026A", v1.getDesign().getRuleCode());

        // 复验：调查日挪到秋后，同小班同字号升版
        FlogSurveyDesignDto d2 = dto(500, "1", "0.6", "2026-10-10", 4, 2);
        d2.setDesignNo("SJ-2026-0001");
        d2.setSubcompNo(d1.getSubcompNo());
        d2.setReviseOfId(v1.getDesign().getId());
        FlogSurveyDesignVo v2 = service.submit(d2);
        assertEquals(2, v2.getDesign().getNodeNo().intValue());
        assertEquals(2, v2.getDesign().getGradeLevel().intValue(), "复验按秋后新界落中");
        assertEquals("SP-2026B", v2.getDesign().getRuleCode());
        assertEquals(v1.getDesign().getId(), v2.getDesign().getReviseOfId());

        List<TFlogSurveyDesign> versions = service.listVersions("SJ-2026-0001");
        assertEquals(2, versions.size());
        assertEquals(0, versions.get(0).getCurrentFlag().intValue(), "旧版留检");
        assertEquals(1, versions.get(1).getCurrentFlag().intValue(), "新版现行");
        assertEquals("SP-2026A", versions.get(0).getRuleCode(), "旧版自己的代号留着");
        assertEquals("SP-2026B", versions.get(1).getRuleCode());

        // 只能就着现行版复验
        FlogSurveyDesignDto d3 = dto(500, "1", "0.6", "2026-10-20", 4, 2);
        d3.setSubcompNo(d1.getSubcompNo());
        d3.setReviseOfId(v1.getDesign().getId());
        assertThrows(FlogBizException.class, () -> service.submit(d3), "旧版不能再被复验");

        // 换小班不许借复验挪单
        FlogSurveyDesignDto d4 = dto(500, "1", "0.6", "2026-10-20", 4, 2);
        d4.setSubcompNo("OTHER");
        d4.setReviseOfId(v2.getDesign().getId());
        assertThrows(FlogBizException.class, () -> service.submit(d4));
    }

    /** 10. 材积表版只许钉一版：停用的版本取不出，整笔退回。 */
    @Test
    void testStoppedVolumeVersionRejected() {
        int stopped = volumeService.stopVersion("VT-ZJ", "2024");
        assertTrue(stopped > 0);
        assertThrows(FlogBizException.class,
                () -> service.evaluate(dto(5, "1", "0.6", "2026-10-01", 4, 2)));
    }

    /** 11. 不存在的材积表版/缺栏，一样不进链。 */
    @Test
    void testMissingVolumeVersionAndFields() {
        FlogSurveyDesignDto d = dto(5, "1", "0.6", "2026-10-01", 4, 2);
        d.setVolumeVer("1999");
        assertThrows(FlogBizException.class, () -> service.evaluate(d));

        FlogSurveyDesignDto noDate = dto(5, "1", "0.6", "2026-10-01", 4, 2);
        noDate.setSurveyDate(null);
        assertThrows(FlogBizException.class, () -> service.evaluate(noDate));
    }
}
