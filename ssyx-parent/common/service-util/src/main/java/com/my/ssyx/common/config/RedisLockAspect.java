package com.my.ssyx.common.config;

import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Aspect
public class RedisLockAspect {
    private  static  final String REDISSON_LOCK_PREDIX="redisson_lock:";

    @Resource
    private RedissonClient redissonClient;

}
