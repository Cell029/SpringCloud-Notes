package com.itheima.mp.mapper;

import com.itheima.mp.domain.po.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface UserMapper extends BaseMapper<User> {

    User queryById(Long id);

}
