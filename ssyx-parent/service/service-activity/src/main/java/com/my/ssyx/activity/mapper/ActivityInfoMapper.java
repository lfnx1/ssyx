package com.my.ssyx.activity.mapper;

import com.my.ssyx.model.activity.ActivityInfo;
import com.my.ssyx.model.activity.ActivityRule;
import com.my.ssyx.model.activity.ActivitySku;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <p>
 * 活动表 Mapper 接口
 * </p>
 *
 * @author lfnx
 * @since 2023-06-21
 */
@Repository
public interface ActivityInfoMapper extends BaseMapper<ActivityInfo> {


    List<Long> selectExistSkuIdList(@Param("skuIdList") List<Long> skuIdList);

    List<ActivityRule> findActivityRule(@Param("skuId") Long skuId);

    List<ActivitySku> selectCartActivity(@Param("skuIdList") List<Long> skuIdList);
}
