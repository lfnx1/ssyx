package com.my.ssyx.payment.controller;

import com.my.ssyx.common.result.Result;
import com.my.ssyx.payment.service.PaymentInfoService;
import com.my.ssyx.payment.service.WeixinService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/api/payment/weixin")
@Api(tags = "微信支付接口")
public class WeixinController {
    @Resource
    private WeixinService weixinService;

    @Resource
    private PaymentInfoService paymentInfoService;

    @ApiOperation(value = "下单 小程序支付")
    @GetMapping("/createJsapi/{orderNo}")
    public Result createJsapi(@PathVariable("orderNo") String orderNo){
        Map<String,String> map =weixinService.createJsapi(orderNo);
        return Result.ok(map);
    }

    @ApiOperation("查看订单状态")
    @GetMapping("queryPayStatus/{orderNo}")
    public Result queryPayStatus(@PathVariable("orderNo") String orderNo){
        //1 调用微信支付系统接口查询订单支付状态
        Map<String,String> resultmap=weixinService.queryPayStatus(orderNo);
        //2 微信支付系统返回值为null 支付失败
        if (resultmap==null){
            return Result.build(null,243,"订单支付失败" );
        }
        //3 如果微信支付系统返回值 判断支付成功
        if ("SUCCESS".equals(resultmap.get("trade_state"))){
            String out_trade_no = resultmap.get("out_trade_no");
            paymentInfoService.paySuccess(out_trade_no,resultmap);
            return Result.ok(null);
        }

        //4.支付中 等待
        return Result.build(null,241,"订单支付中");
    }
}
