package com.cn.blogsystem.service;

import com.cn.blogsystem.dto.RegisterDTO;
import com.cn.blogsystem.dto.UpdateProfileDTO;
import com.cn.blogsystem.entity.User;
import org.springframework.stereotype.Service;


public interface UserService {
    /**
     * 用户注册
     * @param registerDTO 注册信息
     * @return 注册成功返回 true，失败返回 false 或抛异常
     */
    boolean register(RegisterDTO registerDTO);


    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 查询到的用户对象
     */
    User getByUsername(String username);


    boolean updateProfile(Long userId, UpdateProfileDTO dto);

}
