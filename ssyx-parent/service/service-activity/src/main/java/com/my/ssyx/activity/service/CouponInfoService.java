package com.my.ssyx.activity.service;

import com.my.ssyx.model.activity.CouponInfo;
import com.my.ssyx.model.activity.CouponUse;
import com.my.ssyx.model.order.CartInfo;
import com.my.ssyx.vo.activity.CouponRuleVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 优惠券信息 服务类
 * </p>
 *
 * @author lfnx
 * @since 2023-06-21
 */
public interface CouponInfoService extends IService<CouponInfo> {

    IPage<CouponInfo> selectPageCouponInfo(Long page, Long limit);

    CouponInfo getCouponInfo(Long id);

    Map<String, Object> findCouponRuleList(Long id);

    void saveCouponRule(CouponRuleVo couponRuleVo);

    List<CouponInfo> findCouponInfoList(Long skuId, Long userId);

    List<CouponInfo> findCartCouponInfo(List<CartInfo> cartInfoList, Long userId);

    CouponInfo findRangeSkuIdList(List<CartInfo> cartInfoList, Long couponId);

    void updateCouponInfoUseStatus(Long couponId, Long userId, Long orderId);

    CouponUse getCouponInfoAndSet(Long couponId);
}
