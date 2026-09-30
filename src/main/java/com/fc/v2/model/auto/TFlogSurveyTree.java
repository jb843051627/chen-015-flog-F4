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
 * 伐区检尺株账对象 t_flog_survey_tree
 *
 * 山场头一级账，一株一行。两数来路不对（负数、胸径四位九、翻不着材积表档）
 * 不进三级链，整笔退回调查员重录——一行坏数都不许混进蓄积。
 *
 * @author fuce
 * @date 2026-09-30
 */
@TableName("t_flog_survey_tree")
@ApiModel(value = "TFlogSurveyTree", description = "伐区检尺株账")
public class TFlogSurveyTree implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所属设计单版本id */
    @TableField("design_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所属设计单版本id")
    private Long designId;

    /** 调查设计单号（随版钉住） */
    @TableField("design_no")
    @ApiModelProperty(value = "调查设计单号")
    private String designNo;

    /** 所属版本序 */
    @TableField("node_no")
    @ApiModelProperty(value = "所属版本序")
    private Integer nodeNo;

    /** 株序（一单内自1起） */
    @TableField("tree_seq")
    @ApiModelProperty(value = "株序")
    private Integer treeSeq;

    /** 胸径(厘米) */
    @TableField("dbh")
    @ApiModelProperty(value = "胸径(厘米)")
    private BigDecimal dbh;

    /** 树高(米) */
    @TableField("tree_height")
    @ApiModelProperty(value = "树高(米)")
    private BigDecimal treeHeight;

    /** 本株材积(立方米，同一回按所钉表版折) */
    @TableField("tree_volume")
    @ApiModelProperty(value = "本株材积(立方米)")
    private BigDecimal treeVolume;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDesignId() {
        return designId;
    }

    public void setDesignId(Long designId) {
        this.designId = designId;
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

    public Integer getTreeSeq() {
        return treeSeq;
    }

    public void setTreeSeq(Integer treeSeq) {
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
}
