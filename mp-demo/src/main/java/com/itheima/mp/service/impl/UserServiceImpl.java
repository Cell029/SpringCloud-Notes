package com.itheima.mp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.mp.domain.dto.PageDTO;
import com.itheima.mp.domain.po.User;
import com.itheima.mp.domain.po.UserInfo;
import com.itheima.mp.domain.query.UserQuery;
import com.itheima.mp.domain.vo.UserVO;
import com.itheima.mp.mapper.UserMapper;
import com.itheima.mp.service.IUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    @Transactional
    public void deductBalance(Long id, Integer money) {
        // 1. 查询用户
        User user = getById(id);
        // 2. 校验用户状态
        if (user == null || user.getStatus().getValue() == 2) {
            throw new RuntimeException("用户状态异常！");
        }
        // 3. 校验余额是否充足
        if (user.getBalance() < money) {
            throw new RuntimeException("用户余额不足！");
        }
        // 4. 扣减余额 update tb_user set balance = balance - ?
        int remainBalance = user.getBalance() - money;
        this.lambdaUpdate()
                .set(User::getBalance, remainBalance) // 更新余额
                .set(remainBalance == 0, User::getStatus, 2) // 动态判断，是否更新 status
                .eq(User::getId, id)
                .eq(User::getBalance, user.getBalance()) // 乐观锁
                .update();
    }

    @Override
    public PageDTO<UserVO> queryUsersPage(UserQuery query) {
        // 1. 构建条件分页条件
        Page<User> page = query.toMpPageDefaultSortByUpdateTimeDesc();
        // 2. 分页查询
        Page<User> records = lambdaQuery()
                .like(query.getName() != null, User::getUsername, query.getName())
                .eq(query.getStatus() != null, User::getStatus, query.getStatus())
                .page(page);
        // 3. 封装为 VO
        // return PageDTO.of(records, UserVO.class);
        // 或者封装自定义的 VO
        // return PageDTO.of(records, user -> BeanUtil.copyProperties(user, UserVO.class));
        return PageDTO.of(records, user -> {
            // 1. 拷贝基础属性
            UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
            // 2. 处理特殊逻辑
            userVO.setUsername(userVO.getUsername().substring(0, userVO.getUsername().length() - 2) + "**");
            return userVO;
        });
    }
}
