package com.cn.blogsystem.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cn.blogsystem.dto.RegisterDTO;
import com.cn.blogsystem.dto.UpdateProfileDTO;
import com.cn.blogsystem.entity.User;
import com.cn.blogsystem.mapper.UserMapper;
import com.cn.blogsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public boolean register(RegisterDTO registerDTO) {
        // 1. 校验用户名是否已存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, registerDTO.getUsername());
        User existingUser = userMapper.selectOne(queryWrapper);

        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 2. 构建用户实体
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        // 3. 密码加密 (关键步骤，不要存明文)
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));

        // 4. 设置默认状态 (如：正常)
        // user.setStatus(1);

        // 5. 插入数据库
        return userMapper.insert(user) > 0;
    }

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体
     */
    @Override
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(queryWrapper);
        return user;
    }

    /**
     * 修改用户信息
     *
     * @param userId 用户ID
     * @param dto    用户信息
     * @return 是否修改成功
     */
    @Override
    public boolean updateProfile(Long userId, UpdateProfileDTO dto) {
        // 1. 构建更新条件
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getId, userId);

        // 2. 构建更新实体 (只包含需要更新的字段)
        User user = new User();
        boolean needUpdate = false;

        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
            needUpdate = true;
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
            needUpdate = true;
        }
        // 如果有昵称等其他字段，同理判断

        if (!needUpdate) {
            return true; // 没有需要更新的字段，直接视为成功
        }
        // 3. 执行更新
        return userMapper.update(user, wrapper) > 0;
    }

}
