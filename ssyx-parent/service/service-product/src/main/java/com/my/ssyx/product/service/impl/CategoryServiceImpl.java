package com.my.ssyx.product.service.impl;

import com.my.ssyx.model.product.Category;
import com.my.ssyx.product.mapper.CategoryMapper;
import com.my.ssyx.product.service.CategoryService;
import com.my.ssyx.vo.product.CategoryQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * <p>
 * 商品三级分类 服务实现类
 * </p>
 *
 * @author lfnx
 * @since 2023-06-18
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public IPage<Category> selectPageCategory(Page<Category> pagePram, CategoryQueryVo categoryQueryVo) {
        String name = categoryQueryVo.getName();
        LambdaQueryWrapper<Category> wrapper =new LambdaQueryWrapper();
        if (!StringUtils.isEmpty(name)){
            wrapper.like(Category::getName,name);
        }
        IPage<Category> categoryPage = baseMapper.selectPage(pagePram, wrapper);
        return  categoryPage;
    }

    @Override
    public Object findAllList() {
        LambdaQueryWrapper<Category> wrapper =new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSort);
        return this.list(wrapper);
//        List<Category> categories = baseMapper.selectList(wrapper);
//        return categories;
    }
}
