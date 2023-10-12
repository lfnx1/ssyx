package com.my.ssyx.acl.service.impl;

import com.my.ssyx.acl.mapper.RoleMapper;
import com.my.ssyx.acl.service.AdminRoleService;
import com.my.ssyx.acl.service.PermissionService;
import com.my.ssyx.acl.service.RoleService;
import com.my.ssyx.model.acl.AdminRole;
import com.my.ssyx.model.acl.Role;
import com.my.ssyx.vo.acl.RoleQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {


    @Autowired
    AdminRoleService adminRoleService;
    @Autowired
    PermissionService permissionService;



    @Override
    public IPage<Role> selectRolePage(Page<Role> pageParam, RoleQueryVo roleQueryVo) {
        //获取条件值
        String roleName = roleQueryVo.getRoleName();
        //创建mp条件封装对象
        LambdaQueryWrapper<Role> wrapper =new LambdaQueryWrapper<>();
        //判断条件值是否为空
        if(!StringUtils.isEmpty(roleName)){
            wrapper.like(Role::getRoleName,roleName);
        }
        //不为空封装查询条件
        IPage<Role> rolePage = baseMapper.selectPage(pageParam,wrapper);
        //返回分页对象
        return  rolePage;
    }



    @Override
    public Map<String, Object> getRoleByAdminId(Long adminId) {
        //1.查询所有的角色
        List<Role> allRoleList = baseMapper.selectList(null);
        //2. 查询用户已经分配的角色
        //2.1查询所有的角色列表编号
       LambdaQueryWrapper<AdminRole> wrapper =new LambdaQueryWrapper<>();
       //设置查询条件
       wrapper.eq(AdminRole::getAdminId,adminId);
       List<AdminRole> adminRoleList = adminRoleService.list(wrapper);
        //2.2查询用户对应的角色列表编号
        List<Long> roleIdsList = adminRoleList.stream().
                map(item -> item.getRoleId())
                .collect(Collectors.toList());
        //2.3遍历用户角色列表编号  根据id将其存入一个新的集合当中
        List<Role> assignRoleList =new ArrayList<>();
        for (Role role:allRoleList){
            if(roleIdsList.contains(role.getId())){
                assignRoleList.add(role);
            }
        }
        Map<String,Object> result =new HashMap<>();
        //所有角色
        result.put("allRolesList",allRoleList);
        //用户已经分配的角色列表
        result.put("assignRoles",assignRoleList);
        return result;
    }


    @Override
    public void saveAdminRole(Long adminId, Long[] roleIds) {
        //1 删除用户已经分配的角色数据 操作中间表
        LambdaQueryWrapper<AdminRole> wrapper =new LambdaQueryWrapper<>();
        wrapper.eq(AdminRole::getAdminId,adminId);
        adminRoleService.remove(wrapper);
        //2.重新分配
        //多次使用会产生 save资源浪费
//        for (Long roleId:roleIds){
//            AdminRole adminRole =new AdminRole();
//            adminRole.setAdminId(adminId);
//            adminRole.setRoleId(roleId);
//            adminRoleService.save(adminRole);
//        }
        List<AdminRole> list =new ArrayList<>();
        for (Long roleId:roleIds){
            AdminRole adminRole =new AdminRole();
            adminRole.setAdminId(adminId);
            adminRole.setRoleId(roleId);
            //放入list集合
            list.add(adminRole);
        }
        //调用方法添加
        adminRoleService.saveBatch(list);
    }
}
