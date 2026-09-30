package com.fc.v2.model.custom.flog;

import java.math.BigDecimal;

/**
 * 一株的检尺两数：胸径(厘米)、树高(米)。
 * 株序由服务层按进门顺序给，页面不许自带。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class FlogSurveyTreeDto {

    /** 胸径(厘米) */
    private BigDecimal dbh;

    /** 树高(米) */
    private BigDecimal treeHeight;

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
}
