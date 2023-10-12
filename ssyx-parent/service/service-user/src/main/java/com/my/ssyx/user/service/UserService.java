package com.my.ssyx.user.service;

import com.my.ssyx.model.user.User;
import com.my.ssyx.vo.user.LeaderAddressVo;
import com.my.ssyx.vo.user.UserLoginVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {
    User getUserByOpenId(String openId);

    LeaderAddressVo getLeaderAddressByUserId(Long id);

    UserLoginVo getUserLoginVo(Long id);
}
