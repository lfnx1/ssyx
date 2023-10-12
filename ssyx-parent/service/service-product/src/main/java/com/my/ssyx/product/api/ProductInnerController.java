package com.my.ssyx.product.api;

import com.my.ssyx.model.product.Category;
import com.my.ssyx.model.product.SkuInfo;
import com.my.ssyx.product.service.CategoryService;
import com.my.ssyx.product.service.SkuInfoService;
import com.my.ssyx.vo.product.SkuInfoVo;
import com.my.ssyx.vo.product.SkuStockLockVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
public class ProductInnerController {
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SkuInfoService skuInfoService;

    //分类信息
    @ApiOperation(value = "根据分类id获取分类信息")
    @GetMapping("inner/getCategory/{categoryId}")
    public Category getCategory(@PathVariable Long categoryId) {
        return categoryService.getById(categoryId);
    }


    @ApiOperation(value = "根据skuId获取sku信息")
    @GetMapping("inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable("skuId") Long skuId) {
        return skuInfoService.getById(skuId);
    }

    @ApiOperation(value = "根据skuId列表获取sku信息")
    @PostMapping("inner/findSkuInfoList")
    public List<SkuInfo> findSkuInfoList(@RequestBody List<Long> SkuIdList){
        return skuInfoService.findSkuInfoList(SkuIdList);
    }

    @ApiOperation(value = "根据关键字查询匹配列表")
    @GetMapping("inner/findSkuInfoByKeyword/{keyword}")
    public List<SkuInfo> findSkuInfoByKeyword(@PathVariable String keyword){
        List<SkuInfo> skuInfoList=skuInfoService.findSkuInfoByKeyword(keyword);
        return skuInfoList;
    }

    @ApiOperation("根据种类Id列表获取种类信息")
    @PostMapping("inner/findCategoryList")
    public List<Category> findCategoryList(@RequestBody List<Long> categoryId){
        List<Category> categoryList = categoryService.listByIds(categoryId);
        return categoryList;
    }

    //获取所有分类
    @ApiOperation(value = "获取分类信息")
    @GetMapping("inner/findAllCategoryList")
    public List<Category> findAllCategoryList(){
        List<Category> list = categoryService.list();
        return list;
    }


    @ApiOperation(value = "获取新人专享")
    @GetMapping("inner/findNewPersonSkuInfoList")
    public List<SkuInfo> findNewPersonSkuInfoList(){
        List<SkuInfo> skuInfoList=skuInfoService.findNewPersonSkuInfoList();
        return skuInfoList;
    }

    @ApiOperation("根据Id查询sku详细信息")
    @GetMapping("inner/getSkuInfoVo/{skuId}")
    public SkuInfoVo getSkuInfoVo(@PathVariable Long skuId){
       return skuInfoService.getSkuInfoVo(skuId);
    }


    //验证和锁定库存
    @ApiOperation("验证和锁定库存")
    @PostMapping("inner/checkAndLock/{orderNo}")
    public Boolean checkAndLock(@RequestBody List<SkuStockLockVo> skuStockLockVoList, @PathVariable String orderNo) {
        return skuInfoService.checkAndLock(skuStockLockVoList,orderNo);
    }

}
