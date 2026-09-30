package com.fc.v2.controller.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TFlogSurveyDesign;
import com.fc.v2.model.custom.flog.FlogSurveyDesignDto;
import com.fc.v2.model.custom.flog.FlogSurveyDesignVo;
import com.fc.v2.model.custom.flog.FlogYieldEvalVo;
import com.fc.v2.service.ITFlogSurveyDesignService;

/**
 * 伐区调查设计 Controller。
 *
 * 从检尺两数到出材量定档全跟服务层唯一求值口走：自核屏先现算（不落库）给调查员核对，
 * 出单时再由同一口径把档位、界代号、启用之日钉死；控制器与页面不凑界、不判档、不回写中间数。
 *
 * @author fuce
 * @date 2026-09-30
 */
@Api(value = "伐区调查设计")
@Controller
@RequestMapping("/FlogSurveyDesignController")
public class FlogSurveyDesignController extends BaseController {

    private final String prefix = "admin/flogSurveyDesign";

    @Autowired
    private ITFlogSurveyDesignService surveyDesignService;

    @GetMapping("/view")
    @RequiresPermissions("flog:flogSurveyDesign:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "设计单现行集合查询", action = "list")
    @GetMapping("/list")
    @RequiresPermissions("flog:flogSurveyDesign:list")
    @ResponseBody
    public ResultTable list(@RequestParam(value = "siteNo", required = false) String siteNo,
                            @RequestParam(value = "subcompNo", required = false) String subcompNo,
                            @RequestParam(value = "scopeKind", required = false) Integer scopeKind,
                            @RequestParam(value = "status", required = false) Integer status) {
        startPage();
        List<TFlogSurveyDesign> rows = surveyDesignService.listCurrent(siteNo, subcompNo, scopeKind, status);
        PageInfo<TFlogSurveyDesign> page = new PageInfo<>(rows);
        return pageTable(page.getList(), page.getTotal());
    }

    @GetMapping("/add")
    @RequiresPermissions("flog:flogSurveyDesign:add")
    public String add() {
        return prefix + "/add";
    }

    /** 整改复验跳转：就着现行版id另录一版 */
    @GetMapping("/revise/{id}")
    @RequiresPermissions("flog:flogSurveyDesign:add")
    public String revise(@PathVariable("id") Long id, ModelMap model) {
        model.put("reviseOfId", id);
        return prefix + "/add";
    }

    /** 自核屏：同一回算透但不落库，给调查员先看结论与所依界 */
    @Log(title = "设计单自核求值", action = "evaluate")
    @PostMapping("/evaluate")
    @RequiresPermissions("flog:flogSurveyDesign:add")
    @ResponseBody
    public AjaxResult evaluate(@RequestBody FlogSurveyDesignDto dto) {
        FlogYieldEvalVo vo = surveyDesignService.evaluate(dto);
        return AjaxResult.successData(200, vo);
    }

    /** 出单：同一回求值结果钉成设计单，钉过不再改口 */
    @Log(title = "设计单出单", action = "submit")
    @PostMapping("/submit")
    @RequiresPermissions("flog:flogSurveyDesign:add")
    @ResponseBody
    public AjaxResult submit(@RequestBody FlogSurveyDesignDto dto) {
        FlogSurveyDesignVo vo = surveyDesignService.submit(dto);
        return AjaxResult.success(200, vo.getMessage(), vo);
    }

    /** 抽查/详情：钉死值原样摆出，另附同回折出的检尺株账 */
    @GetMapping("/detail/{id}")
    @RequiresPermissions("flog:flogSurveyDesign:list")
    @ResponseBody
    public AjaxResult detail(@PathVariable("id") Long id) {
        TFlogSurveyDesign d = surveyDesignService.selectById(id);
        Map<String, Object> data = new HashMap<>();
        data.put("design", d);
        data.put("trees", surveyDesignService.listTrees(id));
        return AjaxResult.successData(200, data);
    }

    /** 历史版本：同号各版按版本序摆出，两回结论各留各的代号 */
    @GetMapping("/versions/{designNo}")
    @RequiresPermissions("flog:flogSurveyDesign:list")
    @ResponseBody
    public AjaxResult versions(@PathVariable("designNo") String designNo) {
        return AjaxResult.successData(200, surveyDesignService.listVersions(designNo));
    }

    /** 历史版本页跳转 */
    @GetMapping("/historyView")
    @RequiresPermissions("flog:flogSurveyDesign:list")
    public String historyView() {
        return prefix + "/history";
    }
}
