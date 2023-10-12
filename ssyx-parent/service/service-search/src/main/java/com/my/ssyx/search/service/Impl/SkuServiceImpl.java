package com.my.ssyx.search.service.Impl;

import com.my.ssyx.activity.client.ActivityFeignClient;
import com.my.ssyx.client.product.ProductFeignClient;
import com.my.ssyx.common.auth.AuthContextHolder;
import com.my.ssyx.enums.SkuType;
import com.my.ssyx.model.product.Category;
import com.my.ssyx.model.product.SkuInfo;
import com.my.ssyx.model.search.SkuEs;
import com.my.ssyx.search.repository.SkuRepository;
import com.my.ssyx.search.service.SkuService;
import com.my.ssyx.vo.search.SkuEsQueryVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    private SkuRepository skuRepository;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private ActivityFeignClient activityFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    //上架
    public void upperSku(Long skuId) {
        //1.通过远程调用 根据skuId获取相关信息
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        if (skuInfo==null){
            return;
        }
        Category category = productFeignClient.getCategory(skuInfo.getCategoryId());
        if (category==null){
            return;
        }
        //2获取数据封装SkuEs对象
        SkuEs skuEs =new SkuEs();
        //封装分类
        if (category!=null){
            skuEs.setCategoryId(category.getId());
            skuEs.setCategoryName(category.getName());
        }
        //封装sku信息部分
        skuEs.setId(skuInfo.getId());
        skuEs.setKeyword(skuInfo.getSkuName()+","+skuEs.getCategoryName());
        skuEs.setWareId(skuInfo.getWareId());
        skuEs.setIsNewPerson(skuInfo.getIsNewPerson());
        skuEs.setImgUrl(skuInfo.getImgUrl());
        skuEs.setTitle(skuInfo.getSkuName());
        if(skuInfo.getSkuType() == SkuType.COMMON.getCode()) {//普通商品
            skuEs.setSkuType(0);
            skuEs.setPrice(skuInfo.getPrice().doubleValue());
            skuEs.setStock(skuInfo.getStock());
            skuEs.setSale(skuInfo.getSale());
            skuEs.setPerLimit(skuInfo.getPerLimit());
        } else {
            //TODO 待完善-秒杀商品

        }
        //3.调用方法添加Es
        SkuEs save = skuRepository.save(skuEs);
    }

    @Override
    //下架
    public void lowerSku(Long skuId) {
        skuRepository.deleteById(skuId);
    }

    @Override
    public List<SkuEs> findHotSkuList() {
        //find read get 开头
        //关联条件关键字
        //0代表第一页
        Pageable pageable = PageRequest.of(0,10);
        Page<SkuEs> pageModel = skuRepository.findByOrderByHotScoreDesc(pageable);
        List<SkuEs> skuEsList = pageModel.getContent();
        return skuEsList;
    }

    @Override
    public Page<SkuEs> search(Pageable pageable, SkuEsQueryVo skuEsQueryVo) {
        //1 向SkuEsQueryVo中设置当前登录用户仓库id
        skuEsQueryVo.setWareId(AuthContextHolder.getWareId());

        //2.调用skuRepository方法，根据SpringData命名规则定义方法  进行条件查询
        //判断keyword是否为空 如果空 根据仓库id +分类id查询
        String keyword = skuEsQueryVo.getKeyword();

        Page<SkuEs> pageModel =null;
        if (StringUtils.isEmpty(keyword)){
            pageModel= skuRepository.findByCategoryIdAndWareId(
                            skuEsQueryVo.getCategoryId(),
                            skuEsQueryVo.getWareId(),
                            pageable);
        }else {
            //      如果keyword不为空根据仓库id + keyword进行查询
            pageModel= skuRepository.findByKeywordAndWareId(
                    skuEsQueryVo.getKeyword(),
                    skuEsQueryVo.getWareId(),
                    pageable);
        }
        //3.查询商品参加优惠活动
        List<SkuEs> skuEsList = pageModel.getContent();
        if (!CollectionUtils.isEmpty(skuEsList)){
            List<Long> skuIdList = skuEsList.stream().map
                    (item -> item.getId()).collect(Collectors.toList());
            //根据skuId列表远程调用  调用service-activity
            //返回Map<Long,list<String>>
            //key为 skuId值
            //value为 sku参与活动里面多个规则名称列表
            Map<Long,List<String>> skuIdToRuleListMap =activityFeignClient.findActivity(skuIdList);
            //封装获取数据到skuEs里面 ruleList属性
            if (skuIdToRuleListMap!=null){
                for (SkuEs skuEs : skuEsList) {
                    skuEs.setRuleList(skuIdToRuleListMap.get(skuEs.getId()
                    ));
                }
            }
        }

        return pageModel;
    }

    @Override
    public void incrHotScore(Long skuId) {
        String key = "hotScore";
        //redis 保存数据 每次加一
        Double hotScore = redisTemplate.opsForZSet().incrementScore(key, "skuId" + skuId, 1);
        //规则
        if (hotScore%10==0){
            //更新es
            Optional<SkuEs> optional = skuRepository.findById(skuId);
            SkuEs skuEs = optional.get();
            skuEs.setHotScore(Math.round(hotScore));
            skuRepository.save(skuEs);
        }
    }
}
