package com.my.ssyx.activity.service.impl;

import com.my.ssyx.activity.mapper.SeckillMapper;
import com.my.ssyx.activity.service.SeckillService;
import com.my.ssyx.model.activity.Seckill;
import com.my.ssyx.vo.activity.SeckillQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class SeckillServiceImpl extends ServiceImpl<SeckillMapper, Seckill> implements SeckillService {

    @Resource
    private SeckillMapper seckillMapper;

    @Override
    public IPage<Seckill> selectPage(Page<Seckill> pageParam, SeckillQueryVo seckillQueryVo) {
        Integer status = seckillQueryVo.getStatus();
        String title = seckillQueryVo.getTitle();
        LambdaQueryWrapper<Seckill> wrapper = new LambdaQueryWrapper<>();
        if(!StringUtils.isEmpty(status)) {
            wrapper.eq(Seckill::getStatus,status);
        }
        if(!StringUtils.isEmpty(title)) {
            wrapper.like(Seckill::getTitle,title);
        }
        IPage<Seckill> seckillPage = baseMapper.selectPage(pageParam, wrapper);
        return seckillPage;
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Seckill seckill = new Seckill();
        seckill.setStatus(status);
        seckill.setId(id);
        this.updateById(seckill);
    }

}
