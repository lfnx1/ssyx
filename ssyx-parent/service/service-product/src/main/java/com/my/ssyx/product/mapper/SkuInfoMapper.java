package com.my.ssyx.product.mapper;

import com.my.ssyx.model.product.SkuInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * sku信息 Mapper 接口
 * </p>
 *
 * @author lfnx
 * @since 2023-06-18
 */
public interface SkuInfoMapper extends BaseMapper<SkuInfo> {

    void unlock(@Param("skuId") Long skuId, @Param("skuNum") Integer skuNum);

    SkuInfo checkStock(@Param("skuId") Long skuId, @Param("skuNum")Integer skuNum);

    Integer lockStock(@Param("skuId") Long skuId,@Param("skuNum") Integer skuNum);

    void minusStock(@Param("skuId") Long skuId,@Param("skuNum") Integer skuNum);
}
