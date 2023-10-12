//package com.my.ssyx.common.Redisson;
//
//import com.my.ssyx.common.constant.RedisConst;
//import com.my.ssyx.vo.product.SkuStockLockVo;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.aspectj.lang.reflect.MethodSignature;
//import org.redisson.api.RLock;
//import org.redisson.api.RedissonClient;
//import org.springframework.context.annotation.Configuration;
//
//import javax.annotation.Resource;
//import java.util.concurrent.TimeUnit;
//
//@Configuration
//@Aspect
//public class RedisLockAspect {
//    @Resource
//    private RedissonClient redissonClient;
//
//    @Around("@annotation(RedisLock)")
//    public  Object pointcut(ProceedingJoinPoint joinPoint) throws Throwable {
//       RedisLock redisLock=  ((MethodSignature)joinPoint.getSignature()).getMethod().getAnnotation(RedisLock.class);
//        TimeUnit timeUnit = redisLock.timeUnit();
//        String lockName = redisLock.LockName();
//        int expiresTime = redisLock.expiresTime();
//        Object[] args = joinPoint.getArgs();
//        RLock rLock=null;
//        if (joinPoint.getSignature().getName().equals("checkLock")){
//            SkuStockLockVo skuStockLockVo =(SkuStockLockVo) args[0];
//            rLock= redissonClient.getFairLock(RedisConst.SKUKEY_PREFIX + skuStockLockVo.getSkuId());
//        }else {
//            rLock=redissonClient.getFairLock(lockName);
//        }
//        rLock.lock(expiresTime,timeUnit);
//        try{
//            return  joinPoint.proceed();
//        }finally {
//            rLock.unlock();
//        }
//    }
//}
