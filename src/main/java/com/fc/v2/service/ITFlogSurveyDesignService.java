package com.fc.v2.service;

import java.util.List;

import com.fc.v2.model.auto.TFlogSurveyDesign;
import com.fc.v2.model.auto.TFlogSurveyTree;
import com.fc.v2.model.custom.flog.FlogSurveyDesignDto;
import com.fc.v2.model.custom.flog.FlogSurveyDesignVo;
import com.fc.v2.model.custom.flog.FlogYieldEvalVo;

/**
 * 伐区调查设计单 Service接口。
 *
 * 从检尺两数到每公顷出材量再到定档，只在 {@link #evaluate} 这一条求值方法里走完：
 * 界取哪一版、并线取哪一条、材积折哪一版表，全由那一处现算现给。
 * {@link #submit} 出单时钉库的线代号与启用之日以 evaluate 的返回为准，
 * 页面不许自己凑一条界再摆出一个结论。
 *
 * @author fuce
 * @date 2026-09-30
 */
public interface ITFlogSurveyDesignService {

    /**
     * 唯一求值口（不落库，自核屏先算给调查员看）：
     * 检尺两数 → 材积表逐株折材积、累加蓄积 → 乘出材率除面积得每公顷出材量 →
     * 按调查设计之日请界并定档。检尺两数来路不对不进链，抛回重录。
     */
    FlogYieldEvalVo evaluate(FlogSurveyDesignDto dto);

    /**
     * 出单：同一回求值结果钉成设计单（档位、界代号、界启用之日）。
     * 带 reviseOfId 的是整改复验：旧版降历史留检，新版另立一行，两回结论各留各的代号，不合成一条。
     * 无线可依也出单，单子写明无线可依，档位空着，绝不拿邻近日子的线凑。
     */
    FlogSurveyDesignVo submit(FlogSurveyDesignDto dto);

    /** 按id查某一版（钉死值原样摆给抽查屏） */
    TFlogSurveyDesign selectById(Long id);

    /** 现行设计单集合（字号/小班/类别可筛） */
    List<TFlogSurveyDesign> listCurrent(String siteNo, String subcompNo, Integer scopeKind, Integer status);

    /** 同一张单的全部版本（版本序升序，旧版翻得着来历） */
    List<TFlogSurveyDesign> listVersions(String designNo);

    /** 某一版的检尺株账（抽查屏与自核屏摆的是同一回折出的株数材积） */
    List<TFlogSurveyTree> listTrees(Long designId);
}
