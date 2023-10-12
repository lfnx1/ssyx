package com.my.ssyx.product.service.impl;

import com.my.ssyx.model.product.SkuImage;
import com.my.ssyx.product.mapper.SkuImageMapper;
import com.my.ssyx.product.service.SkuImageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 商品图片 服务实现类
 * </p>
 *
 * @author lfnx
 * @since 2023-06-18
 */
@Service
public class SkuImageServiceImpl extends ServiceImpl<SkuImageMapper, SkuImage> implements SkuImageService {

    @Override
    public List<SkuImage> getImageListByskuId(Long id) {
        LambdaQueryWrapper<SkuImage> wrapper =new LambdaQueryWrapper<>();
        wrapper.eq(SkuImage::getSkuId,id);
        List<SkuImage> imageList = baseMapper.selectList(wrapper);
        return imageList;
    }
}
