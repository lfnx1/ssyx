package com.my.ssyx.sys.controller;


import com.my.ssyx.common.result.Result;
import com.my.ssyx.model.sys.Region;
import com.my.ssyx.sys.service.RegionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 地区表 前端控制器
 * </p>
 *
 * @author lfnx
 * @since 2023-06-17
 */
@RestController
@RequestMapping("/admin/sys/region")
@Api(tags = "区域接口")
public class RegionController {
        @Autowired
        private RegionService regionService;

        //根据关键字查询区域列表信息
        @ApiOperation("根据关键字查询区域列表信息")
        @GetMapping("findRegionByKeyword/{keyword}")
        public Result findRegionByKeyword(@PathVariable("keyword") String keyword){
              List<Region> list = regionService.getRegionByKeyword(keyword);
              return Result.ok(list);
        }



}

