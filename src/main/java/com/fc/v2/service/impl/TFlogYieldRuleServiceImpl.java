package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.common.exception.biz.FlogBizException;
import com.fc.v2.mapper.auto.TFlogYieldRuleMapper;
import com.fc.v2.model.auto.TFlogYieldRule;
import com.fc.v2.model.custom.flog.FlogScopeKind;
import com.fc.v2.service.ITFlogYieldRuleService;
import com.fc.v2.util.StringUtils;

/**
 * 可采强度界限值线 Service业务层处理（rule-eval 形状：取界只走一处）。
 *
 * 哪条界该请出来，只认单子上的调查设计那一日、不认今天：落在 [effStart, effEnd) 内才算在场。
 * 让位条（status=1）交棒日之后现行求值再见不到它；但交棒日之前的历史调查日上它仍在场，
 * 去年秋天的设计今年补结论，请的就是它当时那一版。两条界同时在场的，序位（priority）大的说话；
 * 序位也分不出高下，认代号排在后头的。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TFlogYieldRuleServiceImpl implements ITFlogYieldRuleService {

    /** 线的情形：现行（在场候选） */
    private static final int STATUS_CURRENT = 0;
    /** 线的情形：已让位（仅供回查） */
    private static final int STATUS_HANDED_OVER = 1;

    @javax.annotation.Resource
    private TFlogYieldRuleMapper flogYieldRuleMapper;

    /**
     * 时段比较统一走**墙钟字符串**（yyyy-MM-dd HH:mm:ss）+ 字符串绑定：
     * JDBC 的 serverTimezone 与本机时区不对称，直接把 java.util.Date 作参数会整体偏移，
     * 使边界判定在"交棒当日"这类用例上整体错档（实测踩到）。自核屏与抽查屏都过这一处，口径只有一套。
     */
    private static String ts(Date d) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }

    @Override
    public TFlogYieldRule selectTFlogYieldRuleById(Long id) {
        return this.flogYieldRuleMapper.selectById(id);
    }

    @Override
    public int evaluate(String ruleCode, BigDecimal input, Date at) {
        if (StringUtils.isEmpty(ruleCode) || input == null || at == null) {
            return GRADE_NONE;
        }
        if (input.compareTo(BigDecimal.ZERO) < 0) {
            return GRADE_NONE;
        }
        // 按代号请线：同代号多版里只请 at 当日在场的那版。
        // 在场以生效时段为权威：让位条（status=1）若钉了交棒日，在交棒日之前的历史调查日上
        // 仍是当日合法依据（去年秋天的设计今年补结论，就请当时在场那条）；今天 at>=eff_end 时
        // 时段条件自然把它排除，"现行求值再见不到让位条"。唯一例外：status=1 却没有交棒日，
        // 这种条没有生效期右端又已让位，任何日子都不取。真有两版同时在场（录错），
        // 序位大、启用晚的先说话，结论仍唯一。
        TFlogYieldRule rule = this.flogYieldRuleMapper.selectOne(new QueryWrapper<TFlogYieldRule>()
                .eq("del_flag", 0)
                .isNotNull("scope_kind")
                .eq("rule_code", ruleCode.trim())
                .and(q -> q.isNull("eff_start").or().le("eff_start", ts(at)))
                .and(q -> q.gt("eff_end", ts(at))
                        .or(o -> o.isNull("eff_end").eq("status", STATUS_CURRENT)))
                .orderByDesc("priority", "eff_start")
                .last("limit 1"));
        // 查无在场版即无线可依，不拿同代号邻近日子的旧版凑
        if (rule == null) {
            return GRADE_NONE;
        }
        return classify(rule, input);
    }

    @Override
    public int evaluate(Integer scopeKind, BigDecimal input, Date at) {
        if (input == null || at == null || FlogScopeKind.of(scopeKind) == null) {
            return GRADE_NONE;
        }
        if (input.compareTo(BigDecimal.ZERO) < 0) {
            return GRADE_NONE;
        }
        TFlogYieldRule rule = pickAvailable(scopeKind, at);
        return rule == null ? GRADE_NONE : classify(rule, input);
    }

    @Override
    public TFlogYieldRule pickAvailable(Integer scopeKind, Date at) {
        if (at == null || FlogScopeKind.of(scopeKind) == null) {
            return null;
        }
        List<TFlogYieldRule> hit = listAvailable(scopeKind, at);
        // listAvailable 已按并线口径排定：序位降序、代号降序，头一条即唯一开口那条
        return hit.isEmpty() ? null : hit.get(0);
    }

    @Override
    public List<TFlogYieldRule> listAvailable(Integer scopeKind, Date at) {
        if (at == null) {
            return new ArrayList<>();
        }
        // 不按 status 硬过滤：让位条交棒日之前的历史调查日仍在场（请当时那条）；
        // status=1 且无交棒日的死条由右端条件兜底排除。scope_kind 为空的旧库未归类线一条都不请出来
        // （归类补录前宁可以为无线可依，不拿错类别的线凑）。并线序：序位降序、代号降序。
        QueryWrapper<TFlogYieldRule> w = new QueryWrapper<TFlogYieldRule>()
                .eq("del_flag", 0)
                .isNotNull("scope_kind")
                .eq(FlogScopeKind.of(scopeKind) != null, "scope_kind", scopeKind)
                .and(q -> q.isNull("eff_start").or().le("eff_start", ts(at)))
                .and(q -> q.gt("eff_end", ts(at))
                        .or(o -> o.isNull("eff_end").eq("status", STATUS_CURRENT)))
                .orderByDesc("priority", "rule_code");
        return this.flogYieldRuleMapper.selectList(w);
    }

    @Override
    public List<TFlogYieldRule> listAvailable(Date at) {
        return listAvailable(null, at);
    }

    @Override
    public int classify(TFlogYieldRule rule, BigDecimal input) {
        if (rule == null || input == null || input.compareTo(BigDecimal.ZERO) < 0) {
            return GRADE_NONE;
        }
        BigDecimal th1 = rule.getTh1Max();
        BigDecimal th2 = rule.getTh2Max();
        BigDecimal th3 = rule.getTh3Max();
        if (th1 == null && th2 == null && th3 == null) {
            // 三条界全空：无界可评，不算断档也不硬凑一档
            return GRADE_NONE;
        }
        // 界数缺着是“不设这一档”的意思（早年纸质簿录进的条重界原本空着），不算断档：
        // 只拿在场的界由轻到重排，数越过几条在场界，就往上抬几档，缺档整档跳过。
        // 挨着界往高里数：等于界即落高一档——与中界一样高就落重界；越过重界也统统算重，不另开第 4 档。
        int grade = GRADE_LIGHT;
        BigDecimal[] thresholds = new BigDecimal[] {th1, th2, th3};
        for (BigDecimal th : thresholds) {
            if (th == null) {
                // 该档不设：不占一档位，输入无从“落进”它之前
                continue;
            }
            if (input.compareTo(th) < 0) {
                return grade;
            }
            grade++;
        }
        return GRADE_HEAVY;
    }

    @Override
    public boolean usable(Long id, Date at) {
        if (id == null || at == null) {
            return false;
        }
        // 与定位/列表同一个时段口径（含字符串绑定），不在 Java 端另比一套时刻
        Integer cnt = this.flogYieldRuleMapper.selectCount(new QueryWrapper<TFlogYieldRule>()
                .eq("id", id)
                .eq("del_flag", 0)
                .and(q -> q.isNull("eff_start").or().le("eff_start", ts(at)))
                .and(q -> q.gt("eff_end", ts(at))
                        .or(o -> o.isNull("eff_end").eq("status", STATUS_CURRENT))));
        return cnt != null && cnt > 0;
    }

    @Override
    public int countAvailable(Date at) {
        return listAvailable(at).size();
    }

    @Override
    public List<TFlogYieldRule> listAllVersions(Integer scopeKind, Integer status, String ruleCode) {
        return this.flogYieldRuleMapper.selectList(new QueryWrapper<TFlogYieldRule>()
                .eq("del_flag", 0)
                .eq(FlogScopeKind.of(scopeKind) != null, "scope_kind", scopeKind)
                .eq(status != null, "status", status)
                .eq(StringUtils.isNotEmpty(ruleCode), "rule_code", ruleCode.trim())
                .orderByAsc("scope_kind")
                .orderByDesc("eff_start", "rule_code"));
    }

    @Override
    public TFlogYieldRule registerRule(TFlogYieldRule input) {
        if (input == null) {
            throw new FlogBizException("送上来的界条是空的，没东西可立");
        }
        List<String> missing = new ArrayList<>();
        if (StringUtils.isEmpty(input.getRuleCode())) {
            missing.add("界限值代号");
        }
        if (StringUtils.isEmpty(input.getRuleName())) {
            missing.add("线名");
        }
        if (FlogScopeKind.of(input.getScopeKind()) == null) {
            missing.add("所管林地类别（商品林/公益林抚育/公益林更新/公益林低改四选一）");
        }
        if (input.getEffStart() == null) {
            missing.add("启用之日");
        }
        if (!missing.isEmpty()) {
            throw new FlogBizException("这条界立不住，缺了便起不来的格子：" + String.join("、", missing));
        }
        // 界数可空（空即不设该档），但给了就得由轻到重抬得上去，不许倒挂
        BigDecimal th1 = input.getTh1Max();
        BigDecimal th2 = input.getTh2Max();
        BigDecimal th3 = input.getTh3Max();
        if (th1 != null && th1.compareTo(BigDecimal.ZERO) < 0) {
            throw new FlogBizException("轻度界数不能为负");
        }
        if (th2 != null && th2.compareTo(BigDecimal.ZERO) < 0) {
            throw new FlogBizException("中度界数不能为负");
        }
        if (th3 != null && th3.compareTo(BigDecimal.ZERO) < 0) {
            throw new FlogBizException("重度界数不能为负");
        }
        if (th1 != null && th2 != null && th1.compareTo(th2) >= 0) {
            throw new FlogBizException("中度界数得高过轻度界数，界倒挂了没人知道往哪档靠");
        }
        if (th2 != null && th3 != null && th2.compareTo(th3) >= 0) {
            throw new FlogBizException("重度界数得高中度界数，界倒挂了没人知道往哪档靠");
        }
        // 换版只添不改：新界一律以现行条入场，旧版让位另走 handover 同一口径
        input.setId(null);
        input.setStatus(STATUS_CURRENT);
        input.setDelFlag(0);
        this.flogYieldRuleMapper.insert(input);
        return input;
    }

    @Override
    public TFlogYieldRule handover(Long id, Date effEnd) {
        if (id == null) {
            throw new FlogBizException("没给要交棒的界条id");
        }
        if (effEnd == null) {
            throw new FlogBizException("交棒之日得给个日子；旧线管到哪天、新版哪天开口，全站只准这一套说法");
        }
        TFlogYieldRule rule = this.flogYieldRuleMapper.selectById(id);
        if (rule == null || (rule.getDelFlag() != null && rule.getDelFlag() == 1)) {
            throw new FlogBizException("界条 " + id + " 查无在册，交不了棒");
        }
        if (rule.getStatus() != null && rule.getStatus() == STATUS_HANDED_OVER) {
            throw new FlogBizException("代号 " + rule.getRuleCode() + " 已让位，让位的条只供回查，不许再交一回");
        }
        if (rule.getEffStart() != null && !effEnd.after(rule.getEffStart())) {
            throw new FlogBizException("交棒之日得晚于启用之日，旧线不能还没开口就让位");
        }
        // eff_end 不含：交棒当日 00:00:00 起旧条即不在场，新条同一刻开口
        int rows = this.flogYieldRuleMapper.update(null, new UpdateWrapper<TFlogYieldRule>()
                .eq("id", id)
                .eq("status", STATUS_CURRENT)
                .set("status", STATUS_HANDED_OVER)
                .set("eff_end", effEnd));
        if (rows == 0) {
            throw new FlogBizException("代号 " + rule.getRuleCode() + " 刚被另一笔交了棒，本笔请刷新后再看");
        }
        rule.setStatus(STATUS_HANDED_OVER);
        rule.setEffEnd(effEnd);
        return rule;
    }
}
