package com.my.ssyx.activity.api;

import com.my.ssyx.activity.service.ActivityInfoService;
import com.my.ssyx.activity.service.CouponInfoService;
import com.my.ssyx.common.result.Result;
import com.my.ssyx.model.activity.CouponInfo;
import com.my.ssyx.model.activity.CouponUse;
import com.my.ssyx.model.order.CartInfo;
import com.my.ssyx.vo.order.CartInfoVo;
import com.my.ssyx.vo.order.OrderConfirmVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "促销与优惠券接口")
@RestController
@RequestMapping("/api/activity")
public class ActivityInfoApiController {
    @Autowired
    ActivityInfoService activityInfoService;

    @Autowired
    CouponInfoService couponInfoService;


    //获取购物车里面满足条件优惠卷和活动的信息
    @ApiOperation("获取购物车里面满足条件优惠卷和活动的信息")
    @PostMapping("inner/findCartActivityAndCoupon/{userId}")
    public OrderConfirmVo findCartActivityAndCoupon(@RequestBody List<CartInfo> cartInfoList
                                                    ,@PathVariable Long userId){
        return activityInfoService.findCartActivityAndCoupon(cartInfoList,userId);
    }


    @ApiOperation(value = "根据skuId列表获取促销信息")
    @PostMapping("inner/findActivity")
    public Map<Long, List<String>> findActivity(@RequestBody List<Long> skuIdList){
       return activityInfoService.findActivity(skuIdList);
    }

    @ApiOperation("根据skuId获取营销数据和优惠卷数据")
    @GetMapping("inner/findActivityAndCoupon/{skuId}/{userId}")
    public Map<String,Object> findActivityAndCoupon(@PathVariable Long skuId,
                                                    @PathVariable Long userId){
        return activityInfoService.findActivityAndCoupon(skuId,userId);
    }

    @ApiOperation("获取购物车对应规则数据")
    @PostMapping("inner/findCartActivityList")
    public List<CartInfoVo> findCartActivityList(@RequestBody List<CartInfo> cartInfoList){
        return activityInfoService.findCartActivityList(cartInfoList);
    }

    @ApiOperation("获取购物车对应优惠卷")
    @PostMapping("inner/findRangeSkuIdList/{couponId}")
    public CouponInfo findRangeSkuIdList(@RequestBody List<CartInfo> cartInfoList,
                                         @PathVariable Long couponId) {
            return couponInfoService.findRangeSkuIdList(cartInfoList,couponId);
    }

    @ApiOperation("更新优惠卷使用状态")
    @GetMapping(value = "inner/updateCouponInfoUseStatus/{couponId}/{userId}/{orderId}")
    public Boolean updateCouponInfoUseStatus(@PathVariable("couponId") Long couponId, @PathVariable("userId") Long userId,
                                             @PathVariable("orderId") Long orderId) {
        couponInfoService.updateCouponInfoUseStatus(couponId, userId, orderId);
        return true;
    }

    @ApiOperation("领取优惠卷")
    @GetMapping("/auth/getCouponInfo/{couponId}")
    public Result getCouponInfo(@PathVariable Long couponId){
        CouponUse couponUse = couponInfoService.getCouponInfoAndSet(couponId);
        return Result.ok(couponUse) ;
    }

}
