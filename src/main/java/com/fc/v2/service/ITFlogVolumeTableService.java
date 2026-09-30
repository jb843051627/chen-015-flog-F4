package com.fc.v2.service;

import java.util.List;

import com.fc.v2.model.auto.TFlogVolumeTable;

/**
 * 材积表 Service接口。
 *
 * 同代号可多版并册；调查设计单钉哪一版，三级链中蓄积回写与强度评定就得同用那一版。
 * 停用的版本只供旧单回查，新单取表取不在它头上。
 *
 * @author fuce
 * @date 2026-09-30
 */
public interface ITFlogVolumeTableService {

    /** 在场表档查询（status=0；code/ver 给则限定） */
    List<TFlogVolumeTable> listActive(String tableCode, String tableVer);

    /** 全版表档（含停用，供旧单回查表档来历） */
    List<TFlogVolumeTable> listAll(String tableCode, String tableVer, Integer status);

    /** 录一格表档（区间 dbh∈[min,max)、height∈[min,max)，材积正数） */
    TFlogVolumeTable register(TFlogVolumeTable row);

    /** 停用整版（已钉在旧设计单上的表档仍翻得到，只是新单取不在它头上） */
    int stopVersion(String tableCode, String tableVer);

    /**
     * 请出某代号某版本的在场全部表档；版本不存在或已停用按翻不出处理（调用方据此退回重录）。
     */
    List<TFlogVolumeTable> requireActiveVersion(String tableCode, String tableVer);
}
