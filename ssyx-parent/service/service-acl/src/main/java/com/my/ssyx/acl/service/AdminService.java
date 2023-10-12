package com.my.ssyx.acl.service;

import com.my.ssyx.model.acl.Admin;
import com.my.ssyx.vo.acl.AdminQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AdminService extends IService<Admin> {
    IPage<Admin> selectPageUser(Page<Admin> page, AdminQueryVo adminQueryVo);
}
