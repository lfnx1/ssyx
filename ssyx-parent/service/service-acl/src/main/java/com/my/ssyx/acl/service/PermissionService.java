package com.my.ssyx.acl.service;

import com.my.ssyx.model.acl.Permission;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface PermissionService extends IService<Permission> {
    List<Permission> queryAllPermission();

    void removeChildById(Long id);
    void savePermissionRole(Long roleId, Long[] PermissionIds);

    Map<String, Object> getPermissionByRoleId(Long roleId);
}
