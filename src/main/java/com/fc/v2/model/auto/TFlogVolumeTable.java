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
 * 材积表档对象 t_flog_volume_table
 *
 * 一株的胸径、树高两数在此折成单株材积。同代号可多版并册；调查设计单钉哪一版，
 * 蓄积回写与强度评定就得同用那一版，不许两头各取一版表。
 *
 * @author fuce
 * @date 2026-09-30
 */
@TableName("t_flog_volume_table")
@ApiModel(value = "TFlogVolumeTable", description = "材积表档（胸径+树高折单株材积）")
public class TFlogVolumeTable implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 材积表代号 */
    @TableField("table_code")
    @ApiModelProperty(value = "材积表代号")
    private String tableCode;

    /** 材积表版本号 */
    @TableField("table_ver")
    @ApiModelProperty(value = "材积表版本号")
    private String tableVer;

    /** 胸径档起(厘米，含) */
    @TableField("dbh_min")
    @ApiModelProperty(value = "胸径档起(厘米，含)")
    private BigDecimal dbhMin;

    /** 胸径档止(厘米，不含) */
    @TableField("dbh_max")
    @ApiModelProperty(value = "胸径档止(厘米，不含)")
    private BigDecimal dbhMax;

    /** 树高档起(米，含) */
    @TableField("height_min")
    @ApiModelProperty(value = "树高档起(米，含)")
    private BigDecimal heightMin;

    /** 树高档止(米，不含) */
    @TableField("height_max")
    @ApiModelProperty(value = "树高档止(米，不含)")
    private BigDecimal heightMax;

    /** 本档单株材积(立方米) */
    @TableField("tree_volume")
    @ApiModelProperty(value = "本档单株材积(立方米)")
    private BigDecimal treeVolume;

    /** 情形 0在场可用 1停用（停用只供旧单回查） */
    @TableField("status")
    @ApiModelProperty(value = "情形 0在场可用 1停用")
    private Integer status;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
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

    public String getTableCode() {
        return tableCode;
    }

    public void setTableCode(String tableCode) {
        this.tableCode = tableCode;
    }

    public String getTableVer() {
        return tableVer;
    }

    public void setTableVer(String tableVer) {
        this.tableVer = tableVer;
    }

    public BigDecimal getDbhMin() {
        return dbhMin;
    }

    public void setDbhMin(BigDecimal dbhMin) {
        this.dbhMin = dbhMin;
    }

    public BigDecimal getDbhMax() {
        return dbhMax;
    }

    public void setDbhMax(BigDecimal dbhMax) {
        this.dbhMax = dbhMax;
    }

    public BigDecimal getHeightMin() {
        return heightMin;
    }

    public void setHeightMin(BigDecimal heightMin) {
        this.heightMin = heightMin;
    }

    public BigDecimal getHeightMax() {
        return heightMax;
    }

    public void setHeightMax(BigDecimal heightMax) {
        this.heightMax = heightMax;
    }

    public BigDecimal getTreeVolume() {
        return treeVolume;
    }

    public void setTreeVolume(BigDecimal treeVolume) {
        this.treeVolume = treeVolume;
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
