package com.my.ssyx.product.controller;


import com.my.ssyx.common.result.Result;
import com.my.ssyx.model.product.AttrGroup;
import com.my.ssyx.product.service.AttrGroupService;
import com.my.ssyx.vo.product.AttrGroupQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 属性分组 前端控制器
 * </p>
 *
 * @author lfnx
 * @since 2023-06-18
 */
@RestController
@RequestMapping("/admin/product/attrGroup")
@Api(tags = "分组属性接口")
public class AttrGroupController {
    @Autowired
    AttrGroupService attrGroupService;

    @ApiOperation("平台属性分组列表")
    @GetMapping("{page}/{limit}")
    public Result list(@PathVariable Long page,
                       @PathVariable Long limit,
                       AttrGroupQueryVo attrGroupQueryVo){
        Page<AttrGroup> pagePram =new Page<>();
       IPage<AttrGroup> pageModel= attrGroupService.selectPageAttrGroup(pagePram,attrGroupQueryVo);
       return Result.ok(pageModel);
    }
    @ApiOperation(value = "获取")
    @GetMapping("get/{id}")
    public Result get(@PathVariable Long id) {
        AttrGroup attrGroup = attrGroupService.getById(id);
        return Result.ok(attrGroup);
    }

    @ApiOperation(value = "新增")
    @PostMapping("save")
    public Result save(@RequestBody AttrGroup attrGroup) {
        attrGroupService.save(attrGroup);
        return Result.ok(null);
    }

    @ApiOperation(value = "修改")
    @PutMapping("update")
    public Result updateById(@RequestBody AttrGroup attrGroup) {
        attrGroupService.updateById(attrGroup);
        return Result.ok(null);
    }

    @ApiOperation(value = "删除")
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id) {
        attrGroupService.removeById(id);
        return Result.ok(null);
    }

    @ApiOperation(value = "根据id列表删除")
    @DeleteMapping("batchRemove")
    public Result batchRemove(@RequestBody List<Long> idList) {
        attrGroupService.removeByIds(idList);
        return Result.ok(null);
    }

    @ApiOperation("所有平台属性分组列表")
    @GetMapping("findAllList")
    public Result findAllList(){
        List<AttrGroup> list = attrGroupService.list();
        return Result.ok(list);
    }
}

