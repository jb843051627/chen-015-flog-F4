package com.fc.v2.controller.admin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TFlogYieldRule;
import com.fc.v2.service.ITFlogYieldRuleService;

/**
 * 可采强度界限值界册 Controller。
 *
 * 界册只摆与立：换版只添不改，旧版让位走交棒一口（effEnd 不含），
 * 自核屏与抽查屏取线都过服务层同一时段口径，本控制器不另算档、不判在场。
 *
 * @author fuce
 * @date 2026-09-30
 */
@Api(value = "可采强度界限值界册")
@Controller
@RequestMapping("/FlogYieldRuleController")
public class FlogYieldRuleController extends BaseController {

    private final String prefix = "admin/flogYieldRule";

    @Autowired
    private ITFlogYieldRuleService yieldRuleService;

    @GetMapping("/view")
    @RequiresPermissions("flog:flogYieldRule:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "界册全版查询", action = "list")
    @GetMapping("/list")
    @RequiresPermissions("flog:flogYieldRule:list")
    @ResponseBody
    public ResultTable list(@RequestParam(value = "scopeKind", required = false) Integer scopeKind,
                            @RequestParam(value = "status", required = false) Integer status,
                            @RequestParam(value = "ruleCode", required = false) String ruleCode) {
        startPage();
        List<TFlogYieldRule> rows = yieldRuleService.listAllVersions(scopeKind, status, ruleCode);
        PageInfo<TFlogYieldRule> page = new PageInfo<>(rows);
        return pageTable(page.getList(), page.getTotal());
    }

    @GetMapping("/add")
    @RequiresPermissions("flog:flogYieldRule:add")
    public String add() {
        return prefix + "/add";
    }

    @Log(title = "界册立线", action = "add")
    @PostMapping("/add")
    @RequiresPermissions("flog:flogYieldRule:add")
    @ResponseBody
    public AjaxResult add(@org.springframework.web.bind.annotation.RequestBody TFlogYieldRule rule) {
        // id、情形旗子由系统定：新界一律现行入场，让位只走交棒那一口
        rule.setId(null);
        rule.setStatus(null);
        TFlogYieldRule saved = yieldRuleService.registerRule(rule);
        return AjaxResult.success(200, "界 " + saved.getRuleCode() + " 已立，启用之日 "
                + new SimpleDateFormat("yyyy-MM-dd").format(saved.getEffStart()), saved);
    }

    @Log(title = "界版交棒让位", action = "handover")
    @PostMapping("/handover/{id}")
    @RequiresPermissions("flog:flogYieldRule:add")
    @ResponseBody
    public AjaxResult handover(@PathVariable("id") Long id,
                               @RequestParam("effEnd") String effEnd) {
        Date day;
        try {
            day = new SimpleDateFormat("yyyy-MM-dd").parse(effEnd);
        } catch (ParseException e) {
            return AjaxResult.error("交棒之日按 yyyy-MM-dd 给，例如 2026-09-01");
        }
        TFlogYieldRule rule = yieldRuleService.handover(id, day);
        return AjaxResult.success(200,
                "界 " + rule.getRuleCode() + " 已交棒：管到 " + effEnd + " 前一日为止，当天起让位，只供回查", rule);
    }
}
