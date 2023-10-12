package com.my.ssyx.activity.controller;


import com.my.ssyx.activity.service.CouponInfoService;
import com.my.ssyx.common.result.Result;
import com.my.ssyx.model.activity.CouponInfo;
import com.my.ssyx.vo.activity.CouponRuleVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * <p>
 * 优惠券信息 前端控制器
 * </p>
 *
 * @author lfnx
 * @since 2023-06-21
 */
@RestController
@RequestMapping("/admin/activity/couponInfo")
public class CouponInfoController {
    @Autowired
    private CouponInfoService couponInfoService;

    //1.优惠卷分页查询
    @GetMapping("{page}/{limit}")
    public Result list(@PathVariable Long page,
                       @PathVariable Long limit){
        IPage<CouponInfo> pageModel = couponInfoService.selectPageCouponInfo(page,limit);
        return Result.ok(pageModel);
    }
    //2。添加优惠卷

    @PostMapping("save")
    public Result save(@RequestBody CouponInfo couponInfo){
        boolean save = couponInfoService.save(couponInfo);
        return Result.ok(null);
    }
    //3.根据id查询优惠卷

    @GetMapping("get/{id}")
    public Result get(@PathVariable Long id){
        CouponInfo couponInfo=couponInfoService.getCouponInfo(id);
        return Result.ok(couponInfo);
    }
    //根据优惠卷id查询规则数据
    @GetMapping("findCouponRuleList/{id}")
    public Result findCouponRuleList(@PathVariable Long id){
       Map<String ,Object>  map= couponInfoService.findCouponRuleList(id);
       return Result.ok(map);
    }

    //添加优惠卷规则数据
    @PostMapping("saveCouponRule")
    public Result saveCouponRule(@RequestBody CouponRuleVo couponRuleVo){
        couponInfoService.saveCouponRule(couponRuleVo);
        return Result.ok(null);
    }
}

