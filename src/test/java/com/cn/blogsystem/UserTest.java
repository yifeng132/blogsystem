package com.cn.blogsystem;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cn.blogsystem.entity.User;
import com.cn.blogsystem.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest(properties = "debug=true")
public class UserTest {
    @Autowired
    private UserMapper userMapper;

    @Test
    public void selectByIdTest() {
        System.out.println(userMapper.selectById(1));
    }

    @Test
    public void insetTest() {
        User user = new User(null, "admin7", "123456", "123456@qq.com", "");
        int insert = userMapper.insert(user);
        System.out.println(insert);
        System.out.println("id:"+user.getId());
    }

    @Test
    public void updateByIdTest() {
        User user = new User();
        user.setId(8L);
        user.setUsername("百战不殆");
        int update = userMapper.updateById(user);
        System.out.println(update);
    }


    // 批量删除
    @Test
    public void deleteByIdTest() {
        List<Long> ids = new ArrayList<>();
        userMapper.deleteBatchIds(ids);
    }

    //根据字段删除
    @Test
    public void deleteByMapTest() {
        Map<String, Object> map = new HashMap<>();
        map.put("username", "admin1");
        userMapper.deleteByMap( map);
    }

    //条件构造器
    //lt <, gt >, le <=, ge >= , eq =
    //like 包含

    @Test
    public void QueryWrapperTest() {
        // 查询id>5并且id<10的用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.lt("id",10).gt("id",5);
        List<User> users = userMapper.selectList(queryWrapper);
        users.forEach(System.out::println);
    }

    //分页查询
    @Test
    public void selectPageTest() {
        // 1.创建QueryWrapper对象
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 2.设置分页参数
        int pageNum = 1;
        int pageSize = 5;
        // 3.分页查询
        Page<User> page = new Page<>(pageNum,pageSize);
        Page<User> userPage = userMapper.selectPage(page, queryWrapper);
        // 4.获取分页数据
        List<User> records = userPage.getRecords();
        System.out.println("结果集:"+ records);
    }

    @Test
    public void alldelete(){
        userMapper.delete(null);
    }



}
