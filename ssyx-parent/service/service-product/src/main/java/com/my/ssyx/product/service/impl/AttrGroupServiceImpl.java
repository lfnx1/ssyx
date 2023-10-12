package com.my.ssyx.product.service.impl;

import com.my.ssyx.model.product.AttrGroup;
import com.my.ssyx.product.mapper.AttrGroupMapper;
import com.my.ssyx.product.service.AttrGroupService;
import com.my.ssyx.vo.product.AttrGroupQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * <p>
 * 属性分组 服务实现类
 * </p>
 *
 * @author lfnx
 * @since 2023-06-18
 */
@Service
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupMapper, AttrGroup> implements AttrGroupService {

    @Override
    public IPage<AttrGroup> selectPageAttrGroup(Page<AttrGroup> pagePram, AttrGroupQueryVo attrGroupQueryVo) {
        String name = attrGroupQueryVo.getName();
        LambdaQueryWrapper<AttrGroup> wrapper =new LambdaQueryWrapper();
        if (!StringUtils.isEmpty(name)){
            wrapper.eq(AttrGroup::getName,name);
        }
        IPage<AttrGroup> attrGroupPage = baseMapper.selectPage(pagePram, wrapper);
        return attrGroupPage;
    }
}
