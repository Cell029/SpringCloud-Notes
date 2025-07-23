package com.itheima.mp.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.itheima.mp.domain.po.Address;
import com.itheima.mp.domain.po.User;
import com.itheima.mp.domain.po.UserInfo;
import com.itheima.mp.service.IAddressService;
import com.itheima.mp.service.IUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private IUserService userService;
    @Autowired
    private IAddressService addressService;

    @Test
    void testInsert() {
        User user = new User();
        // user.setId(5L);
        user.setUsername("Mike");
        user.setPassword("123");
        user.setPhone("18688990011");
        user.setBalance(200);
        user.setInfo(new UserInfo(23, "体育老师", "male"));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        // userMapper.saveUser(user);
        userMapper.insert(user);
    }

    @Test
    void testSelectById() {
        // User user = userMapper.queryUserById(5L);
        User user = userMapper.selectById(5L);
        System.out.println("user = " + user);
    }


    @Test
    void testQueryByIds() {
        // List<User> users = userMapper.queryUserByIds(List.of(1L, 2L, 3L, 4L));
        List<User> users = userMapper.selectBatchIds(List.of(1L, 2L, 3L, 4L, 5L));
        users.forEach(System.out::println);
    }

    @Test
    void testUpdateById() {
        User user = new User();
        user.setId(5L);
        user.setBalance(20000);
        // userMapper.updateUser(user);
        userMapper.updateById(user);
    }

    @Test
    void testDeleteUser() {
        // userMapper.deleteUser(5L);
        userMapper.deleteById(5L);
    }

    @Test
    void testQuery() {
        User user = userMapper.queryById(1L);
        System.out.println("user = " + user);
    }

    @Test
    void testQueryWrapper() {
        // 1. 构建查询条件 where name like "%o%" AND balance >= 1000
        QueryWrapper<User> wrapper = new QueryWrapper<User>()
                .select("id", "username", "info", "balance")
                .like("username", "o")
                .ge("balance", 1000);
        // 2. 查询数据
        List<User> users = userMapper.selectList(wrapper);
        users.forEach(System.out::println);
    }

    @Test
    void testUpdateByQueryWrapper() {
        // 1. 构建查询条件 where name = "Jack"
        QueryWrapper<User> wrapper = new QueryWrapper<User>().eq("username", "Jack");
        // 2. 更新数据，user 中非 null 字段都会作为 set 语句
        User user = new User();
        user.setBalance(2000);
        userMapper.update(user, wrapper);
    }

    @Test
    void testUpdateWrapper() {
        List<Long> ids = List.of(1L, 2L, 4L);
        // 1. 生成SQL
        UpdateWrapper<User> wrapper = new UpdateWrapper<User>()
                .setSql("balance = balance - 200") // SET balance = balance - 200
                .in("id", ids); // where id in (1, 2, 4)
        // 2. 更新，注意第一个参数可以给 null，也就是不使用实体类作为更新数据的来源，而是基于 UpdateWrapper 中的 setSQL 来更新内容
        userMapper.update(null, wrapper);
    }

    @Test
    void testLambdaQueryWrapper() {
        // 1. 构建条件 WHERE username LIKE "%o%" AND balance >= 1000
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .select(User::getId, User::getUsername, User::getInfo, User::getBalance)
                .like(User::getUsername, "o")
                .ge(User::getBalance, 1000);
        // 2. 查询
        List<User> users = userMapper.selectList(wrapper);
        users.forEach(System.out::println);
    }

    @Test
    void testCustomWrapper() {
        // 1. 准备自定义查询条件
        List<Long> ids = List.of(1L, 2L, 4L);
        QueryWrapper<User> wrapper = new QueryWrapper<User>().in("id", ids);
        // 2. 调用 mapper 的自定义方法，直接传递 Wrapper
        userMapper.deductBalanceByIds(200, wrapper);
    }

    @Test
    void testCustomJoinWrapper() {
        // 1. 准备自定义查询条件
        QueryWrapper<User> wrapper = new QueryWrapper<User>()
                .in("u.id", List.of(1L, 2L, 4L))
                .eq("a.city", "北京");
        // 2. 调用 mapper 的自定义方法
        List<User> users = userMapper.queryUserByWrapper(wrapper);
        users.forEach(System.out::println);
    }

    @Test
    void testSaveOneByOne() {
        long b = System.currentTimeMillis();
        for (int i = 1; i <= 100000; i++) {
            userService.save(buildUser(i));
        }
        long e = System.currentTimeMillis();
        System.out.println("耗时：" + (e - b));
    }

    @Test
    void testSaveBatch() {
        List<User> list = new ArrayList<>(1000);
        long b = System.currentTimeMillis();
        for (int i = 1; i <= 100000; i++) {
            list.add(buildUser(i));
            // 每 1000 条批量插入一次
            if (i % 1000 == 0) {
                userService.saveBatch(list);
                list.clear();
            }
        }
        long e = System.currentTimeMillis();
        System.out.println("耗时：" + (e - b));
    }

    private User buildUser(int i) {
        User user = new User();
        user.setUsername("user2_" + i);
        user.setPassword("123");
        user.setPhone("" + (18688190000L + i));
        user.setBalance(2000);
        user.setInfo(new UserInfo(24, "英文老师", "female"));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(user.getCreateTime());
        return user;
    }

    @Test
    void testDeleteByLogic() {
        // 删除方法与以前没有区别
        addressService.removeById(59L);
    }

    @Test
    void testQueryByLogic() {
        List<Address> list = addressService.list();
        list.forEach(System.out::println);
    }

    @Test
    void testService() {
        List<User> list = userService.list();
        list.forEach(System.out::println);
    }

    @Test
    void testPageQuery() {
        // 1. 分页查询，new Page() 的两个参数分别是：页码、每页大小
        Page<User> p = userService.page(new Page<>(2, 2));
        // 2. 总条数
        System.out.println("total = " + p.getTotal());
        // 3. 总页数
        System.out.println("pages = " + p.getPages());
        // 4. 数据
        List<User> records = p.getRecords();
        records.forEach(System.out::println);
    }

}