package com.my.ssyx.mq.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitService {
    @Autowired
     private RabbitTemplate rabbitTemplate;

    //发送消息的方法
    //exchange:交换机
    //routingKey:路由key
    //message:消息
    public boolean sendMessage(String exchange,String routingKey,Object message){
        rabbitTemplate.convertAndSend(exchange,routingKey,message);
        return true;
    }

    //接收消息的方法

}
