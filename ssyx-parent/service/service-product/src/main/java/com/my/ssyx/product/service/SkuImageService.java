package com.my.ssyx.product.service;

import com.my.ssyx.model.product.SkuImage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 商品图片 服务类
 * </p>
 *
 * @author lfnx
 * @since 2023-06-18
 */
public interface SkuImageService extends IService<SkuImage> {

    List<SkuImage> getImageListByskuId(Long id);
}
