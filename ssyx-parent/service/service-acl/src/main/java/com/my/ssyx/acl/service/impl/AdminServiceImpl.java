package com.my.ssyx.acl.service.impl;

import com.my.ssyx.acl.mapper.AdminMapper;
import com.my.ssyx.acl.service.AdminService;
import com.my.ssyx.model.acl.Admin;
import com.my.ssyx.vo.acl.AdminQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    @Override
    public IPage<Admin> selectPageUser(Page<Admin> page, AdminQueryVo adminQueryVo) {
        String username = adminQueryVo.getUsername();
        String name = adminQueryVo.getName();
        LambdaQueryWrapper<Admin> wrapper=new LambdaQueryWrapper<>();
        if (!StringUtils.isEmpty(username)){
            wrapper.eq(Admin::getUsername,username);
        }
        if(!StringUtils.isEmpty(name)){
            wrapper.like(Admin::getUsername,name);
        }
        Page<Admin> page1 = baseMapper.selectPage(page, wrapper);
        return page1;
    }
}
