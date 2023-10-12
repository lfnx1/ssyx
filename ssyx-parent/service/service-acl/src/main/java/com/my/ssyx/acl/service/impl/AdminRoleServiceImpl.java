package com.my.ssyx.acl.service.impl;

import com.my.ssyx.acl.mapper.AdminRoleMapper;
import com.my.ssyx.acl.service.AdminRoleService;
import com.my.ssyx.model.acl.AdminRole;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class AdminRoleServiceImpl extends ServiceImpl<AdminRoleMapper, AdminRole> implements AdminRoleService {
}
