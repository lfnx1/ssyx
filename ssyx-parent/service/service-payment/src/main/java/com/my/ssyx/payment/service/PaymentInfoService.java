package com.my.ssyx.payment.service;

import com.my.ssyx.model.order.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface PaymentInfoService extends IService<PaymentInfo> {
    PaymentInfo getPaymentInfoByOrderNo(String orderNo);

    PaymentInfo savePaymentInfo(String orderNo);

    void paySuccess(String outTradeNo, Map<String, String> resultmap);
}
