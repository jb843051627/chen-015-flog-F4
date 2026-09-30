package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.common.exception.biz.FlogBizException;
import com.fc.v2.mapper.auto.TFlogVolumeTableMapper;
import com.fc.v2.model.auto.TFlogVolumeTable;
import com.fc.v2.service.ITFlogVolumeTableService;
import com.fc.v2.util.StringUtils;

/**
 * 材积表 Service业务层处理。
 *
 * 一格一档：胸径落在 [dbhMin, dbhMax)、树高落在 [heightMin, heightMax) 即本档材积。
 * 一档翻不出（两数落进空档）不进三级链，退回调查员；同代号同版本的在场档子整版停用。
 *
 * @author fuce
 * @date 2026-09-30
 */
@Service
public class TFlogVolumeTableServiceImpl implements ITFlogVolumeTableService {

    /** 情形：在场可用 */
    private static final int STATUS_ACTIVE = 0;
    /** 情形：停用（只供旧单回查） */
    private static final int STATUS_STOPPED = 1;

    @javax.annotation.Resource
    private TFlogVolumeTableMapper volumeTableMapper;

    @Override
    public List<TFlogVolumeTable> listActive(String tableCode, String tableVer) {
        return this.volumeTableMapper.selectList(new QueryWrapper<TFlogVolumeTable>()
                .eq("del_flag", 0)
                .eq("status", STATUS_ACTIVE)
                .eq(StringUtils.isNotEmpty(tableCode), "table_code",
                        StringUtils.isEmpty(tableCode) ? null : tableCode.trim())
                .eq(StringUtils.isNotEmpty(tableVer), "table_ver",
                        StringUtils.isEmpty(tableVer) ? null : tableVer.trim())
                .orderByAsc("table_code", "table_ver", "dbh_min", "height_min"));
    }

    @Override
    public List<TFlogVolumeTable> listAll(String tableCode, String tableVer, Integer status) {
        return this.volumeTableMapper.selectList(new QueryWrapper<TFlogVolumeTable>()
                .eq("del_flag", 0)
                .eq(status != null, "status", status)
                .eq(StringUtils.isNotEmpty(tableCode), "table_code",
                        StringUtils.isEmpty(tableCode) ? null : tableCode.trim())
                .eq(StringUtils.isNotEmpty(tableVer), "table_ver",
                        StringUtils.isEmpty(tableVer) ? null : tableVer.trim())
                .orderByAsc("table_code", "table_ver", "status", "dbh_min", "height_min"));
    }

    @Override
    public TFlogVolumeTable register(TFlogVolumeTable input) {
        if (input == null) {
            throw new FlogBizException("送上来的表档是空的，没东西可录");
        }
        List<String> missing = new ArrayList<>();
        if (StringUtils.isEmpty(input.getTableCode())) {
            missing.add("材积表代号");
        }
        if (StringUtils.isEmpty(input.getTableVer())) {
            missing.add("材积表版本号");
        }
        if (input.getDbhMin() == null || input.getDbhMax() == null) {
            missing.add("胸径档起止");
        }
        if (input.getHeightMin() == null || input.getHeightMax() == null) {
            missing.add("树高档起止");
        }
        if (input.getTreeVolume() == null) {
            missing.add("本档单株材积");
        }
        if (!missing.isEmpty()) {
            throw new FlogBizException("这格表档录不进来，缺了便起不来的格子：" + String.join("、", missing));
        }
        if (input.getDbhMin().compareTo(input.getDbhMax()) >= 0) {
            throw new FlogBizException("胸径档止得高过档起（档起含、档止不含）");
        }
        if (input.getHeightMin().compareTo(input.getHeightMax()) >= 0) {
            throw new FlogBizException("树高档止得高过档起（档起含、档止不含）");
        }
        if (input.getDbhMin().compareTo(BigDecimal.ZERO) <= 0
                || input.getHeightMin().compareTo(BigDecimal.ZERO) <= 0) {
            throw new FlogBizException("胸径、树高档起都得数正数");
        }
        if (input.getTreeVolume().compareTo(BigDecimal.ZERO) <= 0) {
            throw new FlogBizException("单株材积得是正数");
        }
        // 同版同区间不许再压一格重叠档：重叠了一株折得出两个材积，三级链便对不上
        Integer overlap = this.volumeTableMapper.selectCount(new QueryWrapper<TFlogVolumeTable>()
                .eq("del_flag", 0)
                .eq("table_code", input.getTableCode().trim())
                .eq("table_ver", input.getTableVer().trim())
                .lt("dbh_min", input.getDbhMax())
                .gt("dbh_max", input.getDbhMin())
                .lt("height_min", input.getHeightMax())
                .gt("height_max", input.getHeightMin()));
        if (overlap != null && overlap > 0) {
            throw new FlogBizException("这一格与同版已有档子重叠，一株不许同时折出两个材积");
        }
        input.setId(null);
        input.setTableCode(input.getTableCode().trim());
        input.setTableVer(input.getTableVer().trim());
        input.setStatus(STATUS_ACTIVE);
        input.setDelFlag(0);
        this.volumeTableMapper.insert(input);
        return input;
    }

    @Override
    public int stopVersion(String tableCode, String tableVer) {
        if (StringUtils.isEmpty(tableCode) || StringUtils.isEmpty(tableVer)) {
            throw new FlogBizException("停用得指明材积表代号与版本");
        }
        return this.volumeTableMapper.update(null, new UpdateWrapper<TFlogVolumeTable>()
                .eq("table_code", tableCode.trim())
                .eq("table_ver", tableVer.trim())
                .eq("status", STATUS_ACTIVE)
                .eq("del_flag", 0)
                .set("status", STATUS_STOPPED));
    }

    @Override
    public List<TFlogVolumeTable> requireActiveVersion(String tableCode, String tableVer) {
        if (StringUtils.isEmpty(tableCode) || StringUtils.isEmpty(tableVer)) {
            throw new FlogBizException("得指明材积表代号与版本；三级链不许这头用一版、那头用另一版");
        }
        List<TFlogVolumeTable> bins = listActive(tableCode, tableVer);
        if (bins.isEmpty()) {
            throw new FlogBizException("材积表 " + tableCode.trim() + " 第 " + tableVer.trim()
                    + " 版翻不出在场表档（没录过或已停用），检尺数折不成材积，退回重选表版");
        }
        return bins;
    }
}
