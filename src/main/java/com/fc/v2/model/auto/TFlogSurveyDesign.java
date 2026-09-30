package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 伐区调查设计单对象 t_flog_survey_design
 *
 * 一张单号管一个小班；整改复验一版版往上加，旧版降为历史只供回查。
 * 强度档位、所依界代号与那条界的启用之日，在出单一刻同一回计算钉上，钉过不再改口；
 * 自核屏与抽查屏摆的是同一份钉死值，页面不许另凑一条界再摆结论。
 *
 * @author fuce
 * @date 2026-09-30
 */
@TableName("t_flog_survey_design")
@ApiModel(value = "TFlogSurveyDesign", description = "伐区调查设计单")
public class TFlogSurveyDesign implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 调查设计单号 */
    @TableField("design_no")
    @ApiModelProperty(value = "调查设计单号")
    private String designNo;

    /** 本单版本序（头版1，整改复验一版版往上加） */
    @TableField("node_no")
    @ApiModelProperty(value = "本单版本序")
    private Integer nodeNo;

    /** 重录/复验所依上一版id（头版为空） */
    @TableField("revise_of_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "重录/复验所依上一版id")
    private Long reviseOfId;

    /** 调查编制单位底档字号 */
    @TableField("site_no")
    @ApiModelProperty(value = "调查编制单位底档字号")
    private String siteNo;

    /** 小班号 */
    @TableField("subcomp_no")
    @ApiModelProperty(value = "小班号")
    private String subcompNo;

    /** 采伐方式 1皆伐 2渐伐 3择伐 4抚育采伐 5更新采伐 6低质低效林改造 */
    @TableField("cut_mode")
    @ApiModelProperty(value = "采伐方式")
    private Integer cutMode;

    /** 林种性质 1公益林 2商品林 */
    @TableField("forest_kind")
    @ApiModelProperty(value = "林种性质")
    private Integer forestKind;

    /** 本次定档所管林地类别（随林种+采伐性质折算，钉在单上） */
    @TableField("scope_kind")
    @ApiModelProperty(value = "本次定档所管林地类别")
    private Integer scopeKind;

    /** 检尺株数 */
    @TableField("tree_count")
    @ApiModelProperty(value = "检尺株数")
    private Integer treeCount;

    /** 小班面积(公顷) */
    @TableField("area")
    @ApiModelProperty(value = "小班面积(公顷)")
    private BigDecimal area;

    /** 出材率(0..1之间，蓄积乘它得出材) */
    @TableField("yield_rate")
    @ApiModelProperty(value = "出材率(0..1)")
    private BigDecimal yieldRate;

    /** 本单所用材积表代号 */
    @TableField("volume_code")
    @ApiModelProperty(value = "本单所用材积表代号")
    private String volumeCode;

    /** 本单所钉材积表版本（蓄积与定档同一回表，不许两头两版） */
    @TableField("volume_ver")
    @ApiModelProperty(value = "本单所钉材积表版本")
    private String volumeVer;

    /** 全株逐株累加蓄积(立方米) */
    @TableField("total_volume")
    @ApiModelProperty(value = "全株逐株累加蓄积(立方米)")
    private BigDecimal totalVolume;

    /** 每公顷出材量(立方米/公顷，同一回计算给) */
    @TableField("yield_per_ha")
    @ApiModelProperty(value = "每公顷出材量(立方米/公顷)")
    private BigDecimal yieldPerHa;

    /** 强度档位 1轻 2中 3重；无线可依为空 */
    @TableField("grade_level")
    @ApiModelProperty(value = "强度档位 1轻 2中 3重")
    private Integer gradeLevel;

    /** 定档所依界限值代号（钉死；无线可依时写无线可依记号） */
    @TableField("rule_code")
    @ApiModelProperty(value = "定档所依界限值代号")
    private String ruleCode;

    /** 所依那条界的启用之日（钉死） */
    @TableField("rule_eff_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "所依那条界的启用之日")
    private Date ruleEffStart;

    /** 定档依据一句话（含界上三数，随单留查） */
    @TableField("rule_basis")
    @ApiModelProperty(value = "定档依据一句话")
    private String ruleBasis;

    /** 调查设计之日（取界认这一天，不认今天） */
    @TableField("survey_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "调查设计之日")
    private Date surveyDate;

    /** 调查员（自核与抽查同屏同人） */
    @TableField("survey_by")
    @ApiModelProperty(value = "调查员")
    private String surveyBy;

    /** 版本现行标记 1现行 0历史 */
    @TableField("current_flag")
    @ApiModelProperty(value = "版本现行标记")
    private Integer currentFlag;

    /** 单子情形 0草稿(未钉) 1已钉(出单) 2无线可依 */
    @TableField("status")
    @ApiModelProperty(value = "单子情形 0草稿 1已钉 2无线可依")
    private Integer status;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDesignNo() {
        return designNo;
    }

    public void setDesignNo(String designNo) {
        this.designNo = designNo;
    }

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
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

    public Integer getScopeKind() {
        return scopeKind;
    }

    public void setScopeKind(Integer scopeKind) {
        this.scopeKind = scopeKind;
    }

    public Integer getTreeCount() {
        return treeCount;
    }

    public void setTreeCount(Integer treeCount) {
        this.treeCount = treeCount;
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

    public Integer getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(Integer gradeLevel) {
        this.gradeLevel = gradeLevel;
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

    public Date getSurveyDate() {
        return surveyDate;
    }

    public void setSurveyDate(Date surveyDate) {
        this.surveyDate = surveyDate;
    }

    public String getSurveyBy() {
        return surveyBy;
    }

    public void setSurveyBy(String surveyBy) {
        this.surveyBy = surveyBy;
    }

    public Integer getCurrentFlag() {
        return currentFlag;
    }

    public void setCurrentFlag(Integer currentFlag) {
        this.currentFlag = currentFlag;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
