package com.itheima.mp.controller;

import cn.hutool.core.bean.BeanUtil;
import com.itheima.mp.domain.dto.PageDTO;
import com.itheima.mp.domain.dto.UserFormDTO;
import com.itheima.mp.domain.po.User;
import com.itheima.mp.domain.query.UserQuery;
import com.itheima.mp.domain.vo.UserVO;
import com.itheima.mp.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "用户操作相关接口")
public class UserController {
    @Autowired
    private IUserService userService;

    @PostMapping
    @Operation(summary = "新增用户")
    public void saveUser(@RequestBody UserFormDTO userFormDTO){
        // 1. 转换 DTO 为 PO
        User user = BeanUtil.copyProperties(userFormDTO, User.class);
        // 2. 新增
        userService.save(user);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    public void removeUserById(@PathVariable("id") Long userId){
        userService.removeById(userId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据id查询用户")
    public UserVO queryUserById(@PathVariable("id") Long userId){
        // 1. 查询用户
        User user = userService.getById(userId);
        // 2. 处理 vo
        return BeanUtil.copyProperties(user, UserVO.class);
    }

    @GetMapping
    @Operation(summary = "根据id集合查询用户")
    public List<UserVO> queryUserByIds(@RequestParam("ids") List<Long> ids){
        List<User> users = userService.listByIds(ids);
        return BeanUtil.copyToList(users, UserVO.class);
    }

    @PutMapping("{id}/deduction/{money}")
    @Operation(summary = "扣减用户余额")
    public void deductBalance(@PathVariable("id") Long id, @PathVariable("money")Integer money){
        userService.deductBalance(id, money);
    }

    @GetMapping("/list")
    @Operation(summary = "根据id集合查询用户")
    public List<UserVO> queryUsers(UserQuery query){
        // 1. 组织条件
        String username = query.getName();
        Integer status = query.getStatus();
        Integer minBalance = query.getMinBalance();
        Integer maxBalance = query.getMaxBalance();
        // 2. 查询用户
        List<User> users = userService.lambdaQuery()
                .like(username != null, User::getUsername, username)
                .eq(status != null, User::getStatus, status)
                .ge(minBalance != null, User::getBalance, minBalance)
                .le(maxBalance != null, User::getBalance, maxBalance)
                .list();
        // 3. 处理 vo
        return BeanUtil.copyToList(users, UserVO.class);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public PageDTO<UserVO> queryUsersPage(UserQuery query){
        return userService.queryUsersPage(query);
    }


}
