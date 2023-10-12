package com.my.ssyx.order.service;

import com.my.ssyx.model.order.OrderInfo;
import com.my.ssyx.vo.order.OrderConfirmVo;
import com.my.ssyx.vo.order.OrderSubmitVo;
import com.my.ssyx.vo.order.OrderUserQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrderInfoService extends IService<OrderInfo> {

    OrderConfirmVo confirmOrder();

    Long submitOrder(OrderSubmitVo orderParamVo);

    OrderInfo getOrderInfoById(Long orderId);

    OrderInfo getOrderInfoByOrderNo(String orderNo);

    void orderPay(String orderVo);

    IPage<OrderInfo> getOrderInfoByUserIdPage(Page<OrderInfo> pageParam, OrderUserQueryVo orderUserQueryVo);
}
