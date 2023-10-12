package com.my.ssyx.product.service.impl;

import com.my.ssyx.model.product.SkuPoster;
import com.my.ssyx.product.mapper.SkuPosterMapper;
import com.my.ssyx.product.service.SkuPosterService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 商品海报表 服务实现类
 * </p>
 *
 * @author lfnx
 * @since 2023-06-18
 */
@Service
public class SkuPosterServiceImpl extends ServiceImpl<SkuPosterMapper, SkuPoster> implements SkuPosterService {

    @Override
    public List<SkuPoster> getPosterListByskuId(Long id) {
        LambdaQueryWrapper<SkuPoster> wrapper =new LambdaQueryWrapper();
        wrapper.eq(SkuPoster::getSkuId,id);
        List<SkuPoster> skuPosterList = baseMapper.selectList(wrapper);
        return skuPosterList;
    }
}
