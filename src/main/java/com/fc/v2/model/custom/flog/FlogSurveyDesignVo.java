package com.fc.v2.model.custom.flog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TFlogSurveyDesign;

/**
 * 调查设计出单给回：钉死的单子 + 同一回计算的逐株中间数 + 一句话。
 * 自核屏与抽查屏只摆这一份，页面不另凑界、不另现算。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class FlogSurveyDesignVo {

    /** 钉死的设计单（档位/界代号/启用之日都在上面） */
    private TFlogSurveyDesign design;

    /** 逐株材积（同一回表折出） */
    private List<FlogYieldEvalVo.TreeLine> treeLines = new ArrayList<>();

    /** 强度档位名目 */
    private String gradeLevelText;

    /** 单子情形名目 */
    private String statusText;

    /** 林地类别名目 */
    private String scopeKindText;

    /** 给调查员的一句话（含三级链各数与所依界） */
    private String message;

    public TFlogSurveyDesign getDesign() {
        return design;
    }

    public void setDesign(TFlogSurveyDesign design) {
        this.design = design;
    }

    public List<FlogYieldEvalVo.TreeLine> getTreeLines() {
        return treeLines;
    }

    public void setTreeLines(List<FlogYieldEvalVo.TreeLine> treeLines) {
        this.treeLines = treeLines;
    }

    public String getGradeLevelText() {
        return gradeLevelText;
    }

    public void setGradeLevelText(String gradeLevelText) {
        this.gradeLevelText = gradeLevelText;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public String getScopeKindText() {
        return scopeKindText;
    }

    public void setScopeKindText(String scopeKindText) {
        this.scopeKindText = scopeKindText;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
