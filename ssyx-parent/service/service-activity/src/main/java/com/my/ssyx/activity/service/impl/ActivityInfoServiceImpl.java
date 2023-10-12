package com.my.ssyx.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.my.ssyx.activity.mapper.ActivityInfoMapper;
import com.my.ssyx.activity.mapper.ActivityRuleMapper;
import com.my.ssyx.activity.mapper.ActivitySkuMapper;
import com.my.ssyx.activity.service.ActivityInfoService;
import com.my.ssyx.activity.service.CouponInfoService;
import com.my.ssyx.client.product.ProductFeignClient;
import com.my.ssyx.enums.ActivityType;
import com.my.ssyx.model.activity.ActivityInfo;
import com.my.ssyx.model.activity.ActivityRule;
import com.my.ssyx.model.activity.ActivitySku;
import com.my.ssyx.model.activity.CouponInfo;
import com.my.ssyx.model.order.CartInfo;
import com.my.ssyx.model.product.SkuInfo;
import com.my.ssyx.vo.activity.ActivityRuleVo;
import com.my.ssyx.vo.order.CartInfoVo;
import com.my.ssyx.vo.order.OrderConfirmVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 活动表 服务实现类
 * </p>
 *
 * @author lfnx
 * @since 2023-06-21
 */
@Service
public class ActivityInfoServiceImpl extends ServiceImpl<ActivityInfoMapper, ActivityInfo> implements ActivityInfoService {

    @Autowired
    private ActivityRuleMapper activityRuleMapper;

    @Autowired
    private ActivitySkuMapper activitySkuMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private CouponInfoService couponInfoService;


    @Override
    public Map<String, Object> findActivityAndCoupon(Long skuId, Long userId) {
        //1.根据skuId获取sku营销活动， 一个活动有多个规则
        List<ActivityRule> activityRuleBySkuId = this.findActivityRuleBySkuId(skuId);
        System.out.println("___________________" + activityRuleBySkuId.toString());
        //2.根据skuId+userId查询优惠卷信息
        List<CouponInfo> couponInfoList = couponInfoService.findCouponInfoList(skuId, userId);
        //3.封装到map集合,返回
        Map<String, Object> result = new HashMap<>();
        result.put("couponInfoList", couponInfoList);
        result.put("activityRuleList", activityRuleBySkuId);
        return result;
    }

    @Override
    public List<ActivityRule> findActivityRuleBySkuId(Long skuId) {
        List<ActivityRule> activityRuleList = baseMapper.findActivityRule(skuId);
        for (ActivityRule activityRule : activityRuleList) {
            String ruleDesc = this.getRuleDesc(activityRule);
            activityRule.setRuleDesc(ruleDesc);
        }
        return activityRuleList;
    }

    @Override
    public OrderConfirmVo findCartActivityAndCoupon(List<CartInfo> cartInfoList, Long userId) {
        //1.获取购物车每个购物项参与活动，根据活动规则分组
        //一个活动规则对应多个商品
        //CartInfoVo
        List<CartInfoVo> cartInfoVoList = this.findCartActivityList(cartInfoList);
        //2.计算参与活动之后金额
       BigDecimal  activityReduceAmount= cartInfoVoList.stream().
                filter(cartInfoVo -> cartInfoVo.getActivityRule() != null).
                map(cartInfoVo -> cartInfoVo.getActivityRule().getReduceAmount())
               .reduce(BigDecimal.ZERO,BigDecimal::add);
        //3.获取购物车可以使用优惠卷列表
        List<CouponInfo> couponInfoList =
                couponInfoService.findCartCouponInfo(cartInfoList,userId);
        //4.计算商品使用优惠卷之后金额，一次只能使用一张
        BigDecimal couponReduceAmount =new BigDecimal(0);
        if (!CollectionUtils.isEmpty(couponInfoList)){
            couponReduceAmount = couponInfoList.stream()
                    .filter(couponInfo -> couponInfo.getIsOptimal().intValue() == 1)
                    .map(couponInfo -> couponInfo.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        //5.计算没有参与活动，没有使用优惠卷原始金额
        BigDecimal originalTotalAmount = cartInfoList.stream()
                .filter(cartInfo -> cartInfo.getIsChecked() == 1)
                .map(cartInfo -> cartInfo.getCartPrice().multiply(new BigDecimal((cartInfo.getSkuNum()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        //6.参加活动，使用优惠卷总金额
        BigDecimal totalAmount = originalTotalAmount.subtract(activityReduceAmount).subtract(couponReduceAmount);

        //7.封装需要数据到OrderConfirmVo
        OrderConfirmVo orderTradeVo =new OrderConfirmVo();
        orderTradeVo.setCarInfoVoList(cartInfoVoList);
        orderTradeVo.setActivityReduceAmount(activityReduceAmount);
        orderTradeVo.setCouponInfoList(couponInfoList);
        orderTradeVo.setCouponReduceAmount(couponReduceAmount);
        orderTradeVo.setOriginalTotalAmount(originalTotalAmount);
        orderTradeVo.setTotalAmount(totalAmount);
        return orderTradeVo;
    }

    //获取购物车对应规则数据
    @Override
    public List<CartInfoVo> findCartActivityList(List<CartInfo> cartInfoList) {
        //创建最终返回集合
        List<CartInfoVo> cartInfoVoList = new ArrayList<>();
        //获取所有的skuId
        List<Long> skuIdList = cartInfoList.stream().map(CartInfo::getSkuId).collect(Collectors.toList());
        //根据所有的skuId获取参与活动
        List<ActivitySku> activitySkuList = baseMapper.selectCartActivity(skuIdList);
        //根据活动进行分组,每个活动里面有那些skuId
        //activityIdToSkuIdListMap 中 key 为分组字段 活动id
        //value是每组里面的列表数据 set集合
        Map<Long, Set<Long>> activityIdToSkuIdListMap = activitySkuList.stream().
                collect(Collectors.groupingBy(ActivitySku::getActivityId,
                        Collectors.mapping(ActivitySku::getSkuId, Collectors.toSet())
                ));
        //获取活动里面规则数据
        //key 为活动id value是活动里面的规则列表数据
        Map<Long, List<ActivityRule>> activityIdToActivityRuleListMap = new HashMap<>();
        //所有活动id
        Set<Long> activityIdset = activitySkuList.stream().map(ActivitySku::getActivityId).collect(Collectors.toSet());
        if (!CollectionUtils.isEmpty(activityIdset)) {
            //查询activity_rule表
            LambdaQueryWrapper<ActivityRule> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(ActivityRule::getConditionAmount, ActivityRule::getConditionNum);
            wrapper.in(ActivityRule::getActivityId, activityIdset);
            List<ActivityRule> activityRuleList = activityRuleMapper.selectList(wrapper);
            //封装到activityIdToActivityRuleListMap
            //根据活动id进行分组
            activityIdToActivityRuleListMap =
                    activityRuleList.stream().collect(Collectors.groupingBy(activityRule -> activityRule.getActivityId()));
        }
        //有活动的购物项的skuId
        Set<Long> activitySkuIdSet = new HashSet<>();
        if (!CollectionUtils.isEmpty(activityIdToSkuIdListMap)) {
            //遍历activityIdToSkuIdListMap集合
            Iterator<Map.Entry<Long, Set<Long>>> iterator = activityIdToSkuIdListMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, Set<Long>> entry = iterator.next();
                //活动id
                Long activityId = entry.getKey();
                //每个活动对应skuId列表
                Set<Long> currentActivitySkuIdSet = entry.getValue();
                //获取当前活动对应的购物项列表
                List<CartInfo> currentActivityCartInfoList = cartInfoList.stream().
                        filter(cartInfo -> currentActivitySkuIdSet.contains(cartInfo.getSkuId()))
                        .collect(Collectors.toList());
                //计算购物项总金额和总数量
                BigDecimal activityTotalAmount = this.computeTotalAmount(currentActivityCartInfoList);
                int activityTotalNum = this.computeCartNum(currentActivityCartInfoList);

                //计算活动对应规则
                //根据活动获取对应的规则
                List<ActivityRule> currentactivityRuleList =
                        activityIdToActivityRuleListMap.get(activityId);
                ActivityType activityType = currentactivityRuleList.get(0).getActivityType();
                //判断活动类型:满减还是打折
                ActivityRule activityRule = null;
                if (activityType == ActivityType.FULL_REDUCTION) {
                    activityRule = this.computeFullReduction(activityTotalAmount, currentactivityRuleList);
                } else {//满量
                    activityRule = this.computeFullDiscount(activityTotalNum, activityTotalAmount, currentactivityRuleList);
                }
                //CartInfoVo封装
                CartInfoVo cartInfoVo =new CartInfoVo();
                cartInfoVo.setActivityRule(activityRule);
                cartInfoVo.setCartInfoList(currentActivityCartInfoList);
                cartInfoVoList.add(cartInfoVo);
                //记录那些购物项参加活动
                activitySkuIdSet.addAll(currentActivitySkuIdSet);
            }
        }

        //没有活动的购物项的skuId
        //获取哪些skuId没有参加活动
        skuIdList.removeAll(activitySkuIdSet);
        if (!CollectionUtils.isEmpty(skuIdList)){
            //获取skuId对应的购物项
            Map<Long, CartInfo> skuIdCartInfoMap =
                    cartInfoList.stream().collect(Collectors.toMap(CartInfo::getSkuId, CartInfo -> CartInfo));
            for (Long skuId : skuIdList) {
                CartInfoVo cartInfoVo =new CartInfoVo();
                cartInfoVo.setActivityRule(null);
                CartInfo cartInfo = skuIdCartInfoMap.get(skuId);
                List<CartInfo> cartInfos =new ArrayList<>();
                cartInfos.add(skuIdCartInfoMap.get(skuId));
                cartInfoVo.setCartInfoList(cartInfos);
                cartInfoVoList.add(cartInfoVo);
            }
        }

        return cartInfoVoList;
    }

    private ActivityRule computeFullDiscount(Integer totalNum, BigDecimal totalAmount, List<ActivityRule> activityRuleList) {
        ActivityRule optimalActivityRule = null;
        //该活动规则skuActivityRuleList数据，已经按照优惠金额从大到小排序了
        for (ActivityRule activityRule : activityRuleList) {
            //如果订单项购买个数大于等于满减件数，则优化打折
            if (totalNum.intValue() >= activityRule.getConditionNum()) {
                BigDecimal skuDiscountTotalAmount = totalAmount.multiply(activityRule.getBenefitDiscount().divide(new BigDecimal("10")));
                BigDecimal reduceAmount = totalAmount.subtract(skuDiscountTotalAmount);
                activityRule.setReduceAmount(reduceAmount);
                optimalActivityRule = activityRule;
                break;
            }
        }
        if (null == optimalActivityRule) {
            //如果没有满足条件的取最小满足条件的一项
            optimalActivityRule = activityRuleList.get(activityRuleList.size() - 1);
            optimalActivityRule.setReduceAmount(new BigDecimal("0"));
            optimalActivityRule.setSelectType(1);

            StringBuffer ruleDesc = new StringBuffer()
                    .append("满")
                    .append(optimalActivityRule.getConditionNum())
                    .append("元打")
                    .append(optimalActivityRule.getBenefitDiscount())
                    .append("折，还差")
                    .append(totalNum - optimalActivityRule.getConditionNum())
                    .append("件");
            optimalActivityRule.setRuleDesc(ruleDesc.toString());
        } else {
            StringBuffer ruleDesc = new StringBuffer()
                    .append("满")
                    .append(optimalActivityRule.getConditionNum())
                    .append("元打")
                    .append(optimalActivityRule.getBenefitDiscount())
                    .append("折，已减")
                    .append(optimalActivityRule.getReduceAmount())
                    .append("元");
            optimalActivityRule.setRuleDesc(ruleDesc.toString());
            optimalActivityRule.setSelectType(2);
        }
        return optimalActivityRule;
    }

    /**
     * 计算满减最优规则
     *
     * @param totalAmount
     * @param activityRuleList //该活动规则skuActivityRuleList数据，已经按照优惠金额从大到小排序了
     */
    private ActivityRule computeFullReduction(BigDecimal totalAmount, List<ActivityRule> activityRuleList) {
        ActivityRule optimalActivityRule = null;
        //该活动规则skuActivityRuleList数据，已经按照优惠金额从大到小排序了
        for (ActivityRule activityRule : activityRuleList) {
            //如果订单项金额大于等于满减金额，则优惠金额
            if (totalAmount.compareTo(activityRule.getConditionAmount()) > -1) {
                //优惠后减少金额
                activityRule.setReduceAmount(activityRule.getBenefitAmount());
                optimalActivityRule = activityRule;
                break;
            }
        }
        if (null == optimalActivityRule) {
            //如果没有满足条件的取最小满足条件的一项
            optimalActivityRule = activityRuleList.get(activityRuleList.size() - 1);
            optimalActivityRule.setReduceAmount(new BigDecimal("0"));
            optimalActivityRule.setSelectType(1);

            StringBuffer ruleDesc = new StringBuffer()
                    .append("满")
                    .append(optimalActivityRule.getConditionAmount())
                    .append("元减")
                    .append(optimalActivityRule.getBenefitAmount())
                    .append("元，还差")
                    .append(totalAmount.subtract(optimalActivityRule.getConditionAmount()))
                    .append("元");
            optimalActivityRule.setRuleDesc(ruleDesc.toString());
        } else {
            StringBuffer ruleDesc = new StringBuffer()
                    .append("满")
                    .append(optimalActivityRule.getConditionAmount())
                    .append("元减")
                    .append(optimalActivityRule.getBenefitAmount())
                    .append("元，已减")
                    .append(optimalActivityRule.getReduceAmount())
                    .append("元");
            optimalActivityRule.setRuleDesc(ruleDesc.toString());
            optimalActivityRule.setSelectType(2);
        }
        return optimalActivityRule;
    }

    private BigDecimal computeTotalAmount(List<CartInfo> cartInfoList) {
        BigDecimal total = new BigDecimal("0");
        for (CartInfo cartInfo : cartInfoList) {
            //是否选中
            if (cartInfo.getIsChecked().intValue() == 1) {
                BigDecimal itemTotal = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
                total = total.add(itemTotal);
            }
        }
        return total;
    }

    private int computeCartNum(List<CartInfo> cartInfoList) {
        int total = 0;
        for (CartInfo cartInfo : cartInfoList) {
            //是否选中
            if (cartInfo.getIsChecked().intValue() == 1) {
                total += cartInfo.getSkuNum();
            }
        }
        return total;
    }


    @Override
    public IPage<ActivityInfo> getPage(Page<ActivityInfo> pagePram) {
        IPage<ActivityInfo> activityInfoPage = baseMapper.selectPage(pagePram, null);

        //分页查询对象里面获取列表数据
        List<ActivityInfo> activityInfoList = activityInfoPage.getRecords();
        //遍历activityInfoList集合，得到每个ActivityInfo对象
        //向ActivityInfo对象封装活动类型到activityTypeString属性里
        activityInfoList.stream().forEach(item -> {
            item.setActivityTypeString(item.getActivityType().getComment());
        });
        return activityInfoPage;
    }

    //根据活动id获取活动规则数据
    @Override
    public Map<String, Object> findActivityRuleList(Long id) {
        Map<String, Object> result = new HashMap<>();
        //1.根据活动id，查询规则列表 activity_rule表
        LambdaQueryWrapper<ActivityRule> wrapperRule = new LambdaQueryWrapper();
        wrapperRule.eq(ActivityRule::getActivityId, id);
        List<ActivityRule> activityRules = activityRuleMapper.selectList(wrapperRule);
        result.put("activityRuleList", activityRules);
        //2.根据活动id 查询使用规则商品表 activity_sku表
        LambdaQueryWrapper<ActivitySku> wrapperSku = new LambdaQueryWrapper<>();
        wrapperSku.eq(ActivitySku::getActivityId, id);
        List<ActivitySku> activitySkus = activitySkuMapper.selectList(wrapperSku);
        //获取所有的skuId
        List<Long> SkuIdList = activitySkus.stream().map(ActivitySku::getSkuId).collect(Collectors.toList());
        //2.1远程调用 service-product模块接口 根据skuid列表得到商品信息
        if (!SkuIdList.isEmpty()) {
            List<SkuInfo> skuInfoList = productFeignClient.findSkuInfoList(SkuIdList);
            result.put("skuInfoList", skuInfoList);
        }
        result.put("skuInfoList", null);
        return result;
    }

    @Override
    public void saveActivityRule(ActivityRuleVo activityRuleVo) {
        //第一步 根据活动id删除之前的规则数据
        Long activityId = activityRuleVo.getActivityId();
        //ActivityRule
        activityRuleMapper.delete(new LambdaQueryWrapper<ActivityRule>().
                eq(ActivityRule::getActivityId, activityId));
        //ActivitySku
        activitySkuMapper.delete(new LambdaQueryWrapper<ActivitySku>().
                eq(ActivitySku::getActivityId, activityId));
        //第二步 获取规则列表数据
        List<ActivityRule> activityRuleList = activityRuleVo.getActivityRuleList();
        ActivityInfo activityInfo = baseMapper.selectById(activityId);
        for (ActivityRule activityRule : activityRuleList) {
            activityRule.setActivityId(activityId);//活动id
            activityRule.setActivityType(activityInfo.getActivityType());//类型
            activityRuleMapper.insert(activityRule);
        }
        //第三步 获取规则范围数据
        List<ActivitySku> activitySkuList = activityRuleVo.getActivitySkuList();
        for (ActivitySku activitySku : activitySkuList) {
            activitySku.setActivityId(activityId);
            activitySkuMapper.insert(activitySku);
        }
    }

    //3.根据关键字查询匹配sku信息
    @Override
    public List<SkuInfo> findSkuInfoByKeyword(String keyword) {
        //第一步 根据关键字查询sku匹配内容列表
        //(1) service-product模块创建接口 根据关键字查询sku匹配内容列表
        //(2) service-activity远程调用得到sku内容列表
        List<SkuInfo> skuInfoList = productFeignClient.findSkuInfoByKeyword(keyword);
        if (skuInfoList.isEmpty()) {
            return skuInfoList;
        }
        //从skuInfoList中获取所有id
        List<Long> skuIdList = skuInfoList.stream().map(SkuInfo::getId).collect(Collectors.toList());
        //第二步 判断商品之前是否参加过活动 若参与过  则活动正在进行中  排除商品
        //(1) 查询 两张表  activity_info 和 activity_sku  编写sql语句实现  写在xml文件当中
        //获得正在参加活动的商品
        List<Long> exitSkuIdList = baseMapper.selectExistSkuIdList(skuIdList);
        System.out.println("----------------------" + exitSkuIdList.toString());
        String existSkuIdString = "," + StringUtils.join(exitSkuIdList.toArray(), ",") + ",";
        //(2) 判断逻辑处理:排除已经参加活动商品
        List<SkuInfo> findSkuList = new ArrayList<>();
        //遍历全部sku列表
        for (SkuInfo skuInfo : skuInfoList) {
            if (existSkuIdString.indexOf("," + skuInfo.getId() + ",") == -1) {
                System.out.println(existSkuIdString.indexOf("," + skuInfo.getId() + ","));
                findSkuList.add(skuInfo);
            }
        }
        System.out.println(findSkuList);
        return findSkuList;
    }

    @Override
    public Map<Long, List<String>> findActivity(List<Long> skuIdList) {
        Map<Long, List<String>> result = new HashMap<>();
        //skuIdList遍历  得到每个skuId
        skuIdList.forEach(skuId -> {
            //查询sku对应活动里面规则列表
            List<ActivityRule> activityRuleList = baseMapper.findActivityRule(skuId);
            //数据封装 规则名称
            if (!CollectionUtils.isEmpty(activityRuleList)) {
                List<String> ruleList = new ArrayList<>();
                //把规则名称处理
                for (ActivityRule activityRule : activityRuleList) {
                    activityRule.setRuleDesc(this.getRuleDesc(activityRule));
                    ruleList.add(this.getRuleDesc(activityRule));
                }
//                List<String> ruleList = activityRuleList.stream().map(activityRule ->
//                        activityRule.getRuleDesc()).collect(Collectors.toList());
                result.put(skuId, ruleList);
            }
        });

        return result;
    }


    //构造规则名称方法
    private String getRuleDesc(ActivityRule activityRule) {
        ActivityType activityType = activityRule.getActivityType();
        StringBuffer ruleDesc = new StringBuffer();
        if (activityType == ActivityType.FULL_REDUCTION) {
            ruleDesc
                    .append("满")
                    .append(activityRule.getConditionAmount())
                    .append("元减")
                    .append(activityRule.getBenefitAmount())
                    .append("元");
        } else {
            ruleDesc
                    .append("满")
                    .append(activityRule.getConditionNum())
                    .append("元打")
                    .append(activityRule.getBenefitDiscount())
                    .append("折");
        }
        return ruleDesc.toString();
    }
}
