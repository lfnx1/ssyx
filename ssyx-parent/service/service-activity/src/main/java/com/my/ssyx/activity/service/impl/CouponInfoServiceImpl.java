package com.my.ssyx.activity.service.impl;

import com.my.ssyx.activity.mapper.CouponInfoMapper;
import com.my.ssyx.activity.mapper.CouponRangeMapper;
import com.my.ssyx.activity.mapper.CouponUseMapper;
import com.my.ssyx.activity.service.CouponInfoService;
import com.my.ssyx.client.product.ProductFeignClient;
import com.my.ssyx.common.auth.AuthContextHolder;
import com.my.ssyx.enums.CouponRangeType;
import com.my.ssyx.enums.CouponStatus;
import com.my.ssyx.model.activity.CouponInfo;
import com.my.ssyx.model.activity.CouponRange;
import com.my.ssyx.model.activity.CouponUse;
import com.my.ssyx.model.order.CartInfo;
import com.my.ssyx.model.product.Category;
import com.my.ssyx.model.product.SkuInfo;
import com.my.ssyx.vo.activity.CouponRuleVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 优惠券信息 服务实现类
 * </p>
 *
 * @author lfnx
 * @since 2023-06-21
 */
@Service
public class CouponInfoServiceImpl extends ServiceImpl<CouponInfoMapper, CouponInfo> implements CouponInfoService {


    @Autowired
    private CouponRangeMapper couponRangeMapper;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private CouponUseMapper couponUseMapper;

    @Override
    public IPage<CouponInfo> selectPageCouponInfo(Long page, Long limit) {
        Page<CouponInfo> pageParam = new Page<>(page, limit);
        IPage<CouponInfo> couponInfoPage = baseMapper.selectPage(pageParam, null);
        List<CouponInfo> records = couponInfoPage.getRecords();
        records.stream().forEach(item -> {
            item.setCouponTypeString(item.getCouponType().getComment());
            CouponRangeType rangeType = item.getRangeType();
            if (rangeType != null) {
                item.setRangeTypeString(rangeType.getComment());
            }
        });
        return couponInfoPage;
    }

    @Override
    public CouponInfo getCouponInfo(Long id) {
        CouponInfo couponInfo = baseMapper.selectById(id);
        couponInfo.setCouponTypeString(couponInfo.getCouponType().getComment());
        if (couponInfo.getRangeType() != null) {
            couponInfo.setRangeTypeString(couponInfo.getRangeType().getComment());
        }
        return couponInfo;
    }

    @Override
    public Map<String, Object> findCouponRuleList(Long id) {
        //第一步  根据优惠卷id查询优惠卷基本信息  coupon 表
        CouponInfo couponInfo = baseMapper.selectById(id);
        couponInfo.setCouponTypeString(couponInfo.getCouponType().getComment());
        if (couponInfo.getRangeType() != null) {
            couponInfo.setRangeTypeString(couponInfo.getRangeType().getComment());
        }

        //第二步  根据优惠卷id查询 coupon_range 查询 range_id
        //如果规则类型 SKU  range_id就是skuId值
        //如果规则类型 CATEGORY  range_id就是分类Id值
        List<CouponRange> couponRangeList = couponRangeMapper.selectList(
                new LambdaQueryWrapper<CouponRange>().eq(CouponRange::getCouponId, id));

        List<Long> couponRangeIdList = couponRangeList.stream().map
                (CouponRange::getRangeId).collect(Collectors.toList());
        //第三步  分别判断封装不同数据
        Map<String, Object> result = new HashMap<>();
        if (!couponRangeIdList.isEmpty()) {
            if (couponInfo.getRangeType() == CouponRangeType.SKU) {
                // 如果规则类型是 sku  得到 skuId   远程调用多个skuid值获取对应的sku信息
                System.out.println("ID集合" + couponRangeIdList);
                List<SkuInfo> skuInfoList = productFeignClient.findSkuInfoList(couponRangeIdList);
                result.put("skuInfoList", skuInfoList);
            } else if (couponInfo.getRangeType() == CouponRangeType.CATEGORY) {
                // 如果规则类型是 CATEGORY  得到 分类Id   远程调用多个分类Id值获取对应的分类信息
                List<Category> categoryList = productFeignClient.findCategoryList(couponRangeIdList);
                result.put("categoryList", categoryList);
            }
        }
        System.out.println(result.toString());
        return result;
    }

    @Override
    public void saveCouponRule(CouponRuleVo couponRuleVo) {
        //根据优惠卷ID删除规则数据
        couponRangeMapper.delete(
                new LambdaQueryWrapper<CouponRange>().eq(CouponRange::getCouponId, couponRuleVo.getCouponId()));
        //更新优惠卷基本信息
        CouponInfo couponInfo = baseMapper.selectById(couponRuleVo.getCouponId());
        BeanUtils.copyProperties(couponRuleVo, couponInfo);
        baseMapper.updateById(couponInfo);
        //添加优惠卷新数据
        List<CouponRange> couponRangeList = couponRuleVo.getCouponRangeList();
        for (CouponRange couponRange : couponRangeList) {
            //设置优惠卷id
            couponRange.setCouponId(couponRuleVo.getCouponId());
            //添加
            couponRangeMapper.insert(couponRange);
        }
    }

    @Override
    public List<CouponInfo> findCouponInfoList(Long skuId, Long userId) {
        //根据skuId获取skuInfo信息
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        //根据条件进行查询 skuId +分类Id +userId
        List<CouponInfo> couponInfoList = baseMapper.selectCouponInfoList(skuId,
                skuInfo.getCategoryId(), userId);
        return couponInfoList;
    }

    @Override
    public List<CouponInfo> findCartCouponInfo(List<CartInfo> cartInfoList, Long userId) {
        //1. 根据userId获取用户全部优惠卷
        //coupon_use coupon_info
        List<CouponInfo> userAllCouponInfoList = baseMapper.selectCartCouponInfoList(userId);
        if (CollectionUtils.isEmpty(userAllCouponInfoList)) {
            return new ArrayList<CouponInfo>();
        }
        //2.从第一步返回的list集合中，获取所有优惠卷id列表
        List<Long> couponIdList = userAllCouponInfoList.stream().map
                (couponInfo -> couponInfo.getId()).collect(Collectors.toList());
        //3.查询优惠卷使用范围 coupon_range
        //coupon_rangeList
        LambdaQueryWrapper<CouponRange> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CouponRange::getCouponId, couponIdList);
        List<CouponRange> couponRangeList = couponRangeMapper.selectList(wrapper);
        //4.获取优惠卷id 对应skuId列表
        //优惠卷id进行分组 得到map集合
        //Map<Long,list<Long>>
        Map<Long, List<Long>> couponIdToSkuIdMap = this.findCouponIdToSkuIdMap(cartInfoList, couponRangeList);
        //5.遍历全部优惠卷集合，判断类型
        //全场通用 sku和分类
        BigDecimal reduceAmount = new BigDecimal(0);
        CouponInfo optimalCouponInfo = null;
        for (CouponInfo couponInfo : userAllCouponInfoList) {
            //全场通用
            if (CouponRangeType.ALL == couponInfo.getRangeType()) {
                BigDecimal totalAmount = computeTotalAmount(cartInfoList);
                if (totalAmount.subtract(couponInfo.getConditionAmount()).doubleValue() >= 0) {
                    couponInfo.setIsSelect(1);
                }
            } else {
                //优惠卷id获取对应skuId列表
                List<Long> skuIdList = couponIdToSkuIdMap.get(couponInfo.getId());
                //满足使用范围购物项
                List<CartInfo> currentCartInfoList =
                        cartInfoList.stream().filter(cartInfo ->
                                skuIdList.contains(cartInfo.getSkuId())).collect(Collectors.toList());
                BigDecimal totalAmount = computeTotalAmount(currentCartInfoList);
                if (totalAmount.subtract(couponInfo.getConditionAmount()).doubleValue() >= 0) {
                    couponInfo.setIsSelect(1);
                }
            }
            if (couponInfo.getIsSelect().intValue() == 1 && couponInfo.getAmount().subtract(reduceAmount).doubleValue() > 0) {
                reduceAmount = couponInfo.getAmount();
                optimalCouponInfo = couponInfo;
            }
        }
        if (null != optimalCouponInfo) {
            optimalCouponInfo.setIsOptimal(1);
        }
        //6.返回list<CouponInfo>

        return userAllCouponInfoList;
    }

    //获取购物车对应优惠卷
    @Override
    public CouponInfo findRangeSkuIdList(List<CartInfo> cartInfoList, Long couponId) {
        //根据优惠卷id获取基本信息
        CouponInfo couponInfo = baseMapper.selectById(couponId);
        if (couponInfo == null) {
            return null;
        }
        //根据couponId查询对应coupon_range对应数据
        List<CouponRange> couponRangeList = couponRangeMapper.selectList(new
                LambdaQueryWrapper<CouponRange>().eq(CouponRange::getCouponId, couponId));
        //查询对应的sku信息
        Map<Long, List<Long>> couponIdToSkuIdMap = this.findCouponIdToSkuIdMap(cartInfoList, couponRangeList);
        //遍历map 得到 value值  封装到couponInfo对象
        List<Long> skuIdList = couponIdToSkuIdMap.entrySet().iterator().next().getValue();
        couponInfo.setSkuIdList(skuIdList);
        return couponInfo;
    }

    @Override
    public void updateCouponInfoUseStatus(Long couponId, Long userId, Long orderId) {
        //根据couponId查询优惠卷信息
        CouponUse couponUse = couponUseMapper.selectOne(new LambdaQueryWrapper<CouponUse>().eq(CouponUse::getUserId, userId).
                eq(CouponUse::getCouponId, couponId));
        //设置修改值
        couponUse.setCouponStatus(CouponStatus.USED);
        //调用方法修改
        couponUseMapper.updateById(couponUse);
    }

    @Override
    public CouponUse getCouponInfoAndSet(Long couponId) {
        CouponInfo couponInfo = baseMapper.selectById(couponId);
        Long userId = AuthContextHolder.getUserId();
        CouponUse couponUse1 = couponUseMapper.selectOne(new LambdaQueryWrapper<CouponUse>().
                eq(CouponUse::getCouponId, couponId).
                eq(CouponUse::getUserId, userId));
        CouponUse couponUse = new CouponUse();
        if (couponUse1==null){
            couponUse.setCouponId(couponId);
            couponUse.setUserId(userId);
            couponUse.setCouponStatus(CouponStatus.NOT_USED);
            couponUseMapper.insert(couponUse);
        }
        return couponUse;
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

    private Map<Long, List<Long>> findCouponIdToSkuIdMap(List<CartInfo> cartInfoList, List<CouponRange> couponRangeList) {
        Map<Long, List<Long>> couponToSkuIdMap = new HashMap<>();
        //couponRangeList数据处理，根据优惠卷id分组
        Map<Long, List<CouponRange>> CouponRangeToRangeListMap =
                couponRangeList.stream().collect(Collectors.groupingBy(couponRange -> couponRange.getCouponId()));
        Iterator<Map.Entry<Long, List<CouponRange>>> iterator = CouponRangeToRangeListMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, List<CouponRange>> entry = iterator.next();
            Long couponId = entry.getKey();
            List<CouponRange> rangeList = entry.getValue();
            //创建集合 Set
            Set<Long> skuIdSet = new HashSet<>();
            for (CartInfo cartInfo : cartInfoList) {
                for (CouponRange couponRange : rangeList) {
                    //判断
                    if (couponRange.getRangeType() == CouponRangeType.SKU
                            && couponRange.getRangeId().longValue() == cartInfo.getSkuId().longValue()) {
                        skuIdSet.add(cartInfo.getSkuId());
                    } else if (couponRange.getRangeType() == CouponRangeType.CATEGORY
                            && couponRange.getRangeId().longValue() == cartInfo.getCategoryId()) {
                        skuIdSet.add(cartInfo.getSkuId());
                    } else {

                    }
                }
            }
            couponToSkuIdMap.put(couponId, new ArrayList<>(skuIdSet));
        }
        return couponToSkuIdMap;
    }
}
