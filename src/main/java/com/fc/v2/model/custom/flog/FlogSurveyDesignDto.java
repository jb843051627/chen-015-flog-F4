package com.fc.v2.model.custom.flog;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 伐区调查设计出单报文（JSON 送法）。
 *
 * 头版与整改复验同此一口：复验时带上 reviseOfId（现行版id），服务层据此升版。
 * 档位、界代号、启用之日、材积表版一律不由页面塞，全由唯一求值口现算现给。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class FlogSurveyDesignDto {

    /** 调查设计单号（头版可空，由系统排；复验须带，认得是同一张单） */
    private String designNo;

    /** 整改复验所依现行版id；头版空着 */
    private Long reviseOfId;

    /** 调查编制单位底档字号 */
    private String siteNo;

    /** 小班号 */
    private String subcompNo;

    /** 采伐方式 1皆伐 2渐伐 3择伐 4抚育采伐 5更新采伐 6低质低效林改造 */
    private Integer cutMode;

    /** 林种性质 1公益林 2商品林 */
    private Integer forestKind;

    /** 小班面积(公顷) */
    private BigDecimal area;

    /** 出材率(0..1) */
    private BigDecimal yieldRate;

    /** 材积表代号 */
    private String volumeCode;

    /** 材积表版本（指定本次检尺用哪一版表；三级链同一回都用它） */
    private String volumeVer;

    /** 调查设计之日（取界认这一天，不认今天） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date surveyDate;

    /** 检尺株账：一株两数 */
    private List<FlogSurveyTreeDto> trees;

    /** 备注 */
    private String remark;

    public String getDesignNo() {
        return designNo;
    }

    public void setDesignNo(String designNo) {
        this.designNo = designNo;
    }

    public Long getReviseOfId() {
        return reviseOfId;
    }

    public void setReviseOfId(Long reviseOfId) {
        this.reviseOfId = reviseOfId;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public String getSubcompNo() {
        return subcompNo;
    }

    public void setSubcompNo(String subcompNo) {
        this.subcompNo = subcompNo;
    }

    public Integer getCutMode() {
        return cutMode;
    }

    public void setCutMode(Integer cutMode) {
        this.cutMode = cutMode;
    }

    public Integer getForestKind() {
        return forestKind;
    }

    public void setForestKind(Integer forestKind) {
        this.forestKind = forestKind;
    }

    public BigDecimal getArea() {
        return area;
    }

    public void setArea(BigDecimal area) {
        this.area = area;
    }

    public BigDecimal getYieldRate() {
        return yieldRate;
    }

    public void setYieldRate(BigDecimal yieldRate) {
        this.yieldRate = yieldRate;
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

    public Date getSurveyDate() {
        return surveyDate;
    }

    public void setSurveyDate(Date surveyDate) {
        this.surveyDate = surveyDate;
    }

    public List<FlogSurveyTreeDto> getTrees() {
        return trees;
    }

    public void setTrees(List<FlogSurveyTreeDto> trees) {
        this.trees = trees;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
