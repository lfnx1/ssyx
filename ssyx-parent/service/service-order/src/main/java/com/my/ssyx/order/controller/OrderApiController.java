package com.my.ssyx.order.controller;

import com.my.ssyx.common.auth.AuthContextHolder;
import com.my.ssyx.common.result.Result;
import com.my.ssyx.model.order.OrderInfo;
import com.my.ssyx.order.service.OrderInfoService;
import com.my.ssyx.vo.order.OrderConfirmVo;
import com.my.ssyx.vo.order.OrderSubmitVo;
import com.my.ssyx.vo.order.OrderUserQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Api(value = "Order管理", tags = "Order管理")
@RestController
@RequestMapping("/api/order")
public class OrderApiController {
    @Resource
    private OrderInfoService orderService;

    //订单查询
    @ApiOperation(value = "获取用户订单分页列表")
    @GetMapping("auth/findUserOrderPage/{page}/{limit}")
    public Result findUserOrderPage(
            @ApiParam(name = "page", value = "当前页码", required = true)
            @PathVariable Long page,

            @ApiParam(name = "limit", value = "每页记录数", required = true)
            @PathVariable Long limit,

            @ApiParam(name = "orderVo", value = "查询对象", required = false)
            OrderUserQueryVo orderUserQueryVo){
            //获取用户id
        Long userId = AuthContextHolder.getUserId();
        orderUserQueryVo.setUserId(userId);
        Page<OrderInfo> pageParam =new Page<>();
        IPage<OrderInfo> pageModel=orderService.getOrderInfoByUserIdPage(pageParam,orderUserQueryVo);
        return Result.ok(pageModel);
    }

    @ApiOperation("确认订单")
    @GetMapping("auth/confirmOrder")
    public Result confirm() {
       OrderConfirmVo orderConfirmVo= orderService.confirmOrder();
        return Result.ok(orderConfirmVo);
    }

    @ApiOperation("生成订单")
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderSubmitVo orderParamVo) {
        // 获取到用户Id
        Long userId = AuthContextHolder.getUserId();
        Long orderId=orderService.submitOrder(orderParamVo);
        return Result.ok(orderId);
    }

    @ApiOperation("获取订单详情")
    @GetMapping("auth/getOrderInfoById/{orderId}")
    public Result getOrderInfoById(@PathVariable("orderId") Long orderId){
            OrderInfo orderInfo=orderService.getOrderInfoById(orderId);
        return Result.ok(orderInfo);
    }


    //根据orderNo查询订单信息
    @GetMapping("inner/getOrderInfo/{orderNo}")
    public OrderInfo getOrderInfo (@PathVariable String orderNo){
        OrderInfo orderInfo =orderService.getOrderInfoByOrderNo(orderNo);
        return orderInfo;
    }


}
