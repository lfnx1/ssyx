package com.my.ssyx.product.controller;


import com.my.ssyx.common.result.Result;
import com.my.ssyx.model.product.Attr;
import com.my.ssyx.product.service.AttrService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 商品属性 前端控制器
 * </p>
 *
 * @author lfnx
 * @since 2023-06-18
 */
@RestController
@RequestMapping("/admin/product/attr")
public class AttrController {
    @Autowired
    AttrService attrService;

//    @Autowired
//    AttrMapper attrMapper;

    //平台属性列表
    //根据平台属性分组id查询
    @ApiOperation("根据平台属性分组id查询")
    @GetMapping("{attrGroupId}")
    public Result list(@PathVariable Long attrGroupId){
//        List<Attr> list = attrMapper.selectList(new QueryWrapper<Attr>().eq("attr_group_id", groupId));
        List<Attr> list =attrService.getAttrListByGroupId(attrGroupId);
        return Result.ok(list);
    }
    @ApiOperation(value = "获取")
    @GetMapping("get/{id}")
    public Result get(@PathVariable Long id) {
        Attr attr = attrService.getById(id);
        return Result.ok(attr);
    }

    @ApiOperation(value = "新增")
    @PostMapping("save")
    public Result save(@RequestBody Attr attr) {
        attrService.save(attr);
        return Result.ok(null);
    }

    @ApiOperation(value = "修改")
    @PutMapping("update")
    public Result updateById(@RequestBody Attr attr) {
        attrService.updateById(attr);
        return Result.ok(null);
    }

    @ApiOperation(value = "删除")
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id) {
        attrService.removeById(id);
        return Result.ok(null);
    }

    @ApiOperation(value = "根据id列表删除")
    @DeleteMapping("batchRemove")
    public Result batchRemove(@RequestBody List<Long> idList) {
        attrService.removeByIds(idList);
        return Result.ok(null);
    }



}

