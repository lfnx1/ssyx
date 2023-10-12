package com.my.ssyx.order.service.impl;

import com.my.ssyx.model.order.OrderItem;
import com.my.ssyx.order.mapper.OrderItemMapper;
import com.my.ssyx.order.service.OrderItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper,OrderItem> implements OrderItemService {
}
