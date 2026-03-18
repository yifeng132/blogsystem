package com.cn.blogsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cn.blogsystem.entity.User;
import org.apache.ibatis.annotations.Select;


public interface UserMapper extends BaseMapper<User> {

    @Select("select * from t_user where username = #{username}")
    User selectByUsername(String username);
}
