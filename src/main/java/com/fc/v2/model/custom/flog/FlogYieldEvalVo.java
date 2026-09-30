package com.fc.v2.model.custom.flog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 三级链唯一求值口的给回。
 *
 * 检尺两数 → 材积表折单株材积并逐株累加得蓄积 → 乘出材率除小班面积得每公顷出材量 →
 * 按调查设计之日请出的那条界定轻/中/重，全在同一回计算里走完，中间数一份带齐。
 * 页面自核与站里抽查都只摆这一份；出单时钉库的代号、启用之日也以它为准。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class FlogYieldEvalVo {

    /** 一株一行的求值中间数（材积同回折出） */
    public static class TreeLine {
        /** 株序 */
        private int treeSeq;
        /** 胸径(厘米) */
        private BigDecimal dbh;
        /** 树高(米) */
        private BigDecimal treeHeight;
        /** 本株材积(立方米) */
        private BigDecimal treeVolume;

        public TreeLine() {
        }

        public TreeLine(int treeSeq, BigDecimal dbh, BigDecimal treeHeight, BigDecimal treeVolume) {
            this.treeSeq = treeSeq;
            this.dbh = dbh;
            this.treeHeight = treeHeight;
            this.treeVolume = treeVolume;
        }

        public int getTreeSeq() {
            return treeSeq;
        }

        public void setTreeSeq(int treeSeq) {
            this.treeSeq = treeSeq;
        }

        public BigDecimal getDbh() {
            return dbh;
        }

        public void setDbh(BigDecimal dbh) {
            this.dbh = dbh;
        }

        public BigDecimal getTreeHeight() {
            return treeHeight;
        }

        public void setTreeHeight(BigDecimal treeHeight) {
            this.treeHeight = treeHeight;
        }

        public BigDecimal getTreeVolume() {
            return treeVolume;
        }

        public void setTreeVolume(BigDecimal treeVolume) {
            this.treeVolume = treeVolume;
        }
    }

    /** 管哪一类林地（折算口径钉回给页面） */
    private Integer scopeKind;
    /** 类别名目 */
    private String scopeKindText;

    /** 所用材积表代号/版本（蓄积与定档同一回表） */
    private String volumeCode;
    private String volumeVer;

    /** 检尺株数 */
    private int treeCount;
    /** 逐株材积行 */
    private List<TreeLine> treeLines = new ArrayList<>();
    /** 全株累加蓄积(立方米) */
    private BigDecimal totalVolume;
    /** 每公顷出材量(立方米/公顷) */
    private BigDecimal yieldPerHa;

    /** 调查设计之日（取界所认的日子） */
    private Date surveyDate;

    /** 请出的界：代号 */
    private String ruleCode;
    /** 请出的界：启用之日 */
    private Date ruleEffStart;
    /** 定档依据一句话（含界上三数） */
    private String ruleBasis;
    /** 这一趟翻不翻得出在场界：true=无线可依，档位不给 */
    private boolean noRule;
    /** 无线可依一句话（写明是哪类林地、哪一天无线可依） */
    private String noRuleReason;

    /** 强度档位 1轻 2中 3重；无线可依为 null */
    private Integer gradeLevel;
    /** 档位名目 */
    private String gradeLevelText;

    public Integer getScopeKind() {
        return scopeKind;
    }

    public void setScopeKind(Integer scopeKind) {
        this.scopeKind = scopeKind;
    }

    public String getScopeKindText() {
        return scopeKindText;
    }

    public void setScopeKindText(String scopeKindText) {
        this.scopeKindText = scopeKindText;
    }

    public String getVolumeCode() {
        return volumeCode;
    }

    public void setVolumeCode(String volumeCode) {
        this.volumeCode = volumeCode;
    }

    public String getVolumeVer() {
        return volumeVer;
    }

    public void setVolumeVer(String volumeVer) {
        this.volumeVer = volumeVer;
    }

    public int getTreeCount() {
        return treeCount;
    }

    public void setTreeCount(int treeCount) {
        this.treeCount = treeCount;
    }

    public List<TreeLine> getTreeLines() {
        return treeLines;
    }

    public void setTreeLines(List<TreeLine> treeLines) {
        this.treeLines = treeLines;
    }

    public BigDecimal getTotalVolume() {
        return totalVolume;
    }

    public void setTotalVolume(BigDecimal totalVolume) {
        this.totalVolume = totalVolume;
    }

    public BigDecimal getYieldPerHa() {
        return yieldPerHa;
    }

    public void setYieldPerHa(BigDecimal yieldPerHa) {
        this.yieldPerHa = yieldPerHa;
    }

    public Date getSurveyDate() {
        return surveyDate;
    }

    public void setSurveyDate(Date surveyDate) {
        this.surveyDate = surveyDate;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public Date getRuleEffStart() {
        return ruleEffStart;
    }

    public void setRuleEffStart(Date ruleEffStart) {
        this.ruleEffStart = ruleEffStart;
    }

    public String getRuleBasis() {
        return ruleBasis;
    }

    public void setRuleBasis(String ruleBasis) {
        this.ruleBasis = ruleBasis;
    }

    public boolean isNoRule() {
        return noRule;
    }

    public void setNoRule(boolean noRule) {
        this.noRule = noRule;
    }

    public String getNoRuleReason() {
        return noRuleReason;
    }

    public void setNoRuleReason(String noRuleReason) {
        this.noRuleReason = noRuleReason;
    }

    public Integer getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(Integer gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public String getGradeLevelText() {
        return gradeLevelText;
    }

    public void setGradeLevelText(String gradeLevelText) {
        this.gradeLevelText = gradeLevelText;
    }
}
