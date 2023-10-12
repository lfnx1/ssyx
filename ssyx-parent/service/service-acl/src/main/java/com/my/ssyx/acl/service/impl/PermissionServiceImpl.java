package com.my.ssyx.acl.service.impl;

import com.my.ssyx.acl.mapper.PermissionMapper;
import com.my.ssyx.acl.service.PermissionService;
import com.my.ssyx.acl.service.RolePermissionService;
import com.my.ssyx.acl.utils.PermissionHelper;
import com.my.ssyx.model.acl.Permission;
import com.my.ssyx.model.acl.RolePermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService  {
    @Autowired
    RolePermissionService rolePermissionService;

    @Override
    public List<Permission> queryAllPermission() {
        //查询所有的菜单
        List<Permission> allPermissionList = baseMapper.selectList(null);
        //转换数据格式
        List<Permission> result= PermissionHelper.buildPermission(allPermissionList);
        return result;
    }

    @Override
    public void removeChildById(Long id) {
        //idList有删除所有菜单id
        List<Long> idList =new ArrayList<>();
        //获取所有子菜单id
        //包括子菜单的子菜单也要一起得到
        //重点! 递归获取
        this.getAllPermissionId(id,idList);
        //设置当前菜单id
        idList.add(id);
        //根据多个菜单id删除
        baseMapper.deleteBatchIds(idList);
    }

    //id:当前菜单id
    //idList: 封装最终list集合
    private void getAllPermissionId(Long id, List<Long> idList) {
        //获取一级子菜单
        LambdaQueryWrapper<Permission> wrapper =new LambdaQueryWrapper<>();
        wrapper.eq(Permission::getPid,id);
        List<Permission> childList =baseMapper.selectList(wrapper);
        //递归查询二级子菜单
        childList.stream().forEach(item->{
            //封装菜单id到idList中
            idList.add(item.getId());
            //递归
            this.getAllPermissionId(item.getPid(),idList);
        });
    }


    @Override
    public void savePermissionRole(Long roleId, Long[] PermissionIds) {
        LambdaQueryWrapper<RolePermission> wrapper =new LambdaQueryWrapper<>();
        wrapper.eq(RolePermission::getRoleId,roleId);
        rolePermissionService.remove(wrapper);
        //保存新的关系
        List<RolePermission> list =new ArrayList<>();
        for (Long permissionId : PermissionIds) {
            RolePermission rolePermission =new RolePermission();
            rolePermission.setPermissionId(permissionId);
            rolePermission.setRoleId(roleId);
            list.add(rolePermission);
        }
        rolePermissionService.saveBatch(list);
    }

    @Override
    public Map<String, Object> getPermissionByRoleId(Long roleId) {
        //所有权限
        List<Permission> list = baseMapper.selectList(null);
        //角色有的权限
        LambdaQueryWrapper<RolePermission> wrapper =new LambdaQueryWrapper();
        wrapper.eq(RolePermission::getRoleId,roleId);
        List<RolePermission> RolePermissionList = rolePermissionService.list(wrapper);
        List<Long> PermissionIdsList = RolePermissionList.stream().map(item -> item.getPermissionId()).collect(Collectors.toList());
        List<Permission> assignPermissionList =new ArrayList<>();
        for (Permission permission : list) {
            if (PermissionIdsList.contains(permission.getPid())){
                assignPermissionList.add(permission);
            }
        }
        Map<String,Object> result =new HashMap<>();
        //所有角色
        result.put("allPermissions",list);
        //用户已经分配的角色列表
        result.put("assignPermissions",assignPermissionList);
        return result;
    }

}
