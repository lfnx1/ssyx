package com.my.ssyx.payment.service.impl;

import com.my.ssyx.client.OrderFeignClient;
import com.my.ssyx.common.exception.SsyxException;
import com.my.ssyx.common.result.ResultCodeEnum;
import com.my.ssyx.enums.PaymentStatus;
import com.my.ssyx.enums.PaymentType;
import com.my.ssyx.model.order.OrderInfo;
import com.my.ssyx.model.order.PaymentInfo;
import com.my.ssyx.mq.constant.MqConst;
import com.my.ssyx.mq.service.RabbitService;
import com.my.ssyx.payment.mapper.PaymentInfoMapper;
import com.my.ssyx.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Service
public class PaymentInfoServiceImpl extends ServiceImpl <PaymentInfoMapper,PaymentInfo> implements PaymentInfoService {

    @Resource
    private OrderFeignClient orderFeignClient;

    @Resource
    private RabbitService rabbitService;
    @Override
    public PaymentInfo getPaymentInfoByOrderNo(String orderNo) {
        PaymentInfo paymentInfo = baseMapper.selectOne(new
                LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, orderNo));
        return paymentInfo;
    }

    @Override
    public PaymentInfo savePaymentInfo(String orderNo) {
        //远程调用  根据orderNo 查询订单信息
        OrderInfo order = orderFeignClient.getOrderInfo(orderNo);
        if (order==null){
            throw new SsyxException(ResultCodeEnum.DATA_ERROR);
        }
        //封装到PaymentInfo对象
        PaymentInfo  paymentInfo =new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(order.getId());
        paymentInfo.setPaymentType(PaymentType.WEIXIN);
        paymentInfo.setUserId(order.getUserId());
        paymentInfo.setOrderNo(order.getOrderNo());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        String subject = "userId:"+order.getUserId()+"下订单";
        paymentInfo.setSubject(subject);
        //paymentInfo.setTotalAmount(order.getTotalAmount());
        paymentInfo.setTotalAmount(new BigDecimal("0.01"));
        //添加
        baseMapper.insert(paymentInfo);
        return paymentInfo;
    }

    @Override
    public void paySuccess(String orderNo, Map<String, String> resultmap) {
        //3.1 支付成功 修改支付记录表状态 : 已经支付
        //查询当前订单表的状态是否为已经支付
        PaymentInfo paymentInfo = baseMapper.selectOne(
                new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, orderNo));
        if (paymentInfo.getPaymentStatus()!=PaymentStatus.UNPAID){
            return;
        }
        //如果没有 更新
        paymentInfo.setPaymentStatus(PaymentStatus.PAID);
        paymentInfo.setTradeNo(resultmap.get("transaction_id"));
        paymentInfo.setCallbackContent(resultmap.toString());
        baseMapper.updateById(paymentInfo);
        // 使用RabbitMQ 修改订单记录已经支付  库存扣减
        rabbitService.sendMessage(MqConst.
                EXCHANGE_PAY_DIRECT,MqConst.ROUTING_PAY_SUCCESS,orderNo);
    }
}
