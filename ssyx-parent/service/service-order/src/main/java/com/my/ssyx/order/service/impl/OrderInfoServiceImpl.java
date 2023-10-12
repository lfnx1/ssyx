package com.my.ssyx.order.service.impl;

import com.my.ssyx.activity.client.ActivityFeignClient;
import com.my.ssyx.client.CartFeignClient;
import com.my.ssyx.client.product.ProductFeignClient;
import com.my.ssyx.client.user.UserFeignClient;
import com.my.ssyx.common.auth.AuthContextHolder;
import com.my.ssyx.common.constant.RedisConst;
import com.my.ssyx.common.exception.SsyxException;
import com.my.ssyx.common.result.ResultCodeEnum;
import com.my.ssyx.common.utils.DateUtil;
import com.my.ssyx.enums.*;
import com.my.ssyx.model.activity.ActivityRule;
import com.my.ssyx.model.activity.CouponInfo;
import com.my.ssyx.model.order.CartInfo;
import com.my.ssyx.model.order.OrderInfo;
import com.my.ssyx.model.order.OrderItem;
import com.my.ssyx.mq.constant.MqConst;
import com.my.ssyx.mq.service.RabbitService;
import com.my.ssyx.order.mapper.OrderInfoMapper;
import com.my.ssyx.order.mapper.OrderItemMapper;
import com.my.ssyx.order.service.OrderInfoService;
import com.my.ssyx.order.service.OrderItemService;
import com.my.ssyx.vo.order.CartInfoVo;
import com.my.ssyx.vo.order.OrderConfirmVo;
import com.my.ssyx.vo.order.OrderSubmitVo;
import com.my.ssyx.vo.order.OrderUserQueryVo;
import com.my.ssyx.vo.product.SkuStockLockVo;
import com.my.ssyx.vo.user.LeaderAddressVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper,OrderInfo> implements OrderInfoService {

    @Resource
    private UserFeignClient userFeignClient;
    @Resource
    private CartFeignClient cartFeignClient;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private RabbitService rabbitService;

    @Resource
    private ActivityFeignClient activityFeignClient;

    @Resource
    private ProductFeignClient productFeignClient;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private OrderItemService orderItemService;
    @Override
    public OrderConfirmVo confirmOrder() {
        //获取用户id
        Long userId = AuthContextHolder.getUserId();
        //获取团长相关信息
        LeaderAddressVo leaderAddressVo = userFeignClient.getUserAddressByUserId(userId);
        //获取购物车里面选中的商品
        List<CartInfo> cartInfoList = cartFeignClient.getCartCheckedList(userId);
        //唯一标识
        String orderVo =System.currentTimeMillis()+"";
        redisTemplate.opsForValue().set(RedisConst.ORDER_REPEAT+orderVo,orderVo,24, TimeUnit.HOURS);
        //获取购物车满足条件活动和优惠卷信息
        OrderConfirmVo orderConfirmVo = activityFeignClient.findCartActivityAndCoupon(cartInfoList, userId);
        //封装其他值
        orderConfirmVo.setLeaderAddressVo(leaderAddressVo);
        orderConfirmVo.setOrderNo(orderVo);
        return orderConfirmVo;
    }

    //生成订单
    @Override
    public Long submitOrder(OrderSubmitVo orderParamVo) {
        //第一步 设置某个用户订单 设置 userId
        Long userId = AuthContextHolder.getUserId();
        orderParamVo.setUserId(userId);
        //第二步 订单不能重复提交 重复提交验证
        // 通过redis进行判断 +lua脚本 (保证原子性)
        //1.获取传递过来的订单 orderNo
        String orderNo = orderParamVo.getOrderNo();
        if (StringUtils.isEmpty(orderNo)){
            throw  new SsyxException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        //2.拿着orderNo 到redis进行查询
        //lua脚本 redis官方书写
        String script ="if(redis.call('get', KEYS[1]) == ARGV[1]) then return " +
                "redis.call('del', KEYS[1]) else return 0 end";
        //3.如果redis有相同的orderNo,表示正常的提交订单 把redis 的orderNo删除
        Boolean flag =(Boolean) redisTemplate.execute(new DefaultRedisScript(script, Boolean.class),
                Arrays.asList(RedisConst.ORDER_REPEAT + orderNo), orderNo);
        //4. 如果redis没有orderNo 表示重复提交  不能再往后进行
        if (!flag){
            throw new SsyxException(ResultCodeEnum.REPEAT_SUBMIT);
        }
        //第三步 验证库存 并且 锁定库存(分布式锁)
        //待支付完成之后才真正减少库存
        //1.获取当前用户购物车商品选中购物项
        List<CartInfo> cartInfoList = cartFeignClient.getCartCheckedList(userId);
        //2.购物车有很多商品 商品有不同类型  重点处理普通类商品
        List<CartInfo> commonSkuList = cartInfoList.stream().filter(cartInfo -> cartInfo.getSkuType()
                == SkuType.COMMON.getCode()).collect(Collectors.toList());
        //3.把获取购物车里面普通类型商品list集合 转换为List<SkuStockVo>
        if (!CollectionUtils.isEmpty(commonSkuList)){
            List<SkuStockLockVo> commonSkuStockLockVoList = commonSkuList.stream().map(item -> {
                SkuStockLockVo skuStockLockVo = new SkuStockLockVo();
                skuStockLockVo.setSkuId(item.getSkuId());
                skuStockLockVo.setSkuNum(item.getSkuNum());
                return skuStockLockVo;
            }).collect(Collectors.toList());
            //4.远程调用service-product模块实现锁定商品
            //验证库存并锁定库存，保证原子性
            Boolean isLockSuccess = productFeignClient.checkAndLock(commonSkuStockLockVoList, orderNo);
            if (!isLockSuccess){
                throw new SsyxException(ResultCodeEnum.ORDER_STOCK_FALL);
            }
        }
        //第四步 下单过程
        //1.向两张表里面添加数据 order_info order_item
        Long orderId=this.saveOrder(orderParamVo,cartInfoList);
        //下单完成，删除购物车记录
        //发送mq消息
        rabbitService.sendMessage(MqConst.EXCHANGE_ORDER_DIRECT,
                MqConst.ROUTING_DELETE_CART, orderParamVo.getUserId());
        //第五步 返回订单id
        return orderId;
    }
    @Transactional(rollbackFor = {Exception.class})
    public Long saveOrder(OrderSubmitVo orderParamVo, List<CartInfo> cartInfoList) {
        if (CollectionUtils.isEmpty(cartInfoList)){
            throw new SsyxException(ResultCodeEnum.DATA_ERROR);
        }
        //获取用户提货点和团长信息
        Long userId = AuthContextHolder.getUserId();
        LeaderAddressVo leaderAddressVo = userFeignClient.getUserAddressByUserId(userId);
        if (leaderAddressVo==null){
            throw new SsyxException(ResultCodeEnum.DATA_ERROR);
        }
        //计算金额
        //营销活动
        Map<String, BigDecimal> activitySplitAmount = this.computeActivitySplitAmount(cartInfoList);
        // 优惠卷 金额
        Map<String, BigDecimal> couponInfoSplitAmount = this.
                computeCouponInfoSplitAmount(cartInfoList, orderParamVo.getCouponId());

        //封装订单项的数据
        List<OrderItem> orderItemList =new ArrayList<>();
        for (CartInfo cartInfo : cartInfoList) {
            OrderItem orderItem =new OrderItem();
            orderItem.setId(null);
            orderItem.setCategoryId(cartInfo.getCategoryId());
            if(cartInfo.getSkuType() == SkuType.COMMON.getCode()) {
                orderItem.setSkuType(SkuType.COMMON);
            } else {
                orderItem.setSkuType(SkuType.SECKILL);
            }
            orderItem.setSkuId(cartInfo.getSkuId());
            orderItem.setSkuName(cartInfo.getSkuName());
            orderItem.setSkuPrice(cartInfo.getCartPrice());
            orderItem.setImgUrl(cartInfo.getImgUrl());
            orderItem.setSkuNum(cartInfo.getSkuNum());
            orderItem.setLeaderId(orderParamVo.getLeaderId());
            //营销活动金额
            BigDecimal activityAmount = activitySplitAmount.get("activity:" + orderItem.getSkuId());
            if (activityAmount==null){
                activityAmount=new BigDecimal(0);
            }
            orderItem.setSplitActivityAmount(activityAmount);
            //优惠卷金额
            BigDecimal couponAmount =couponInfoSplitAmount.get("coupon:" + orderItem.getSkuId());
            if (couponAmount==null){
                couponAmount=new BigDecimal(0);
            }
            orderItem.setSplitCouponAmount(couponAmount);
            //总金额
            BigDecimal skuTotalAmount = orderItem.getSkuPrice().multiply(new BigDecimal(orderItem.getSkuNum()));
            //优惠之后的金额
            BigDecimal splitTotalAmount = skuTotalAmount.subtract(activityAmount).subtract(couponAmount);
            orderItem.setSplitTotalAmount(splitTotalAmount);
            orderItemList.add(orderItem);
        }

        //封装订单部分 order_Info 部分
        OrderInfo order =new OrderInfo();
        order.setUserId(userId);
        order.setOrderNo(orderParamVo.getOrderNo());
        order.setOrderStatus(OrderStatus.UNPAID);
        order.setProcessStatus(ProcessStatus.UNPAID);
        order.setCouponId(orderParamVo.getCouponId());
        order.setLeaderId(orderParamVo.getLeaderId());
        order.setLeaderName(leaderAddressVo.getLeaderName());
        order.setLeaderPhone(leaderAddressVo.getLeaderPhone());
        order.setTakeName(leaderAddressVo.getTakeName());
        order.setReceiverName(orderParamVo.getReceiverName());
        order.setReceiverPhone(orderParamVo.getReceiverPhone());
        order.setReceiverProvince(leaderAddressVo.getProvince());
        order.setReceiverCity(leaderAddressVo.getCity());
        order.setReceiverDistrict(leaderAddressVo.getDistrict());
        order.setReceiverAddress(leaderAddressVo.getDetailAddress());
        order.setWareId(cartInfoList.get(0).getWareId());
        //计算订单金额
        BigDecimal originalTotalAmount = this.computeTotalAmount(cartInfoList);
        BigDecimal activityAmount = activitySplitAmount.get("activity:total");
        if(null == activityAmount) activityAmount = new BigDecimal(0);
        BigDecimal couponAmount = couponInfoSplitAmount.get("coupon:total");
        if(null == couponAmount) couponAmount = new BigDecimal(0);
        BigDecimal totalAmount = originalTotalAmount.subtract(activityAmount).subtract(couponAmount);
        //计算订单金额
        order.setOriginalTotalAmount(originalTotalAmount);
        order.setActivityAmount(activityAmount);
        order.setCouponAmount(couponAmount);
        order.setTotalAmount(totalAmount);
        //计算团长佣金
        BigDecimal profitRate = new BigDecimal(10);//orderSetService.getProfitRate();
        BigDecimal commissionAmount = order.getTotalAmount().multiply(profitRate);
        order.setCommissionAmount(commissionAmount);
        //添加数据到该表中
        baseMapper.insert(order);
        for(OrderItem orderItem : orderItemList) {
            orderItem.setOrderId(order.getId());
        }
        orderItemService.saveBatch(orderItemList);

        //如果当前订单使用了优惠卷，更新优惠卷状态
        if (order.getCouponId()!=null&&order.getCouponId()!=0){
            activityFeignClient.updateCouponInfoUseStatus
                    (order.getCouponId(),userId,order.getId());
        }
        //下单成功，记录用户购物商品数量 redis
        String orderSkuKey = RedisConst.ORDER_SKU_MAP + orderParamVo.getUserId();
        BoundHashOperations<String, String, Integer> hashOperations = redisTemplate.boundHashOps(orderSkuKey);
        cartInfoList.forEach(cartInfo -> {
            if(hashOperations.hasKey(cartInfo.getSkuId().toString())) {
                Integer orderSkuNum = hashOperations.get(cartInfo.getSkuId().toString()) + cartInfo.getSkuNum();
                hashOperations.put(cartInfo.getSkuId().toString(), orderSkuNum);
            }
        });
        redisTemplate.expire(orderSkuKey, DateUtil.getCurrentExpireTimes(), TimeUnit.SECONDS);
        //订单id
        return order.getId();

    }

    @Override
    public OrderInfo getOrderInfoById(Long orderId) {
        //根据orderId查询基本信息
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        //根据orderId查询所有订单项list列表
        List<OrderItem> orderItemList = orderItemMapper.selectList
                (new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        //查询所有订单项封装到每个订单对象里面
        orderInfo.setOrderItemList(orderItemList);
        return orderInfo;
    }

    @Override
    public OrderInfo getOrderInfoByOrderNo(String orderNo) {
        OrderInfo orderInfo = baseMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        return orderInfo;
    }

    @Override
    public void orderPay(String orderNo) {
        OrderInfo orderInfo = this.getOrderInfoByOrderNo(orderNo);
        if(orderInfo==null||orderInfo.getOrderStatus()!=OrderStatus.UNPAID){
            return;
        }
        //更新状态
        this.updateOrderStatus(orderInfo.getId());

        //扣减库存
        rabbitService.sendMessage(MqConst.EXCHANGE_ORDER_DIRECT,MqConst.ROUTING_MINUS_STOCK,orderNo);
    }

    @Override
    public IPage<OrderInfo> getOrderInfoByUserIdPage(Page<OrderInfo> pageParam, OrderUserQueryVo orderUserQueryVo) {
        IPage<OrderInfo> pageModel = baseMapper.selectPage(pageParam, new LambdaQueryWrapper<OrderInfo>().
                eq(OrderInfo::getUserId, orderUserQueryVo.getUserId()).
                eq(OrderInfo::getOrderStatus, orderUserQueryVo.getOrderStatus()));
        //获取每个订单 把每个订单项查询封装
        List<OrderInfo> orderInfoList = pageModel.getRecords();
        for (OrderInfo orderInfo : orderInfoList) {
            //根据订单id查询里面所有订单项列表
            List<OrderItem> orderItemList = orderItemMapper.selectList(new
                    LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderInfo.getId()));
            orderInfo.setOrderItemList(orderItemList);
            //封装订单状态名称
            orderInfo.getParam().put("orderStatusName",orderInfo.getOrderStatus().getComment());
        }
        return pageModel;
    }

    private void updateOrderStatus(Long id) {
        OrderInfo orderInfo = baseMapper.selectById(id);
        orderInfo.setOrderStatus(OrderStatus.WAITING_DELEVER);
        orderInfo.setProcessStatus(ProcessStatus.WAITING_DELEVER);
        baseMapper.updateById(orderInfo);
    }

    //计算总金额
    private BigDecimal computeTotalAmount(List<CartInfo> cartInfoList) {
        BigDecimal total = new BigDecimal(0);
        for (CartInfo cartInfo : cartInfoList) {
            BigDecimal itemTotal = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
            total = total.add(itemTotal);
        }
        return total;
    }

    /**
     * 计算购物项分摊的优惠减少金额
     * 打折：按折扣分担
     * 现金：按比例分摊
     * @param cartInfoParamList
     * @return
     */
    private Map<String, BigDecimal> computeActivitySplitAmount(List<CartInfo> cartInfoParamList) {
        Map<String, BigDecimal> activitySplitAmountMap = new HashMap<>();

        //促销活动相关信息
        List<CartInfoVo> cartInfoVoList = activityFeignClient.findCartActivityList(cartInfoParamList);

        //活动总金额
        BigDecimal activityReduceAmount = new BigDecimal(0);
        if(!CollectionUtils.isEmpty(cartInfoVoList)) {
            for(CartInfoVo cartInfoVo : cartInfoVoList) {
                ActivityRule activityRule = cartInfoVo.getActivityRule();
                List<CartInfo> cartInfoList = cartInfoVo.getCartInfoList();
                if(null != activityRule) {
                    //优惠金额， 按比例分摊
                    BigDecimal reduceAmount = activityRule.getReduceAmount();
                    activityReduceAmount = activityReduceAmount.add(reduceAmount);
                    if(cartInfoList.size() == 1) {
                        activitySplitAmountMap.put("activity:"+cartInfoList.get(0).getSkuId(), reduceAmount);
                    } else {
                        //总金额
                        BigDecimal originalTotalAmount = new BigDecimal(0);
                        for(CartInfo cartInfo : cartInfoList) {
                            BigDecimal skuTotalAmount = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
                            originalTotalAmount = originalTotalAmount.add(skuTotalAmount);
                        }
                        //记录除最后一项是所有分摊金额， 最后一项=总的 - skuPartReduceAmount
                        BigDecimal skuPartReduceAmount = new BigDecimal(0);
                        if (activityRule.getActivityType() == ActivityType.FULL_REDUCTION) {
                            for(int i=0, len=cartInfoList.size(); i<len; i++) {
                                CartInfo cartInfo = cartInfoList.get(i);
                                if(i < len -1) {
                                    BigDecimal skuTotalAmount = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
                                    //sku分摊金额
                                    BigDecimal skuReduceAmount = skuTotalAmount.divide(originalTotalAmount, 2, RoundingMode.HALF_UP).multiply(reduceAmount);
                                    activitySplitAmountMap.put("activity:"+cartInfo.getSkuId(), skuReduceAmount);

                                    skuPartReduceAmount = skuPartReduceAmount.add(skuReduceAmount);
                                } else {
                                    BigDecimal skuReduceAmount = reduceAmount.subtract(skuPartReduceAmount);
                                    activitySplitAmountMap.put("activity:"+cartInfo.getSkuId(), skuReduceAmount);
                                }
                            }
                        } else {
                            for(int i=0, len=cartInfoList.size(); i<len; i++) {
                                CartInfo cartInfo = cartInfoList.get(i);
                                if(i < len -1) {
                                    BigDecimal skuTotalAmount = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));

                                    //sku分摊金额
                                    BigDecimal skuDiscountTotalAmount = skuTotalAmount.multiply(activityRule.getBenefitDiscount().divide(new BigDecimal("10")));
                                    BigDecimal skuReduceAmount = skuTotalAmount.subtract(skuDiscountTotalAmount);
                                    activitySplitAmountMap.put("activity:"+cartInfo.getSkuId(), skuReduceAmount);

                                    skuPartReduceAmount = skuPartReduceAmount.add(skuReduceAmount);
                                } else {
                                    BigDecimal skuReduceAmount = reduceAmount.subtract(skuPartReduceAmount);
                                    activitySplitAmountMap.put("activity:"+cartInfo.getSkuId(), skuReduceAmount);
                                }
                            }
                        }
                    }
                }
            }
        }
        activitySplitAmountMap.put("activity:total", activityReduceAmount);
        return activitySplitAmountMap;
    }

    //优惠卷优惠金额
    private Map<String, BigDecimal> computeCouponInfoSplitAmount(List<CartInfo> cartInfoList, Long couponId) {
        Map<String, BigDecimal> couponInfoSplitAmountMap = new HashMap<>();

        if(null == couponId) return couponInfoSplitAmountMap;
        CouponInfo couponInfo = activityFeignClient.findRangeSkuIdList(cartInfoList, couponId);

        if(null != couponInfo) {
            //sku对应的订单明细
            Map<Long, CartInfo> skuIdToCartInfoMap = new HashMap<>();
            for (CartInfo cartInfo : cartInfoList) {
                skuIdToCartInfoMap.put(cartInfo.getSkuId(), cartInfo);
            }
            //优惠券对应的skuId列表
            List<Long> skuIdList = couponInfo.getSkuIdList();
            if(CollectionUtils.isEmpty(skuIdList)) {
                return couponInfoSplitAmountMap;
            }
            //优惠券优化总金额
            BigDecimal reduceAmount = couponInfo.getAmount();
            if(skuIdList.size() == 1) {
                //sku的优化金额
                couponInfoSplitAmountMap.put("coupon:"+skuIdToCartInfoMap.get(skuIdList.get(0)).getSkuId(), reduceAmount);
            } else {
                //总金额
                BigDecimal originalTotalAmount = new BigDecimal(0);
                for (Long skuId : skuIdList) {
                    CartInfo cartInfo = skuIdToCartInfoMap.get(skuId);
                    BigDecimal skuTotalAmount = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
                    originalTotalAmount = originalTotalAmount.add(skuTotalAmount);
                }
                //记录除最后一项是所有分摊金额， 最后一项=总的 - skuPartReduceAmount
                BigDecimal skuPartReduceAmount = new BigDecimal(0);
                if (couponInfo.getCouponType() == CouponType.CASH || couponInfo.getCouponType() == CouponType.FULL_REDUCTION) {
                    for(int i=0, len=skuIdList.size(); i<len; i++) {
                        CartInfo cartInfo = skuIdToCartInfoMap.get(skuIdList.get(i));
                        if(i < len -1) {
                            BigDecimal skuTotalAmount = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
                            //sku分摊金额
                            BigDecimal skuReduceAmount = skuTotalAmount.divide(originalTotalAmount, 2, RoundingMode.HALF_UP).multiply(reduceAmount);
                            couponInfoSplitAmountMap.put("coupon:"+cartInfo.getSkuId(), skuReduceAmount);

                            skuPartReduceAmount = skuPartReduceAmount.add(skuReduceAmount);
                        } else {
                            BigDecimal skuReduceAmount = reduceAmount.subtract(skuPartReduceAmount);
                            couponInfoSplitAmountMap.put("coupon:"+cartInfo.getSkuId(), skuReduceAmount);
                        }
                    }
                }
            }
            couponInfoSplitAmountMap.put("coupon:total", couponInfo.getAmount());
        }
        return couponInfoSplitAmountMap;
    }
}
