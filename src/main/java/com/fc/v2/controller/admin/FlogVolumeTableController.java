package com.fc.v2.controller.admin;

import java.util.List;

import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TFlogVolumeTable;
import com.fc.v2.service.ITFlogVolumeTableService;

/**
 * 材积表册 Controller：表档录入、在场/停用各版查询、整版停用。
 *
 * @author fuce
 * @date 2026-09-30
 */
@Api(value = "材积表册")
@Controller
@RequestMapping("/FlogVolumeTableController")
public class FlogVolumeTableController extends BaseController {

    private final String prefix = "admin/flogVolumeTable";

    @Autowired
    private ITFlogVolumeTableService volumeTableService;

    @GetMapping("/view")
    @RequiresPermissions("flog:flogVolumeTable:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "材积表档查询", action = "list")
    @GetMapping("/list")
    @RequiresPermissions("flog:flogVolumeTable:list")
    @ResponseBody
    public ResultTable list(@RequestParam(value = "tableCode", required = false) String tableCode,
                            @RequestParam(value = "tableVer", required = false) String tableVer,
                            @RequestParam(value = "status", required = false) Integer status) {
        startPage();
        List<TFlogVolumeTable> rows = volumeTableService.listAll(tableCode, tableVer, status);
        PageInfo<TFlogVolumeTable> page = new PageInfo<>(rows);
        return pageTable(page.getList(), page.getTotal());
    }

    @GetMapping("/add")
    @RequiresPermissions("flog:flogVolumeTable:add")
    public String add() {
        return prefix + "/add";
    }

    @Log(title = "材积表档录入", action = "add")
    @PostMapping("/add")
    @RequiresPermissions("flog:flogVolumeTable:add")
    @ResponseBody
    public AjaxResult add(TFlogVolumeTable row) {
        row.setId(null);
        row.setStatus(null);
        TFlogVolumeTable saved = volumeTableService.register(row);
        return AjaxResult.success(200,
                "表档已录：" + saved.getTableCode() + " 第 " + saved.getTableVer() + " 版", saved);
    }

    @Log(title = "材积表整版停用", action = "stop")
    @PostMapping("/stop")
    @RequiresPermissions("flog:flogVolumeTable:add")
    @ResponseBody
    public AjaxResult stop(@RequestParam("tableCode") String tableCode,
                           @RequestParam("tableVer") String tableVer) {
        int n = volumeTableService.stopVersion(tableCode, tableVer);
        if (n == 0) {
            return AjaxResult.error("该版本没有在场表档可停（没录过或已停用；旧单上钉着的仍翻得到）");
        }
        return AjaxResult.success("已停用 " + n + " 格表档，已钉旧单仍可回查，新单取不在它头上");
    }
}
