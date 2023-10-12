package com.my.ssyx.user.service.Impl;

import com.my.ssyx.model.user.Leader;
import com.my.ssyx.model.user.User;
import com.my.ssyx.model.user.UserDelivery;
import com.my.ssyx.user.mapper.LeaderMapper;
import com.my.ssyx.user.mapper.UserDeliverMapper;
import com.my.ssyx.user.mapper.UserMapper;
import com.my.ssyx.user.service.UserService;
import com.my.ssyx.vo.user.LeaderAddressVo;
import com.my.ssyx.vo.user.UserLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper,User> implements UserService {

    @Autowired
    private UserDeliverMapper userDeliverMapper;
    @Autowired
    private LeaderMapper leaderMapper;
    @Override
    public User getUserByOpenId(String openId) {
        User user = baseMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getOpenId, openId));
        return user;
    }

    @Override
    public LeaderAddressVo getLeaderAddressByUserId(Long id) {
        //根据userId查询用户默认的团长id
        UserDelivery userDelivery = userDeliverMapper.selectOne(new LambdaQueryWrapper<UserDelivery>()
                .eq(UserDelivery::getUserId, id)
                .eq(UserDelivery::getIsDefault,1));
        if (userDelivery==null){
            return null;
        }
        //拿着团长id查询leader表  获取其他信息
        Leader leader = leaderMapper.selectById(userDelivery.getLeaderId());
        //封装数据到LeaderAddressVo
        LeaderAddressVo leaderAddressVo =new LeaderAddressVo();
        BeanUtils.copyProperties(leader,leaderAddressVo);
        leaderAddressVo.setUserId(id);
        leaderAddressVo.setLeaderId(leader.getId());
        leaderAddressVo.setLeaderName(leader.getName());
        leaderAddressVo.setLeaderPhone(leader.getPhone());
        leaderAddressVo.setWareId(userDelivery.getWareId());
        leaderAddressVo.setStorePath(leader.getStorePath());
        return leaderAddressVo;
    }

    @Override
    public UserLoginVo getUserLoginVo(Long id) {
        User user = baseMapper.selectById(id);
        UserLoginVo userLoginVo=new UserLoginVo();
        BeanUtils.copyProperties(user,userLoginVo);
        userLoginVo.setUserId(id);
        UserDelivery userDelivery = userDeliverMapper.selectOne(new LambdaQueryWrapper<UserDelivery>()
                .eq(UserDelivery::getUserId, id)
                .eq(UserDelivery::getIsDefault,1));
        if (userDelivery!=null){
            userLoginVo.setLeaderId(userDelivery.getLeaderId());
            userLoginVo.setWareId(userDelivery.getWareId());
        }else {
            userLoginVo.setLeaderId(1L);
            userLoginVo.setWareId(1L);
        }
        return userLoginVo;
    }
}
