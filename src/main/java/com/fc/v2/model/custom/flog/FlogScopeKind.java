package com.fc.v2.model.custom.flog;

/**
 * 界册所管林地类别。
 *
 * 商品林单走一类；公益林依法只许抚育、更新、低质低效林改造三性质，
 * 三类各有各的界，定档时按林种 + 采伐性质各认各的线，不许串用。
 *
 * @author fuce
 * @date 2026-09-30
 */
public enum FlogScopeKind {

    /** 商品林（采伐方式不限，强度界单走一套） */
    COMMERCIAL(1, "商品林"),
    /** 公益林·抚育采伐性质 */
    WELFARE_TENDING(2, "公益林-抚育"),
    /** 公益林·更新采伐性质 */
    WELFARE_RENEWAL(3, "公益林-更新"),
    /** 公益林·低质低效林改造性质 */
    WELFARE_REFORM(4, "公益林-低质低效林改造");

    private final int code;
    private final String name;

    FlogScopeKind(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static FlogScopeKind of(Integer code) {
        if (code == null) {
            return null;
        }
        for (FlogScopeKind k : values()) {
            if (k.code == code) {
                return k;
            }
        }
        return null;
    }

    /**
     * 按林种性质与采伐方式折出该用哪一类界。
     * 公益林走主伐（皆伐/渐伐/择伐）翻不出合法类别，返回 null —— 调用方按非法退回。
     */
    public static FlogScopeKind resolve(Integer forestKind, Integer cutMode) {
        if (forestKind == null || cutMode == null) {
            return null;
        }
        if (forestKind == 2) {
            return COMMERCIAL;
        }
        if (forestKind == 1) {
            switch (cutMode) {
                case 4:
                    return WELFARE_TENDING;
                case 5:
                    return WELFARE_RENEWAL;
                case 6:
                    return WELFARE_REFORM;
                default:
                    return null;
            }
        }
        return null;
    }
}
