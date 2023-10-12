package com.my.ssyx.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.my.ssyx.common.constant.RedisConst;
import com.my.ssyx.common.exception.SsyxException;
import com.my.ssyx.common.result.ResultCodeEnum;
import com.my.ssyx.model.product.SkuAttrValue;
import com.my.ssyx.model.product.SkuImage;
import com.my.ssyx.model.product.SkuInfo;
import com.my.ssyx.model.product.SkuPoster;
import com.my.ssyx.mq.constant.MqConst;
import com.my.ssyx.mq.service.RabbitService;
import com.my.ssyx.product.mapper.SkuInfoMapper;
import com.my.ssyx.product.service.SkuAttrValueService;
import com.my.ssyx.product.service.SkuImageService;
import com.my.ssyx.product.service.SkuInfoService;
import com.my.ssyx.product.service.SkuPosterService;
import com.my.ssyx.vo.product.SkuInfoQueryVo;
import com.my.ssyx.vo.product.SkuInfoVo;
import com.my.ssyx.vo.product.SkuStockLockVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * <p>
 * sku信息 服务实现类
 * </p>
 *
 * @author lfnx
 * @since 2023-06-18
 */
@Service
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoMapper, SkuInfo> implements SkuInfoService {

    //sku图片
    @Autowired
    private SkuImageService skuImageService;

    //sku海报
    @Autowired
    private SkuAttrValueService skuAttrValueService;

    //添加商品sku信息
    @Autowired
    private SkuPosterService skuPosterService;


    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;


    @Override
    public IPage<SkuInfo> selectPageSkuInfo(Page<SkuInfo> pageParam, SkuInfoQueryVo skuInfoQueryVo) {
        String keyword = skuInfoQueryVo.getKeyword();
        Long categoryId = skuInfoQueryVo.getCategoryId();
        String skuType = skuInfoQueryVo.getSkuType();
        LambdaQueryWrapper<SkuInfo> wrapper = new LambdaQueryWrapper<>();
        if (!StringUtils.isEmpty(keyword)) {
            wrapper.like(SkuInfo::getSkuName, keyword);
        }
        if (!StringUtils.isEmpty(categoryId)) {
            wrapper.eq(SkuInfo::getCategoryId, categoryId);
        }
        if (!StringUtils.isEmpty(skuType)) {
            wrapper.eq(SkuInfo::getSkuType, skuType);
        }
        IPage<SkuInfo> skuInfoPage = baseMapper.selectPage(pageParam, wrapper);
        return skuInfoPage;
    }

    @Override
    public void saveSkuInfo(SkuInfoVo skuInfoVo) {
        //1.添加sku基本信息
        SkuInfo skuInfo = new SkuInfo();
        BeanUtils.copyProperties(skuInfoVo, skuInfo);
        baseMapper.insert(skuInfo);
        //2.保存sku海报
        List<SkuPoster> skuPosterList = skuInfoVo.getSkuPosterList();
        if (!CollectionUtils.isEmpty(skuPosterList)) {
            for (SkuPoster skuPoster : skuPosterList) {
                skuPoster.setSkuId(skuInfo.getId());
            }
            skuPosterService.saveBatch(skuPosterList);
        }

        //3.保存sku图片
        List<SkuImage> skuImagesList = skuInfoVo.getSkuImagesList();
        if (!CollectionUtils.isEmpty(skuImagesList)) {
            for (SkuImage skuImage : skuImagesList) {
                skuImage.setSkuId(skuInfo.getId());
            }
            skuImageService.saveBatch(skuImagesList);
        }

        //4.保存sku平台属性
        List<SkuAttrValue> skuAttrValueList = skuInfoVo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)) {
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
            }
            skuAttrValueService.saveBatch(skuAttrValueList);
        }
    }


    @Override
    public SkuInfoVo getSkuInfo(Long id) {
        SkuInfoVo skuInfoVo = new SkuInfoVo();
        //根据id查询sku基本信息
        SkuInfo skuInfo = baseMapper.selectById(id);
        //查询图片列表
        List<SkuImage> imageList = skuImageService.getImageListByskuId(id);
        //查询海报列表
        List<SkuPoster> skuPosterList = skuPosterService.getPosterListByskuId(id);
        //查询商品属性列表
        List<SkuAttrValue> skuAttrValueList = skuAttrValueService.getAttrValueByskuId(id);
        //封装数据
        BeanUtils.copyProperties(skuInfo, skuInfoVo);
        skuInfoVo.setSkuImagesList(imageList);
        skuInfoVo.setSkuPosterList(skuPosterList);
        skuInfoVo.setSkuAttrValueList(skuAttrValueList);
        return skuInfoVo;
    }

    @Override
    public void updateSkuInfo(SkuInfoVo skuInfoVo) {
        //修改基本信息
        SkuInfo skuInfo = new SkuInfo();
        BeanUtils.copyProperties(skuInfoVo, skuInfo);
        baseMapper.updateById(skuInfo);
        Long id = skuInfo.getId();
        //海报信息
        LambdaQueryWrapper<SkuPoster> wrapper = new LambdaQueryWrapper();
        wrapper.eq(SkuPoster::getSkuId, id);
        skuPosterService.remove(wrapper);
        List<SkuPoster> skuPosterList = skuInfoVo.getSkuPosterList();
        if (!CollectionUtils.isEmpty(skuPosterList)) {
            for (SkuPoster skuPoster : skuPosterList) {
                skuPoster.setSkuId(skuInfo.getId());
            }
            skuPosterService.saveBatch(skuPosterList);
        }
        //商品图片
        skuImageService.remove(new LambdaQueryWrapper<SkuImage>().eq(SkuImage::getSkuId, id));
        List<SkuImage> skuImagesList = skuInfoVo.getSkuImagesList();
        if (!CollectionUtils.isEmpty(skuImagesList)) {
            for (SkuImage skuImage : skuImagesList) {
                skuImage.setSkuId(skuInfo.getId());
            }
            skuImageService.saveBatch(skuImagesList);
        }
        //商品属性
        skuAttrValueService.remove(new LambdaQueryWrapper<SkuAttrValue>().eq(SkuAttrValue::getSkuId, id));
        List<SkuAttrValue> skuAttrValueList = skuInfoVo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)) {
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
            }
            skuAttrValueService.saveBatch(skuAttrValueList);
        }
    }

    @Override
    public void check(Long skuId, Integer status) {
        SkuInfo skuInfo = baseMapper.selectById(skuId);
        skuInfo.setCheckStatus(status);
        baseMapper.updateById(skuInfo);
    }

    @Override
    public void publish(Long id, Integer status) {
        if (status == 1) {
            //上架
            SkuInfo skuInfo = baseMapper.selectById(id);
            skuInfo.setPublishStatus(status);
            System.out.println("__________________________");
            System.out.println(skuInfo.toString());
            baseMapper.updateById(skuInfo);
            //整合mq同步到es里面
            //发送到mq中
            rabbitService.sendMessage(MqConst.EXCHANGE_GOODS_DIRECT, MqConst.ROUTING_GOODS_UPPER, id);

        } else {
            //下架
            SkuInfo skuInfo = baseMapper.selectById(id);
            skuInfo.setPublishStatus(status);
            baseMapper.updateById(skuInfo);
            //整合mq同步到es里面
            //发送到mq中
            rabbitService.sendMessage(MqConst.EXCHANGE_GOODS_DIRECT, MqConst.ROUTING_GOODS_LOWER, id);
        }

    }

    @Override
    public void isNewPerson(Long skuId, Integer status) {
        SkuInfo skuInfoUp = new SkuInfo();
        skuInfoUp.setId(skuId);
        skuInfoUp.setIsNewPerson(status);
        baseMapper.updateById(skuInfoUp);
    }

    @Override
    public List<SkuInfo> findSkuInfoList(List<Long> skuIdList) {
        List<SkuInfo> skuInfoList = baseMapper.selectBatchIds(skuIdList);
        return skuInfoList;
    }

    @Override
    public List<SkuInfo> findSkuInfoByKeyword(String keyword) {
        List<SkuInfo> skuInfoList = baseMapper.selectList(new LambdaQueryWrapper<SkuInfo>().
                like(SkuInfo::getSkuName, keyword));
        return skuInfoList;
    }

    @Override
    public List<SkuInfo> findNewPersonSkuInfoList() {
        //条件1: is_new_person=1
        //条件2: publish_status=1
        //条件3: 显示其中三个
        //获取第一页数据  每页显示三条记录
        Page<SkuInfo> pageParam = new Page<>(1,3);
        //封装
        IPage<SkuInfo> skuInfoPage = baseMapper.selectPage(pageParam, new LambdaQueryWrapper<SkuInfo>()
                .eq(SkuInfo::getIsNewPerson, 1).
                eq(SkuInfo::getPublishStatus, 1).orderByDesc(SkuInfo::getStock));
        List<SkuInfo> records = skuInfoPage.getRecords();
        return records;
    }

    @Override
    public SkuInfoVo getSkuInfoVo(Long skuId) {
        SkuInfoVo skuInfoVo =new SkuInfoVo();
        //根据SkuId查询基本信息
        SkuInfo skuInfo = baseMapper.selectById(skuId);
        //根据SkuId查询图片
        List<SkuImage> imageListBySkuId = skuImageService.getImageListByskuId(skuId);
        //根据skuId查询海报
        List<SkuPoster> posterListBySkuId = skuPosterService.getPosterListByskuId(skuId);
        //根据skuId查询属性
        List<SkuAttrValue> attrValueBySkuId = skuAttrValueService.getAttrValueByskuId(skuId);
        //封装到skuInfoVo对象
        BeanUtils.copyProperties(skuInfo,skuInfoVo);
        skuInfoVo.setSkuImagesList(imageListBySkuId);
        skuInfoVo.setSkuPosterList(posterListBySkuId);
        skuInfoVo.setSkuAttrValueList(attrValueBySkuId);
        return skuInfoVo;
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public Boolean checkAndLock(List<SkuStockLockVo> skuStockLockVoList, String orderNo) {
        //1.判断skuStockLockVoList是否为空
        if (CollectionUtils.isEmpty(skuStockLockVoList)){
            throw new SsyxException(ResultCodeEnum.DATA_ERROR);
        }
        //2.遍历 skuStockLockVoList得到每个商品 验证库存并且进行锁定操作 保证原子性
        skuStockLockVoList.stream().forEach(skuStockLockVo -> {
            this.checkLock(skuStockLockVo);
        });
        //3.只要有一个商品锁定失败  所有锁定成功的商品都解锁
        boolean flag = skuStockLockVoList.stream().anyMatch(skuStockLockVo ->
            !skuStockLockVo.getIsLock());
        if (flag){
            //所有锁定成功的商品都解锁
            skuStockLockVoList.stream().filter(SkuStockLockVo::getIsLock).forEach(skuStockLockVo -> {
                baseMapper.unlock(skuStockLockVo.getSkuId(),skuStockLockVo.getSkuNum());
            });
            //返回失败状态
            return  false;
        }
        //4.如果所有商品都锁定成功了 redis缓存相关数据  为了方便后面解锁和减库存
        redisTemplate.opsForValue().set(RedisConst.STOCK_INFO+orderNo,skuStockLockVoList);
        return true;
    }

    @Override
    public void minusStock(String orderNo) {
        //从Redis中获取锁定库存信息
        List<SkuStockLockVo> stockLockVoList = ( List<SkuStockLockVo> )redisTemplate.
                opsForValue().get(RedisConst.STOCK_INFO + orderNo);
        if (CollectionUtils.isEmpty(stockLockVoList)){
            return;
        }
        //遍历集合得到每个对象 减库存
        stockLockVoList.forEach(skuStockLockVo -> {
            baseMapper.minusStock(skuStockLockVo.getSkuId(),skuStockLockVo.getSkuNum());
        });
        //删除Redis数据
        redisTemplate.delete(RedisConst.STOCK_INFO+orderNo);
    }



    private void checkLock(SkuStockLockVo skuStockLockVo) {
        //获取锁
        //公平锁
        RLock rLock = this.redissonClient.getFairLock(RedisConst.SKUKEY_PREFIX + skuStockLockVo.getSkuId());
        //加锁
        rLock.lock();
        try {
            //验证库存
           SkuInfo skuInfo= baseMapper.checkStock(skuStockLockVo.getSkuId(),skuStockLockVo.getSkuNum());
           //判断
            //没有满足条件商品 设置isLock为false
           if (skuInfo==null){
               skuStockLockVo.setIsLock(false);
               return;
           }
           //有满足条件商品
            //锁定库存: update
            Integer rows =baseMapper.lockStock(skuStockLockVo.getSkuId(),skuStockLockVo.getSkuNum());
           if (rows==1){
               skuStockLockVo.setIsLock(true);
           }
        }finally {
            //解锁
            rLock.unlock();
        }
    }
}
