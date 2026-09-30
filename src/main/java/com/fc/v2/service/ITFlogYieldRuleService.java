package com.fc.v2.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TFlogYieldRule;

/**
 * 可采强度界限值线 Service接口（rule-eval 形状：取界只走一处）。
 *
 * 界取哪一版、两条界同时靠上时哪一条说话，全由本接口这几口径定：
 * 只认调查设计那一日落在 [effStart, effEnd) 内的线（让位条在其交棒日之前的历史调查日上
 * 仍算在场，旧设计补结论请当时那条；交棒日之后现行求值再见不到它）；
 * 序位大的先说话，序位分不出高下认代号排在后头的。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITFlogYieldRuleService {

    /** 无在场界 / 输入越界 / 参数缺失时的求值给回（无线可依，不许拿邻近日子的线凑） */
    int GRADE_NONE = 0;
    /** 轻度 */
    int GRADE_LIGHT = 1;
    /** 中度 */
    int GRADE_MIDDLE = 2;
    /** 重度 */
    int GRADE_HEAVY = 3;

    /** 按主键查询规则（让位条也翻得着，仅供回查来历） */
    TFlogYieldRule selectTFlogYieldRuleById(Long id);

    /**
     * 单规则求值：按 ruleCode 请出 at 时刻在场的那一版，按三条界判档（1轻 2中 3重）。
     * 无在场版本 / 输入越界 / 参数缺失一律返回 0；越过重界也算重档，不另设第 4 档。
     */
    int evaluate(String ruleCode, BigDecimal input, Date at);

    /**
     * 按林地类别取线求值（同一方法名下的另一种取法）：at 时刻该类别在场各线中，
     * 序位最大者说话；序位相同认代号排在后头那条。一条都请不出返回 0（无线可依）。
     */
    int evaluate(Integer scopeKind, BigDecimal input, Date at);

    /**
     * at 时刻某类林地唯一该开口的那条界（并线口径：序位降序、代号降序，头一条说话）。
     * 一条在场界都没有返回 null —— 调用方须写明无线可依，不得改拿邻近日子的线凑。
     */
    TFlogYieldRule pickAvailable(Integer scopeKind, Date at);

    /** at 时刻在场界（序位降序、代号降序；scopeKind 为 null 时各类别全列）。交棒日之后的让位条不在其列，交棒日之前的历史日仍取得到 */
    List<TFlogYieldRule> listAvailable(Integer scopeKind, Date at);

    /** {@link #listAvailable(Integer, Date)} 不分类别的旧调法，照旧可调 */
    List<TFlogYieldRule> listAvailable(Date at);

    /** 就着给定的一条界分档（界数空着即不设该档，跟着下一档往下落） */
    int classify(TFlogYieldRule rule, BigDecimal input);

    /** 单条界在 at 时刻是否在场（口径与定位/列表一致：现行且在 [effStart, effEnd) 内） */
    boolean usable(Long id, Date at);

    /** at 时刻在场界条数（角标 / 看板） */
    int countAvailable(Date at);

    // ---- 界册维护：换版只添不改，让位另走交棒 ----

    /** 界册全版（含让位条，按类别、启用日倒序），供翻来历 */
    List<TFlogYieldRule> listAllVersions(Integer scopeKind, Integer status, String ruleCode);

    /** 立一条新界（status=0 现行；界上三数可空，空即不设该档） */
    TFlogYieldRule registerRule(TFlogYieldRule rule);

    /**
     * 交棒：旧界管到 effEnd 前一刻（effEnd 不含，当天起让位），置 status=1。
     * 全站交棒日只此一个口径：自核屏与抽查屏都按这同一个时段取线。
     */
    TFlogYieldRule handover(Long id, Date effEnd);
}
