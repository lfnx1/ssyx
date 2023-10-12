package com.my.ssyx.cart.controller;

import com.my.ssyx.activity.client.ActivityFeignClient;
import com.my.ssyx.cart.service.CartInfoService;
import com.my.ssyx.common.auth.AuthContextHolder;
import com.my.ssyx.common.result.Result;
import com.my.ssyx.model.order.CartInfo;
import com.my.ssyx.vo.order.OrderConfirmVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartApiController {
    @Autowired
    private CartInfoService cartInfoService;

    @Autowired
    private ActivityFeignClient activityFeignClient;

    //获取当前用户购物车选中购物项
    @GetMapping("inner/getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable Long userId){
        return cartInfoService.getCartCheckList(userId);
    }

    //修改选中状态
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId,
                            @PathVariable Integer isChecked){
        //获取用户id
        Long userId = AuthContextHolder.getUserId();
        //调用方法
        cartInfoService.checckCart(userId,skuId,isChecked);
        return Result.ok(null);
    }

    //全选
    @GetMapping("checkAllCart/{isChecked}")
    public Result checkAllCart(@PathVariable Integer isChecked){
        Long userId = AuthContextHolder.getUserId();
        cartInfoService.checkAllCart(userId,isChecked);
        return Result.ok(null);
    }

    //批量选中
    @ApiOperation(value="批量选择购物车")
    @PostMapping("batchCheckCart/{isChecked}")
    public Result batchCheckCart(@RequestBody List<Long> skuIdList,
                                 @PathVariable Integer isChecked){
        Long userId = AuthContextHolder.getUserId();
        cartInfoService.batchCheckCart(userId,skuIdList,isChecked);
        return Result.ok(null);
    }

    @GetMapping("addToCart/{skuId}/{skuNum}")
    private Result addToCart(@PathVariable Long skuId,
                             @PathVariable Integer skuNum){
        Long userId = AuthContextHolder.getUserId();
        cartInfoService.addToCart(skuId, userId, skuNum);
        return Result.ok(null);
    }

    //根据skuId删除购物车
    @DeleteMapping("deleteCart/{skuId}")
    public Result deleteCart(@PathVariable("skuId") Long skuId){
        Long userId = AuthContextHolder.getUserId();
        cartInfoService.deleteCart(skuId,userId);
        return Result.ok(null);
    }

    //清空购物车
    @ApiOperation(value="清空购物车")
    @DeleteMapping("deleteAllCart")
    public Result deleteAllCart(){
        Long userId = AuthContextHolder.getUserId();
        cartInfoService.deleteAllCart(userId);
        return Result.ok(null);
    }

    //批量删除购物车 skuIdList
    @ApiOperation(value="批量删除购物车")
    @PostMapping("batchDeleteCart")
    public Result batchDeleteCart(@RequestBody List<Long> skuIdList){
        Long userId = AuthContextHolder.getUserId();
        cartInfoService.batchDeleteCart(skuIdList,userId);
        return Result.ok(null);
    }

    //购物车列表
    @GetMapping("cartList")
    public Result cartList(){
        Long userId = AuthContextHolder.getUserId();
        List<CartInfo> cartInfoList=cartInfoService.getcartList(userId);
        return Result.ok(cartInfoList);
    }

    //查看带优惠券的购物车
    @GetMapping("activityCartList")
    public Result activityCartList() {
        // 获取用户Id
        Long userId = AuthContextHolder.getUserId();
        List<CartInfo> cartInfoList = cartInfoService.getcartList(userId);

        OrderConfirmVo orderTradeVo = activityFeignClient.findCartActivityAndCoupon(cartInfoList, userId);
        return Result.ok(orderTradeVo);
    }
}
