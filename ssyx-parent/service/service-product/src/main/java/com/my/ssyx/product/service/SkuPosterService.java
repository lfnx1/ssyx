package com.my.ssyx.product.service;

import com.my.ssyx.model.product.SkuPoster;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 商品海报表 服务类
 * </p>
 *
 * @author lfnx
 * @since 2023-06-18
 */
public interface SkuPosterService extends IService<SkuPoster> {

    List<SkuPoster> getPosterListByskuId(Long id);
}
