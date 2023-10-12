package com.my.ssyx.cart.service;

import com.my.ssyx.model.order.CartInfo;

import java.util.List;

public interface CartInfoService {
    void addToCart(Long skuId, Long userId, Integer skuNum);

    void deleteCart(Long skuId, Long userId);

    void deleteAllCart(Long userId);

    void batchDeleteCart(List<Long> skuIdList,Long userId);

    List<CartInfo> getcartList(Long userId);

    void checckCart(Long userId, Long skuId, Integer isChecked);

    void checkAllCart(Long userId, Integer isChecked);

    void batchCheckCart(Long userId, List<Long> skuIdList, Integer isChecked);

    List<CartInfo> getCartCheckList(Long userId);

    void deleteCartChecked(Long userId);
}
