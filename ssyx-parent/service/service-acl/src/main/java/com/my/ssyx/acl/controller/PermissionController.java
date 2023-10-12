package com.my.ssyx.acl.controller;

import com.my.ssyx.acl.service.PermissionService;
import com.my.ssyx.acl.service.RoleService;
import com.my.ssyx.common.result.Result;
import com.my.ssyx.model.acl.Permission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/acl/permission")
@Api(tags = "菜单管理") //跨域
public class PermissionController {
    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RoleService roleService;

    @ApiOperation("获取所有权限")
    @GetMapping("toAssign/{roleId}")
    public Result toAssign(@PathVariable Long roleId){
         Map<String,Object> map =permissionService.getPermissionByRoleId(roleId);
        return Result.ok(map);
    }

    @ApiOperation("分配菜单权限")
    @PostMapping("doAssign")
    public Result doAssign(@RequestParam Long roleId,
                           @RequestParam Long[] permissionId){
        permissionService.savePermissionRole(roleId,permissionId);
        return Result.ok(null);
    }

    //查询所有的菜单
    @ApiOperation("查询所有菜单")
    @GetMapping
    public Result list(){
        List<Permission> list = permissionService.queryAllPermission();
        return  Result.ok(list);
    }
    //添加菜单
    @ApiOperation("添加菜单")
    @PostMapping("save")
    public Result save(@RequestBody Permission permission){
        boolean save = permissionService.save(permission);
        return Result.ok(null);
    }
    //修改菜单
    @ApiOperation("修改菜单")
    @PutMapping("update")
    public Result update(@RequestBody Permission permission){
        boolean b = permissionService.updateById(permission);
        return  Result.ok(null);
    }
    //递归删除菜单
    @ApiOperation("删除菜单")
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id){
        permissionService.removeChildById(id);
        return Result.ok(null);
    }
}
