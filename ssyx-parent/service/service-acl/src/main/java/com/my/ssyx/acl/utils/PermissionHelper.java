package com.my.ssyx.acl.utils;

import com.my.ssyx.model.acl.Permission;

import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {
    public static List<Permission> buildPermission(List<Permission> allList){
        List<Permission> trees =new ArrayList<>();
        //遍历所有菜单list集合,得到第一层数据
        for (Permission permission : allList) {
            //pid=0,第一层数据
            if(permission.getPid()==0){
                permission.setLevel(1);
                //调用方法,从第一层开始找
                trees.add(findChildren(permission,allList));
            }
        }
        return trees;
    }
    //递归往下去找
    //permission:上层结点
    //allList 所有菜单
    private static Permission findChildren(Permission permission, List<Permission> allList) {
        permission.setChildren(new ArrayList<Permission>());
        //遍历allList所有菜单数据
        //判断当前结点id=pid是否一样 封装 递归往下寻找
        for (Permission it : allList) {
            if(it.getPid().longValue()==permission.getId().longValue()){
               int level =permission.getLevel()+1;
               if (permission.getChildren()==null){
                   permission.setChildren(new ArrayList<>());
               }
               it.setLevel(level);
               permission.getChildren().add(findChildren(it,allList));
            }
        }
        return permission;
    }
}
