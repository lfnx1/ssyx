package com.my.ssyx.sys.controller;


import com.my.ssyx.common.result.Result;
import com.my.ssyx.model.sys.Ware;
import com.my.ssyx.sys.service.WareService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 仓库表 前端控制器
 * </p>
 *
 * @author lfnx
 * @since 2023-06-17
 */
@RestController
@RequestMapping("/admin/sys/ware")
@Api(tags = "仓库接口")
public class WareController {
    @Autowired
    WareService wareService;

    @ApiOperation("查询所有仓库")
    @GetMapping("findAllList")
    public Result findList(){
        List<Ware> list = wareService.list();
        return Result.ok(list);
    }


}

