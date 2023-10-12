package com.my.ssyx.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.my.ssyx.common.auth.AuthContextHolder;
import com.my.ssyx.common.constant.RedisConst;
import com.my.ssyx.common.result.Result;
import com.my.ssyx.common.utils.JwtHelper;
import com.my.ssyx.enums.UserType;
import com.my.ssyx.model.user.User;
import com.my.ssyx.user.service.UserService;
import com.my.ssyx.user.utils.ConstantPropertiesUtil;
import com.my.ssyx.user.utils.HttpClientUtils;
import com.my.ssyx.vo.user.LeaderAddressVo;
import com.my.ssyx.vo.user.UserLoginVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user/weixin")
public class WeixinApiController {
    @Autowired
    UserService userService;

    @Autowired
    RedisTemplate redisTemplate;

    //用户微信授权登录
    @ApiOperation(value = "微信登录获取openid(小程序)")
    @GetMapping("/wxLogin/{code}")
    public Result loginWx(@PathVariable String code) throws Exception {
        //1.得到微信返回code临时票据值
        String wxOpenAppId = ConstantPropertiesUtil.WX_OPEN_APP_ID;
        String wxOpenAppSecret = ConstantPropertiesUtil.WX_OPEN_APP_SECRET;
        //2.拿着code+小程序id+小程序密钥 请求微信接口服务
        //使用HTTPClient工具请求
        StringBuffer url = new StringBuffer().append("https://api.weixin.qq.com/sns/jscode2session")
                .append("?appid=%s")
                .append("&secret=%s")
                .append("&js_code=%s")
                .append("&grant_type=authorization_code");
        String tokenUrl = String.format(url.toString(), wxOpenAppId, wxOpenAppSecret, code);
        //发生get请求
        String result = null;
        result = HttpClientUtils.get(tokenUrl);
        //3.请求微信接口服务，返回两个值 session_key 和openId
        //OpenId是微信的唯一标识
        JSONObject jsonObject = JSONObject.parseObject(result);
        String session_key = jsonObject.getString("session_key");
        String openId = jsonObject.getString("openid");

        //4.添加微信信息到数据库里
        // 操作user表
        //判断是否第一次使用微信授权登录:  如何判断？ openId进行判断
        User user = userService.getUserByOpenId(openId);
        if (user == null) {
            user = new User();
            user.setOpenId(openId);
            user.setNickName(openId);
            user.setPhotoUrl("");
            user.setUserType(UserType.USER);
            user.setIsNew(0);
            userService.save(user);
        }
        //5.根据userId查询提货点和团长信息
        //提货点  user表 user_delivery表
        //团长    leader表
        LeaderAddressVo leaderAddressVo =
                userService.getLeaderAddressByUserId(user.getId());
        //6.使用JWT工具根据userId 和userName生成token字符串
        String token = JwtHelper.createToken(user.getId(), user.getNickName());
        //7.获取当前登录用户信息 放到Redis里面 设置有效时间
        UserLoginVo userLoginVo = userService.getUserLoginVo(user.getId());
        redisTemplate.opsForValue().set(RedisConst.USER_LOGIN_KEY_PREFIX + user.getId(),
                                            userLoginVo,
                                            RedisConst.USERKEY_TIMEOUT,
                                            TimeUnit.DAYS);
        //8.需要数据封装到map返回
        Map<String, Object> map =new HashMap<>();
        map.put("user",user);
        map.put("token",token);
        System.out.println("BeforeToken:"+token);
        map.put("leaderAddressVo",leaderAddressVo);
        return Result.ok(map);
    }

    @PostMapping("/auth/updateUser")
    @ApiOperation(value = "更新用户昵称与头像")
    public Result updateUser(@RequestBody User user) {
        //获取当前登录用户的Id
        User user1 = userService.getById(AuthContextHolder.getUserId());
//        System.out.println(AuthContextHolder.getUserId());
        //把昵称更新为微信用户
        user1.setNickName(user.getNickName().replaceAll("[ue000-uefff]", "*"));
        user1.setPhotoUrl(user.getPhotoUrl());
        userService.updateById(user1);
        return Result.ok(null);
    }
}
