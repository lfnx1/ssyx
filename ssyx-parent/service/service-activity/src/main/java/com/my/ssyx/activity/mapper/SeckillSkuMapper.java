package com.my.ssyx.activity.mapper;

import com.my.ssyx.model.activity.SeckillSku;
import com.my.ssyx.vo.activity.SeckillSkuVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeckillSkuMapper extends BaseMapper<SeckillSku> {
    void rollBackStock(Long seckillSkuId, Integer skuNum);

    Integer minusStock(Long seckillSkuId, Integer skuNum);

    List<SeckillSkuVo> findSeckillSkuListByDate(String date);
}
