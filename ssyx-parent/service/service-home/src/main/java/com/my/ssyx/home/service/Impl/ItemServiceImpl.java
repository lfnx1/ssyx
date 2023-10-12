package com.my.ssyx.home.service.Impl;

import com.my.ssyx.activity.client.ActivityFeignClient;
import com.my.ssyx.client.product.ProductFeignClient;
import com.my.ssyx.client.search.SkuFeignClient;
import com.my.ssyx.home.service.ItemService;
import com.my.ssyx.vo.product.SkuInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class ItemServiceImpl implements ItemService {

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private ActivityFeignClient activityFeignClient;

    @Autowired
    private SkuFeignClient skuFeignClient;
    //商品详情
    @Override
    public Map<String, Object> item(Long id, Long userId) {
        Map<String, Object> result =new HashMap<>();

        //skuId查询信息
        CompletableFuture<SkuInfoVo> skuInfoVocompletableFuture = CompletableFuture.supplyAsync(() -> {
            //远程调用获取sku对应数据
            SkuInfoVo skuInfoVo = productFeignClient.getSkuInfoVo(id);
            result.put("skuInfoVo", skuInfoVo);
            return skuInfoVo;
        },threadPoolExecutor);

        //sku对应优惠卷信息
        CompletableFuture<Void> activityCompletableFuture = CompletableFuture.runAsync(() -> {
            //远程调用获取优惠卷
            Map<String, Object> activityMap = activityFeignClient.findActivityAndCoupon(id, userId);
            result.putAll(activityMap);
        },threadPoolExecutor);

        //更新商品热度
        CompletableFuture<Void> hotCompletableFuture = CompletableFuture.runAsync(() -> {
            //远程调用更新热度
            Boolean aBoolean = skuFeignClient.incrHotScore(id);
        },threadPoolExecutor);

        //任务组合
        CompletableFuture.allOf(
                skuInfoVocompletableFuture,
                activityCompletableFuture,
                hotCompletableFuture
        ).join();
        return result;
    }
}
