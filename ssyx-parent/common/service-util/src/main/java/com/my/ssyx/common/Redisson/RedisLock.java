package com.my.ssyx.common.Redisson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisLock {
    //锁名
    String LockName()  default "MyKey";
    //过期时间
    int expiresTime() default 6000;
    //时间单位
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

}
