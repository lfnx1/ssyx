package com.my.ssyx.activity.service;

import com.my.ssyx.model.activity.ActivityInfo;
import com.my.ssyx.model.activity.ActivityRule;
import com.my.ssyx.model.order.CartInfo;
import com.my.ssyx.model.product.SkuInfo;
import com.my.ssyx.vo.activity.ActivityRuleVo;
import com.my.ssyx.vo.order.CartInfoVo;
import com.my.ssyx.vo.order.OrderConfirmVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 活动表 服务类
 * </p>
 *
 * @author lfnx
 * @since 2023-06-21
 */
public interface ActivityInfoService extends IService<ActivityInfo> {

    IPage<ActivityInfo> getPage(Page<ActivityInfo> pagePram);

    Map<String, Object> findActivityRuleList(Long id);

    void saveActivityRule(ActivityRuleVo activityRuleVo);

    List<SkuInfo> findSkuInfoByKeyword(String keyword);

    Map<Long, List<String>> findActivity(List<Long> skuIdList);

    Map<String, Object> findActivityAndCoupon(Long skuId, Long userId);

    List<ActivityRule> findActivityRuleBySkuId(Long skuId);

    OrderConfirmVo findCartActivityAndCoupon(List<CartInfo> cartInfoList, Long userId);

    //获取购物车对应规则数据
    List<CartInfoVo> findCartActivityList(List<CartInfo> cartInfoList);
}
