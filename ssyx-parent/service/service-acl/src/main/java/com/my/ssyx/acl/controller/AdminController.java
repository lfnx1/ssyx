package com.my.ssyx.acl.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.my.ssyx.acl.service.AdminService;
import com.my.ssyx.acl.service.RoleService;
import com.my.ssyx.common.result.Result;
import com.my.ssyx.common.utils.MD5;
import com.my.ssyx.model.acl.Admin;
import com.my.ssyx.vo.acl.AdminQueryVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "用户接口")
@RestController
@RequestMapping("/admin/acl/user")
public class AdminController {
    @Autowired
    private AdminService adminService;
    @Autowired
    private RoleService roleService;

    //获取所有角色 根据用户id查询用户分配角色列表
    @ApiOperation("获取用户角色列表")
    @GetMapping("toAssign/{adminId}")
    public Result toAssign(@PathVariable Long adminId){
        //返回map集合包括两部份  所有角色和为用户分配角色列表
        Map<String,Object> map = roleService.getRoleByAdminId(adminId);
        return  Result.ok(map);
    }
    //参数 ： 用户
    @ApiOperation(("为用户进行角色分配"))
    @PostMapping("doAssign")
    public  Result doAssign(@RequestParam Long adminId,
                            @RequestParam Long[] roleId){
            roleService.saveAdminRole(adminId,roleId);
            return Result.ok(null);
    }

    //1.用户列表
    @ApiOperation("用户列表")
    @GetMapping("{current}/{limit}")
    public Result list(@PathVariable Long current, @PathVariable Long limit, AdminQueryVo adminQueryVo){
        Page<Admin> page =new Page<Admin>(current,limit);
        IPage<Admin> pageModel = adminService.selectPageUser(page,adminQueryVo);
        return  Result.ok(pageModel);
    }
    //2. id查询用户
    @ApiOperation("根据id查询")
    @GetMapping("get/{id}")
    public Result getUser(@PathVariable Long id){
        Admin admin = adminService.getById(id);
        return Result.ok(admin);
    }

    //3. 添加用户
    @ApiOperation("添加用户")
    @PostMapping("save")
    public Result save(@RequestBody Admin admin){
        //获取输入的密码
        String password = admin.getPassword();
        //对密码进行加密 MD5
        String passwordMd5 = MD5.encrypt(password);
        //设置到admin对象里面
        admin.setPassword(passwordMd5);
        //调用方法添加
        boolean b = adminService.save(admin);
        if (b){
            return Result.ok(null);
        }else{
            return Result.fail(null);
        }
    }
    //4. 修改
    @ApiOperation("修改用户")
    @PutMapping("update")
    public Result update(@RequestBody Admin admin){
        boolean b = adminService.updateById(admin);
        if (b){
            return Result.ok(admin);
        }else{
            return Result.fail(null);
        }
    }
    //5  id删除
    @ApiOperation("删除用户")
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id){
        boolean b = adminService.removeById(id);
        if (b){
            return Result.ok(null);
        }else{
            return Result.fail(null);
        }
    }

    //6 批量删除
    @ApiOperation("批量删除")
    @DeleteMapping("batchRemove")
    public Result batchRemove(@RequestBody List<Long> idList){
        boolean b = adminService.removeByIds(idList);
        if (b){
            return Result.ok(null);
        }else{
            return Result.fail(null);
        }
    }


}
