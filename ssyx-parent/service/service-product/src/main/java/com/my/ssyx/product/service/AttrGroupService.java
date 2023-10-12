package com.my.ssyx.product.service;

import com.my.ssyx.model.product.AttrGroup;
import com.my.ssyx.vo.product.AttrGroupQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 属性分组 服务类
 * </p>
 *
 * @author lfnx
 * @since 2023-06-18
 */
public interface AttrGroupService extends IService<AttrGroup> {

    IPage<AttrGroup> selectPageAttrGroup(Page<AttrGroup> pagePram, AttrGroupQueryVo attrGroupQueryVo);
}
