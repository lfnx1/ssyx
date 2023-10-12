package com.my.ssyx.product.service.impl;

import com.my.ssyx.model.product.SkuAttrValue;
import com.my.ssyx.product.mapper.SkuAttrValueMapper;
import com.my.ssyx.product.service.SkuAttrValueService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * spu属性值 服务实现类
 * </p>
 *
 * @author lfnx
 * @since 2023-06-18
 */
@Service
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValue> implements SkuAttrValueService {

    @Override
    public List<SkuAttrValue> getAttrValueByskuId(Long id) {
        LambdaQueryWrapper<SkuAttrValue> wrapper =new LambdaQueryWrapper<>();
        wrapper.eq(SkuAttrValue::getSkuId,id);
        List<SkuAttrValue> skuAttrValueList = baseMapper.selectList(wrapper);
        return skuAttrValueList;
    }
}
