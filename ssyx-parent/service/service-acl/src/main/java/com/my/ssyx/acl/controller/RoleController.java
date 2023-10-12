package com.my.ssyx.acl.controller;

import com.my.ssyx.acl.service.impl.RoleServiceImpl;
import com.my.ssyx.common.result.Result;
import com.my.ssyx.model.acl.Role;
import com.my.ssyx.vo.acl.RoleQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "角色接口")
@RestController
@RequestMapping("/admin/acl/role")
public class RoleController {
    @Resource
    private RoleServiceImpl roleService;

    //1. 角色列表(条件分页查询)
    @ApiOperation("角色条件分页查询")
    @GetMapping("{current}/{limit}")
    public Result pageList(@PathVariable Long current,
                           @PathVariable Long limit,
                           RoleQueryVo roleQueryVo){
        //1.创建page对象，传递当前页和每页记录
        Page<Role>  pageParam=new Page<>(current,limit);
        //2.调用service方法实现分页查询 ,返回分页对象
        IPage<Role> pageModel= roleService.selectRolePage(pageParam,roleQueryVo);
        return  Result.ok(pageModel);

    }

    //2. 根据id查询角色
    @ApiOperation("根据id查询角色")
    @GetMapping("get/{id}")
    public Result get(@PathVariable Long id){
        Role role = roleService.getById(id);
        return Result.ok(role);
    }
    //3.添加角色
    @ApiOperation("添加角色")
    @PostMapping("save")
    //@RequestBody 接收json格式数据将其封装到对象当中
    public Result save(@RequestBody Role role){
        boolean is_success = roleService.save(role);
        if(is_success){
            return Result.ok(null);
        }else {
            return Result.fail(null);
        }
    }
    //4.修改角色
    @ApiOperation("修改角色")
    @PutMapping("update")
    public Result update(@RequestBody Role role){
        boolean b = roleService.updateById(role);
        if(b){
            return Result.ok(null);
        }else {
            return Result.fail(null);
        }
    }
    //5.根据id删除角色
    @ApiOperation("删除角色")
    @DeleteMapping("remove/{id}")
    public  Result remove(@PathVariable Long id){
        boolean b = roleService.removeById(id);
        if(b){
            return Result.ok(null);
        }else {
            return Result.fail(null);
        }
    }
    //6.批量删除角色
    //json 数组对应java 中的List集合
    @ApiOperation("批量删除角色")
    @DeleteMapping("batchRemove")
    public  Result batchRemove(@RequestBody List<Long> idList){
        boolean b = roleService.removeByIds(idList);
        return Result.ok(null);
    }
}
