package com.my.ssyx.home.service.Impl;

import com.my.ssyx.client.product.ProductFeignClient;
import com.my.ssyx.client.search.SkuFeignClient;
import com.my.ssyx.client.user.UserFeignClient;
import com.my.ssyx.home.service.HomeService;
import com.my.ssyx.model.product.Category;
import com.my.ssyx.model.product.SkuInfo;
import com.my.ssyx.model.search.SkuEs;
import com.my.ssyx.vo.user.LeaderAddressVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HomeServiceImpl implements HomeService {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private SkuFeignClient skuFeignClient;

    @Override
    public Map<String, Object> homeData(Long userId) {
        Map<String,Object> result =new HashMap<>();
        //1.根据userId获取当前登录用户提货地址信息
        //远程调用 service-user模块接口获取需要数据
        LeaderAddressVo leaderAddressVo =
                userFeignClient.getUserAddressByUserId(userId);
        result.put("leaderAddressVo",leaderAddressVo);
        //2.获取所有分类
        //远程调用service-product模块接口
        List<Category> allCategoryList = productFeignClient.findAllCategoryList();
        result.put("categoryList",allCategoryList);
        //3 获取新人专享商品
        //远程调用service-product模块接口
        List<SkuInfo> personSkuInfoList = productFeignClient.findNewPersonSkuInfoList();
        result.put("newPersonSkuInfoList",personSkuInfoList);
        //4. 获取爆款商品
        //远程调用service-search模块接口
        // score评分降序排序
        List<SkuEs> hotSkuList = skuFeignClient.findHotSkuList();
        result.put("hotSkuList",hotSkuList);
        //5.封装数据到map集合返回
        return result;
    }
}
