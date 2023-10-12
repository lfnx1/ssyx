package com.my.ssyx.product.service;

import com.my.ssyx.model.product.Category;
import com.my.ssyx.vo.product.CategoryQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 商品三级分类 服务类
 * </p>
 *
 * @author lfnx
 * @since 2023-06-18
 */
public interface CategoryService extends IService<Category> {

    IPage<Category> selectPageCategory(Page<Category> pagePram, CategoryQueryVo categoryQueryVo);

    Object findAllList();
}
