package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.exception.biz.FlogBizException;
import com.fc.v2.mapper.auto.TFlogSurveyDesignMapper;
import com.fc.v2.mapper.auto.TFlogSurveyTreeMapper;
import com.fc.v2.model.auto.TFlogSurveyDesign;
import com.fc.v2.model.auto.TFlogSurveyTree;
import com.fc.v2.model.auto.TFlogVolumeTable;
import com.fc.v2.model.auto.TFlogYieldRule;
import com.fc.v2.model.custom.flog.FlogScopeKind;
import com.fc.v2.model.custom.flog.FlogSurveyDesignDto;
import com.fc.v2.model.custom.flog.FlogSurveyDesignVo;
import com.fc.v2.model.custom.flog.FlogSurveyTreeDto;
import com.fc.v2.model.custom.flog.FlogYieldEvalVo;
import com.fc.v2.service.ITFlogSurveyDesignService;
import com.fc.v2.service.ITFlogVolumeTableService;
import com.fc.v2.service.ITFlogYieldRuleService;
import com.fc.v2.util.SnowflakeIdWorker;
import com.fc.v2.util.StringUtils;

/**
 * 伐区调查设计单 Service业务层处理。
 *
 * 三级链只在这一处算清：
 * 一级（山场）检尺每株胸径树高 → 二级照同一份材积表把两数折成单株材积、逐株累加得小班蓄积
 * → 三级蓄积乘出材率、除小班面积得每公顷出材量，再按调查设计之日请出的那条界靠轻/中/重。
 * 界取哪一版、并线取哪一条、材积用哪版表，全在 {@link #evaluate} 现算现给；
 * 出单钉库的线代号与启用之日只认它的返回，页面凑的界一律不进门。
 *
 * @author fuce
 * @date 2026-09-30
 */
@Service
public class TFlogSurveyDesignServiceImpl implements ITFlogSurveyDesignService {

    /** 无线可依记号：钉在 rule_code 格上，这一趟不算断档、也不拿邻近日子的线凑 */
    public static final String NO_RULE_CODE = "无线可依";

    /** 版本现行标记：现行 */
    private static final int CURRENT_YES = 1;
    /** 版本现行标记：历史（让位版本，仅供回查） */
    private static final int CURRENT_NO = 0;

    /** 单子情形：草稿（未钉，本系统不出半钉状态，保留码位） */
    private static final int STATUS_DRAFT = 0;
    /** 单子情形：已钉（出单） */
    private static final int STATUS_PINNED = 1;
    /** 单子情形：无线可依（出单但档位空着，写明无线可依） */
    private static final int STATUS_NO_RULE = 2;

    /** 胸径到了四百九十九厘米往上，定是把数录错了（俗称“四位九”，4.99 录成 499 一类），不进链 */
    private static final BigDecimal DBH_ABSURD = new BigDecimal("499");
    /** 树高防呆上界(米) */
    private static final BigDecimal HEIGHT_ABSURD = new BigDecimal("200");
    /** 材积/蓄积/出材量统一保留 4 位 */
    private static final int VOL_SCALE = 4;

    @javax.annotation.Resource
    private TFlogSurveyDesignMapper designMapper;

    @javax.annotation.Resource
    private TFlogSurveyTreeMapper treeMapper;

    @javax.annotation.Resource
    private ITFlogVolumeTableService volumeTableService;

    @javax.annotation.Resource
    private ITFlogYieldRuleService yieldRuleService;

    @Override
    public FlogYieldEvalVo evaluate(FlogSurveyDesignDto dto) {
        validateHead(dto);

        // 林地类别随林种 + 采伐性质折：商品林单走一套，公益林三性质各管各的；公益林走主伐没有合法界可依
        FlogScopeKind scope = FlogScopeKind.resolve(dto.getForestKind(), dto.getCutMode());
        if (scope == null) {
            throw new FlogBizException("林种 " + dto.getForestKind() + " 配采伐方式 " + dto.getCutMode()
                    + " 翻不出合法的强度界类别：公益林依法只许抚育、更新、低质低效林改造三性质，这笔退回重录");
        }

        // 同一份检尺数，材积只许取一版表：版本在报文头指定一次，下面逐株都在这同一批表档里折
        List<TFlogVolumeTable> bins =
                volumeTableService.requireActiveVersion(dto.getVolumeCode(), dto.getVolumeVer());

        // 二级：逐株两数折材积，顺手把两数来路点干净
        List<FlogYieldEvalVo.TreeLine> lines = new ArrayList<>();
        BigDecimal totalVolume = BigDecimal.ZERO;
        int seq = 0;
        for (FlogSurveyTreeDto t : dto.getTrees()) {
            seq++;
            if (t == null || t.getDbh() == null || t.getTreeHeight() == null) {
                throw new FlogBizException("第 " + seq + " 株胸径、树高两数缺一笔记，整笔退回重录");
            }
            if (t.getDbh().compareTo(BigDecimal.ZERO) <= 0
                    || t.getTreeHeight().compareTo(BigDecimal.ZERO) <= 0) {
                throw new FlogBizException("第 " + seq + " 株胸径 " + t.getDbh() + "、树高 " + t.getTreeHeight()
                        + " 不是正数，量回来的数来路不对，整笔退回调查员重录");
            }
            if (t.getDbh().compareTo(DBH_ABSURD) >= 0 || t.getTreeHeight().compareTo(HEIGHT_ABSURD) >= 0) {
                throw new FlogBizException("第 " + seq + " 株胸径 " + t.getDbh() + " 厘米/树高 "
                        + t.getTreeHeight() + " 米明摆着录错了（胸径四位九一类），整笔退回调查员重录");
            }
            TFlogVolumeTable bin = locateBin(bins, t.getDbh(), t.getTreeHeight());
            if (bin == null) {
                throw new FlogBizException("第 " + seq + " 株（胸径 " + t.getDbh() + "、树高 "
                        + t.getTreeHeight() + "）在材积表 " + dto.getVolumeCode() + " 第 " + dto.getVolumeVer()
                        + " 版里翻不到档子，空档折不材积，整笔退回调查员重录");
            }
            BigDecimal v = bin.getTreeVolume().setScale(VOL_SCALE, RoundingMode.HALF_UP);
            totalVolume = totalVolume.add(v);
            lines.add(new FlogYieldEvalVo.TreeLine(seq, t.getDbh(), t.getTreeHeight(), v));
        }
        if (lines.isEmpty()) {
            throw new FlogBizException("检尺株数为零，没有一株进得了账，退回调查员重录");
        }
        totalVolume = totalVolume.setScale(VOL_SCALE, RoundingMode.HALF_UP);

        // 三级：蓄积 × 出材率 ÷ 小班面积 = 每公顷出材量（与逐株材积同一回计算，中间不许再取第二版表/第二条界）
        BigDecimal yieldPerHa = totalVolume.multiply(dto.getYieldRate())
                .divide(dto.getArea(), VOL_SCALE, RoundingMode.HALF_UP);

        FlogYieldEvalVo vo = new FlogYieldEvalVo();
        vo.setScopeKind(scope.getCode());
        vo.setScopeKindText(scope.getName());
        vo.setVolumeCode(dto.getVolumeCode().trim());
        vo.setVolumeVer(dto.getVolumeVer().trim());
        vo.setTreeCount(lines.size());
        vo.setTreeLines(lines);
        vo.setTotalVolume(totalVolume);
        vo.setYieldPerHa(yieldPerHa);
        vo.setSurveyDate(dto.getSurveyDate());

        // 取界只认调查设计那一日，不认今天；去年秋天的设计今年补结论，也请当时在场那条
        TFlogYieldRule rule = yieldRuleService.pickAvailable(scope.getCode(), dto.getSurveyDate());
        if (rule == null) {
            // 翻不出依据就写明无线可依，切勿拿邻近日子上的某条来凑
            vo.setNoRule(true);
            vo.setRuleCode(NO_RULE_CODE);
            vo.setNoRuleReason(scope.getName() + "在 " + day(dto.getSurveyDate())
                    + " 翻不出在场的强度界限值线，这一趟无线可依，不拿邻近日子的线凑");
            vo.setGradeLevel(null);
            vo.setGradeLevelText("无线可依");
            return vo;
        }
        int grade = yieldRuleService.classify(rule, yieldPerHa);
        if (grade == ITFlogYieldRuleService.GRADE_NONE) {
            // 那条界三数全空，评不出档：同样按无线可依写明
            vo.setNoRule(true);
            vo.setRuleCode(rule.getRuleCode());
            vo.setRuleEffStart(rule.getEffStart());
            vo.setNoRuleReason("界 " + rule.getRuleCode() + " 轻中重三条界数全空，评不出档位");
            vo.setGradeLevel(null);
            vo.setGradeLevelText("无线可依");
            return vo;
        }
        vo.setNoRule(false);
        vo.setRuleCode(rule.getRuleCode());
        vo.setRuleEffStart(rule.getEffStart());
        vo.setRuleBasis(buildBasis(rule, yieldPerHa, grade));
        vo.setGradeLevel(grade);
        vo.setGradeLevelText(gradeText(grade));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogSurveyDesignVo submit(FlogSurveyDesignDto dto) {
        // 先在唯一求值口把三级链走透；钉库的界代号、启用之日、档位全以这份返回为准
        FlogYieldEvalVo eval = evaluate(dto);

        String designNo;
        int nodeNo;
        Long reviseOfId = null;
        if (dto.getReviseOfId() != null) {
            // 整改复验：认现行版升版，旧版降为历史留检；两回结论各留各的代号，合成一条不算对
            TFlogSurveyDesign old = this.designMapper.selectById(dto.getReviseOfId());
            if (old == null || (old.getDelFlag() != null && old.getDelFlag() == 1)) {
                throw new FlogBizException("复验源单 " + dto.getReviseOfId() + " 查无此单，升不了版");
            }
            if (old.getCurrentFlag() == null || old.getCurrentFlag() != CURRENT_YES) {
                throw new FlogBizException("单号 " + old.getDesignNo()
                        + " 那一版已不是现行版，复验请按最新现行版再录一版");
            }
            // 同一张单：字号、小班仍须是同一对，换小班那是另立一张新单
            if (!eq(old.getSiteNo(), dto.getSiteNo() == null ? null : dto.getSiteNo().trim())
                    || !eq(old.getSubcompNo(), dto.getSubcompNo() == null ? null : dto.getSubcompNo().trim())) {
                throw new FlogBizException("复验只能就着原小班（" + old.getSiteNo() + "/"
                        + old.getSubcompNo() + "）重算；换了小班请另出新单，不许借复验挪单");
            }
            int rows = this.designMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<TFlogSurveyDesign>()
                            .eq("id", old.getId())
                            .eq("current_flag", CURRENT_YES)
                            .set("current_flag", CURRENT_NO));
            if (rows == 0) {
                throw new FlogBizException("单号 " + old.getDesignNo() + " 刚被另一笔复验顶下，本笔请刷新后再录");
            }
            designNo = old.getDesignNo();
            nodeNo = (old.getNodeNo() == null ? 1 : old.getNodeNo()) + 1;
            reviseOfId = old.getId();
        } else {
            if (StringUtils.isNotEmpty(dto.getDesignNo())) {
                Integer cnt = this.designMapper.selectCount(new QueryWrapper<TFlogSurveyDesign>()
                        .eq("design_no", dto.getDesignNo().trim())
                        .eq("del_flag", 0));
                if (cnt != null && cnt > 0) {
                    throw new FlogBizException("单号 " + dto.getDesignNo().trim()
                            + " 已在册；同小班整改复验请带原单id升版，不许另压一张同号单");
                }
                designNo = dto.getDesignNo().trim();
            } else {
                designNo = nextDesignNo(yearOf(dto.getSurveyDate()));
            }
            nodeNo = 1;
        }

        TFlogSurveyDesign d = new TFlogSurveyDesign();
        d.setDesignNo(designNo);
        d.setNodeNo(nodeNo);
        d.setReviseOfId(reviseOfId);
        d.setSiteNo(dto.getSiteNo().trim());
        d.setSubcompNo(dto.getSubcompNo().trim());
        d.setCutMode(dto.getCutMode());
        d.setForestKind(dto.getForestKind());
        d.setScopeKind(eval.getScopeKind());
        d.setTreeCount(eval.getTreeCount());
        d.setArea(dto.getArea());
        d.setYieldRate(dto.getYieldRate());
        d.setVolumeCode(eval.getVolumeCode());
        d.setVolumeVer(eval.getVolumeVer());
        d.setTotalVolume(eval.getTotalVolume());
        d.setYieldPerHa(eval.getYieldPerHa());
        d.setSurveyDate(dto.getSurveyDate());
        d.setSurveyBy(currentLoginName());
        d.setCurrentFlag(CURRENT_YES);
        d.setDelFlag(0);
        d.setRemark(dto.getRemark());

        String msg;
        if (eval.isNoRule()) {
            // 无线可依也出单：档位空着、写明无线可依，钉过不改口
            d.setGradeLevel(null);
            d.setRuleCode(NO_RULE_CODE);
            d.setRuleEffStart(null);
            d.setRuleBasis(eval.getNoRuleReason());
            d.setStatus(STATUS_NO_RULE);
            msg = "单号 " + designNo + " 第 " + nodeNo + " 版：" + eval.getNoRuleReason()
                    + "；蓄积 " + eval.getTotalVolume() + "、每公顷出材 " + eval.getYieldPerHa()
                    + " 已记账，强度档位空着待界";
        } else {
            // 评定结果连同所依代号、那条界启用之日一并钉进设计单，钉过不再改口
            d.setGradeLevel(eval.getGradeLevel());
            d.setRuleCode(eval.getRuleCode());
            d.setRuleEffStart(eval.getRuleEffStart());
            d.setRuleBasis(eval.getRuleBasis());
            d.setStatus(STATUS_PINNED);
            msg = "单号 " + designNo + " 第 " + nodeNo + " 版已钉：依界 " + eval.getRuleCode()
                    + "（启用 " + day(eval.getRuleEffStart()) + "），蓄积 " + eval.getTotalVolume()
                    + " 立方米、每公顷出材 " + eval.getYieldPerHa() + " 立方米/公顷，定为"
                    + eval.getGradeLevelText() + "；钉过不再改口";
        }
        this.designMapper.insert(d);

        // 检尺株账随版钉住：一株一行，材积是同一回折的那个数
        for (FlogYieldEvalVo.TreeLine line : eval.getTreeLines()) {
            TFlogSurveyTree tr = new TFlogSurveyTree();
            tr.setDesignId(d.getId());
            tr.setDesignNo(designNo);
            tr.setNodeNo(nodeNo);
            tr.setTreeSeq(line.getTreeSeq());
            tr.setDbh(line.getDbh());
            tr.setTreeHeight(line.getTreeHeight());
            tr.setTreeVolume(line.getTreeVolume());
            tr.setDelFlag(0);
            this.treeMapper.insert(tr);
        }

        FlogSurveyDesignVo vo = new FlogSurveyDesignVo();
        vo.setDesign(d);
        vo.setTreeLines(eval.getTreeLines());
        vo.setGradeLevelText(eval.getGradeLevelText());
        vo.setScopeKindText(eval.getScopeKindText());
        vo.setStatusText(statusText(d.getStatus()));
        vo.setMessage(msg);
        return vo;
    }

    @Override
    public TFlogSurveyDesign selectById(Long id) {
        if (id == null) {
            throw new FlogBizException("没给设计单id");
        }
        TFlogSurveyDesign d = this.designMapper.selectOne(new QueryWrapper<TFlogSurveyDesign>()
                .eq("id", id)
                .eq("del_flag", 0));
        if (d == null) {
            throw new FlogBizException("设计单 " + id + " 查无此版");
        }
        return d;
    }

    @Override
    public List<TFlogSurveyDesign> listCurrent(String siteNo, String subcompNo, Integer scopeKind, Integer status) {
        return this.designMapper.selectList(new QueryWrapper<TFlogSurveyDesign>()
                .eq("current_flag", CURRENT_YES)
                .eq("del_flag", 0)
                .eq(StringUtils.isNotEmpty(siteNo), "site_no",
                        StringUtils.isEmpty(siteNo) ? null : siteNo.trim())
                .eq(StringUtils.isNotEmpty(subcompNo), "subcomp_no",
                        StringUtils.isEmpty(subcompNo) ? null : subcompNo.trim())
                .eq(FlogScopeKind.of(scopeKind) != null, "scope_kind", scopeKind)
                .eq(status != null, "status", status)
                .orderByDesc("survey_date", "design_no"));
    }

    @Override
    public List<TFlogSurveyDesign> listVersions(String designNo) {
        if (StringUtils.isEmpty(designNo)) {
            return new ArrayList<>();
        }
        return this.designMapper.selectList(new QueryWrapper<TFlogSurveyDesign>()
                .eq("design_no", designNo.trim())
                .eq("del_flag", 0)
                .orderByAsc("node_no"));
    }

    @Override
    public List<TFlogSurveyTree> listTrees(Long designId) {
        if (designId == null) {
            return new ArrayList<>();
        }
        return this.treeMapper.selectList(new QueryWrapper<TFlogSurveyTree>()
                .eq("design_id", designId)
                .eq("del_flag", 0)
                .orderByAsc("tree_seq"));
    }

    // ------------------------------------------------------------------
    // 校验与折档
    // ------------------------------------------------------------------

    /** 报文头各栏点一遍：缺栏、非正数、越界数一律不进链。 */
    private void validateHead(FlogSurveyDesignDto dto) {
        if (dto == null) {
            throw new FlogBizException("递上来的设计单是空的，没东西可算");
        }
        List<String> missing = new ArrayList<>();
        if (StringUtils.isEmpty(dto.getSiteNo())) {
            missing.add("调查编制单位底档字号");
        }
        if (StringUtils.isEmpty(dto.getSubcompNo())) {
            missing.add("小班号");
        }
        if (com.fc.v2.model.custom.flog.FlogCutMode.of(dto.getCutMode()) == null) {
            missing.add("采伐方式（六类选一）");
        }
        if (dto.getForestKind() == null || (dto.getForestKind() != 1 && dto.getForestKind() != 2)) {
            missing.add("林种性质（公益林/商品林）");
        }
        if (dto.getArea() == null) {
            missing.add("小班面积");
        }
        if (dto.getYieldRate() == null) {
            missing.add("出材率");
        }
        if (StringUtils.isEmpty(dto.getVolumeCode())) {
            missing.add("材积表代号");
        }
        if (StringUtils.isEmpty(dto.getVolumeVer())) {
            missing.add("材积表版本");
        }
        if (dto.getSurveyDate() == null) {
            missing.add("调查设计之日（取界认这一天）");
        }
        if (dto.getTrees() == null || dto.getTrees().isEmpty()) {
            missing.add("检尺株账（株数为零进不了链）");
        }
        if (!missing.isEmpty()) {
            throw new FlogBizException("这笔检尺进不了三级链，缺了便起不来的格子：" + String.join("、", missing));
        }
        if (dto.getArea().compareTo(BigDecimal.ZERO) <= 0) {
            throw new FlogBizException("小班面积 " + dto.getArea() + " 不是正数，除面积这一步算不了，退回重录");
        }
        if (dto.getYieldRate().compareTo(BigDecimal.ZERO) <= 0
                || dto.getYieldRate().compareTo(BigDecimal.ONE) > 0) {
            throw new FlogBizException("出材率 " + dto.getYieldRate() + " 越界（得在 0 与 1 之间），退回重录");
        }
    }

    /** 一档区间：胸径 [min,max)、树高 [min,max)，档起含、档止不含。 */
    private TFlogVolumeTable locateBin(List<TFlogVolumeTable> bins, BigDecimal dbh, BigDecimal height) {
        for (TFlogVolumeTable b : bins) {
            boolean dbhIn = dbh.compareTo(b.getDbhMin()) >= 0 && dbh.compareTo(b.getDbhMax()) < 0;
            boolean hIn = height.compareTo(b.getHeightMin()) >= 0 && height.compareTo(b.getHeightMax()) < 0;
            if (dbhIn && hIn) {
                return b;
            }
        }
        return null;
    }

    /** 定档依据一句话：界上三数 + 本次每公顷出材量 + 落档，随单留查。 */
    private String buildBasis(TFlogYieldRule rule, BigDecimal yieldPerHa, int grade) {
        StringBuilder sb = new StringBuilder();
        sb.append("依界 ").append(rule.getRuleCode()).append("（").append(rule.getRuleName())
                .append("，启用 ").append(day(rule.getEffStart())).append("）：");
        sb.append("轻界 ").append(rule.getTh1Max() == null ? "不设" : rule.getTh1Max().toPlainString());
        sb.append("、中界 ").append(rule.getTh2Max() == null ? "不设" : rule.getTh2Max().toPlainString());
        sb.append("、重界 ").append(rule.getTh3Max() == null ? "不设上界" : rule.getTh3Max().toPlainString());
        sb.append("（立方米/公顷，挨着界往高里数）；每公顷出材 ").append(yieldPerHa.toPlainString())
                .append("，落").append(gradeText(grade));
        return sb.toString();
    }

    private String gradeText(int grade) {
        switch (grade) {
            case ITFlogYieldRuleService.GRADE_LIGHT:
                return "轻度";
            case ITFlogYieldRuleService.GRADE_MIDDLE:
                return "中度";
            case ITFlogYieldRuleService.GRADE_HEAVY:
                return "重度";
            default:
                return "无线可依";
        }
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case STATUS_PINNED:
                return "已钉";
            case STATUS_NO_RULE:
                return "无线可依";
            case STATUS_DRAFT:
                return "草稿";
            default:
                return String.valueOf(status);
        }
    }

    private boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private String day(Date d) {
        return d == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(d);
    }

    private int yearOf(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        return cal.get(Calendar.YEAR);
    }

    /** 调查设计单号自动排、撞号重试：SJ + 调查日那年 + 8 位尾。 */
    private String nextDesignNo(int year) {
        for (int i = 0; i < 10; i++) {
            String no = "SJ" + year + "-"
                    + String.format("%08d", Math.abs((SnowflakeIdWorker.getUUID() + "-" + i).hashCode())
                            % 100000000L);
            Integer cnt = this.designMapper.selectCount(new QueryWrapper<TFlogSurveyDesign>()
                    .eq("design_no", no)
                    .eq("del_flag", 0));
            if (cnt == null || cnt == 0) {
                return no;
            }
        }
        throw new FlogBizException("调查设计单号连撞，缓一缓再报");
    }

    private String currentLoginName() {
        try {
            return com.fc.v2.shiro.util.ShiroUtils.getLoginName();
        } catch (Exception e) {
            return null;
        }
    }
}
