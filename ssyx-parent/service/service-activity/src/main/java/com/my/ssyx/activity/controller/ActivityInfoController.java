package com.my.ssyx.activity.controller;


import com.my.ssyx.activity.service.ActivityInfoService;
import com.my.ssyx.common.result.Result;
import com.my.ssyx.model.activity.ActivityInfo;
import com.my.ssyx.model.product.SkuInfo;
import com.my.ssyx.vo.activity.ActivityRuleVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 活动表 前端控制器
 * </p>
 *
 * @author lfnx
 * @since 2023-06-21
 */
@RestController
@RequestMapping("/admin/activity/activityInfo")
@Api(tags = "活动接口")
public class ActivityInfoController {
    @Autowired
    private ActivityInfoService activityInfoService;

    @ApiOperation(value = "获取分页列表")
    @GetMapping("{page}/{limit}")
    public Result list(@PathVariable Long page,
                       @PathVariable Long limit){
        Page<ActivityInfo> pagePram= new Page<>(page,limit);
        IPage<ActivityInfo> pageModel=activityInfoService.getPage(pagePram);
        return Result.ok(pageModel);
    }

    @ApiOperation(value = "获取活动")
    @GetMapping("get/{id}")
    public Result get(@PathVariable Long id) {
        ActivityInfo activityInfo = activityInfoService.getById(id);
        activityInfo.setActivityTypeString(activityInfo.getActivityType().getComment());
        return Result.ok(activityInfo);
    }

    @ApiOperation("新增活动")
    @PostMapping("save")
    public Result save(@RequestBody ActivityInfo activityInfo){
        boolean save = activityInfoService.save(activityInfo);
        return Result.ok(null);
    }


    @ApiOperation(value = "修改活动")
    @PutMapping("update")
    public Result updateById(@RequestBody ActivityInfo activityInfo) {
        activityInfoService.updateById(activityInfo);
        return Result.ok(null);
    }

    @ApiOperation(value = "删除活动")
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id) {
        activityInfoService.removeById(id);
        return Result.ok(null);
    }

    @ApiOperation(value="根据id列表删除活动")
    @DeleteMapping("batchRemove")
    public Result batchRemove(@RequestBody List<String> idList){
        activityInfoService.removeByIds(idList);
        return Result.ok(null);
    }

    //营销活动规则相关接口
    //1.根据活动id获取活动规则数据
    @ApiOperation(value = "根据活动id获取活动规则数据")
    @GetMapping("findActivityRuleList/{id}")
    public Result findActivityRuleList(@PathVariable Long id){
        Map<String,Object> ActivityRuleMap =activityInfoService.findActivityRuleList(id);
        return Result.ok(ActivityRuleMap);
    }
    //2.在活动里面添加活动规则数据
    @PostMapping("saveActivityRule")
    public Result saveActivityRule(@RequestBody ActivityRuleVo activityRuleVo){
        activityInfoService.saveActivityRule(activityRuleVo);
        return Result.ok(null);
    }

    //3.根据关键字查询匹配sku信息
    @GetMapping("findSkuInfoByKeyword/{keyword}")
    public Result findSkuInfoByKeyword(@PathVariable String keyword){
        List<SkuInfo> list=activityInfoService.findSkuInfoByKeyword(keyword);
        return Result.ok(list);
    }

}

