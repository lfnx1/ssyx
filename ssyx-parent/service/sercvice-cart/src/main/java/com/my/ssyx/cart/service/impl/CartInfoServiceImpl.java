package com.my.ssyx.cart.service.impl;

import com.my.ssyx.cart.service.CartInfoService;
import com.my.ssyx.client.product.ProductFeignClient;
import com.my.ssyx.common.constant.RedisConst;
import com.my.ssyx.common.exception.SsyxException;
import com.my.ssyx.common.result.ResultCodeEnum;
import com.my.ssyx.enums.SkuType;
import com.my.ssyx.model.order.CartInfo;
import com.my.ssyx.model.product.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartInfoServiceImpl implements CartInfoService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ProductFeignClient productFeignClient;

    //获取key
    private String getCartKey(Long userId){
        return RedisConst.USER_KEY_PREFIX+userId+RedisConst.USER_CART_KEY_SUFFIX;
    }

    //购物车添加商品
    @Override
    public void addToCart(Long skuId, Long userId, Integer skuNum) {
            //1.从redis 里面根据key 获取数据  这个key包含userId
        String cartKey = this.getCartKey(userId);
        BoundHashOperations<String,String, CartInfo> HashOperations = redisTemplate.boundHashOps(cartKey);
        //2.根据第一步查询结果   得到的是skuId+skuNum 关系
            //为了判断是否为第一次添加这个商品到购物车
        //进行判断 判断结果里面  是否有skuId

        CartInfo cartInfo=null;
        if (HashOperations.hasKey(skuId.toString())){
            //3.如果有skuId就不是第一次添加
            //3.1根据skuId 获取对应数量 更新数量
           cartInfo = HashOperations.get(skuId.toString());
           Integer currentSkuNum= cartInfo.getSkuNum()+skuNum;
           if (currentSkuNum<1){
               return;
           }
           //更新cartInfo对象
            cartInfo.setSkuNum(currentSkuNum);
            cartInfo.setCurrentBuyNum(currentSkuNum);
            //判断商品数量不能大于限购数量
            Integer perLimit = cartInfo.getPerLimit();
            if (currentSkuNum>perLimit){
                throw  new SsyxException(ResultCodeEnum.SKU_LIMIT_ERROR);
            }
            //更新其他值
            cartInfo.setIsChecked(1);
            cartInfo.setUpdateTime(new Date());
        }
        else {
            //4.否则为第一次添加
            //4.1 直接进行添加
            skuNum=1;

            //封装cartInfo对象
            cartInfo =new CartInfo();
            //远程调用获取skuInfo
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            if (skuInfo==null){
                throw  new SsyxException(ResultCodeEnum.DATA_ERROR);
            }
            cartInfo.setSkuId(skuId);
            cartInfo.setCategoryId(skuInfo.getCategoryId());
            cartInfo.setSkuType(skuInfo.getSkuType());
            cartInfo.setIsNewPerson(skuInfo.getIsNewPerson());
            cartInfo.setUserId(userId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setCurrentBuyNum(skuNum);
            cartInfo.setSkuType(SkuType.COMMON.getCode());
            cartInfo.setPerLimit(skuInfo.getPerLimit());
            cartInfo.setImgUrl(skuInfo.getImgUrl());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setWareId(skuInfo.getWareId());
            cartInfo.setIsChecked(1);
            cartInfo.setStatus(1);
            cartInfo.setCreateTime(new Date());
            cartInfo.setUpdateTime(new Date());
        }


        //5.更新Redis缓存
        HashOperations.put(skuId.toString(),cartInfo);

        //6.设置过期时间
        this.setCartKeyExpire(cartKey);


    }

    @Override
    public void deleteCart(Long skuId, Long userId) {
        BoundHashOperations<String,String,CartInfo> hashOperations = redisTemplate.boundHashOps(this.getCartKey(userId));
        if (hashOperations.hasKey(skuId.toString())){
            hashOperations.delete(skuId.toString());
        }
    }

    @Override
    public void deleteAllCart(Long userId) {
        BoundHashOperations<String,String,CartInfo> hashOperations = redisTemplate.boundHashOps(this.getCartKey(userId));
        List<CartInfo> values = hashOperations.values();
        for (CartInfo value : values) {
            hashOperations.delete(value.getSkuId().toString());
        }
    }

    @Override
    public void batchDeleteCart(List<Long> skuIdList,Long userId) {
        BoundHashOperations<String,String,CartInfo> hashOperations = redisTemplate.boundHashOps(this.getCartKey(userId));
        skuIdList.forEach(skuId->{
            hashOperations.delete(skuId.toString());
        });
    }

    @Override
    public List<CartInfo> getcartList(Long userId) {
        //判断是否为空
        List<CartInfo> cartInfoList =new ArrayList<>();
        if (StringUtils.isEmpty(cartInfoList)){
            return cartInfoList;
        }
        //从Redis中获取数据
        BoundHashOperations<String,String,CartInfo> hashOperations = redisTemplate.boundHashOps(this.getCartKey(userId));
        cartInfoList = hashOperations.values();
        if (!StringUtils.isEmpty(cartInfoList)){
           //降序排列
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getCreateTime().compareTo(o2.getCreateTime());
                }
            });
        }
        return cartInfoList;
    }

    @Override
    public void checckCart(Long userId, Long skuId, Integer isChecked) {
        //获取redis里面的key
        String cartKey = this.getCartKey(userId);
        BoundHashOperations<String,String,CartInfo> hashOperations = redisTemplate.boundHashOps(cartKey);
        //根据skuId获取value值
        CartInfo cartInfo = hashOperations.get(skuId.toString());
        if (cartInfo!=null){
            cartInfo.setIsChecked(isChecked);
            //更新
            hashOperations.put(skuId.toString(),cartInfo);
            //设置过期时间
            this.setCartKeyExpire(cartKey);
        }
    }

    @Override
    public void checkAllCart(Long userId, Integer isChecked) {
        String cartKey = this.getCartKey(userId);
        BoundHashOperations<String,String,CartInfo> boundHashOperations = redisTemplate.boundHashOps(cartKey);
        List<CartInfo> cartInfoList = boundHashOperations.values();
        cartInfoList.stream().forEach(item->{
            item.setIsChecked(isChecked);
            boundHashOperations.put(item.getSkuId().toString(),item);
        });
        this.setCartKeyExpire(cartKey);
    }

    @Override
    public void batchCheckCart(Long userId, List<Long> skuIdList, Integer isChecked) {
        String cartKey = this.getCartKey(userId);
        BoundHashOperations<String,String,CartInfo> boundHashOperations = redisTemplate.boundHashOps(cartKey);
        skuIdList.forEach(skuId->{
            CartInfo cartInfo = boundHashOperations.get(skuId.toString());
            cartInfo.setIsChecked(isChecked);
            boundHashOperations.put(cartInfo.getSkuId().toString(),cartInfo);
        });
        this.setCartKeyExpire(cartKey);
    }

    @Override
    public List<CartInfo> getCartCheckList(Long userId) {
        String cartKey = this.getCartKey(userId);
        BoundHashOperations<String,String,CartInfo> boundHashOperations = redisTemplate.boundHashOps(cartKey);
        List<CartInfo> cartInfoList = boundHashOperations.values();
        List<CartInfo> cartInfoListNew = cartInfoList.stream().filter(cartInfo -> {
            return cartInfo.getIsChecked().intValue() == 1;
        }).collect(Collectors.toList());
        return cartInfoListNew;
    }

    @Override
    public void deleteCartChecked(Long userId) {
        //根据userid查询选中购物记录
        List<CartInfo> cartCheckList = this.getCartCheckList(userId);
        //查询list数据处理，得到skuId集合
        List<Long> skuIdList = cartCheckList.stream().map(item ->
            item.getSkuId()
        ).collect(Collectors.toList());
        //构建redis的key值
        String cartKey = this.getCartKey(userId);
        //根据key查询filed-value结构
        BoundHashOperations<String,String,Object> hashOperations = redisTemplate.boundHashOps(cartKey);
        //根据filed删除redis数据
        skuIdList.forEach(skuId->{
            hashOperations.delete(skuId.toString());
        });
    }

    //设置key 过期时间
    private void setCartKeyExpire(String key){
        redisTemplate.expire(key,RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }
}
