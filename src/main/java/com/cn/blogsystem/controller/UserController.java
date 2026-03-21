package com.cn.blogsystem.controller;


import com.cn.blogsystem.common.Result;
import com.cn.blogsystem.common.JwtUtil;
import com.cn.blogsystem.dto.LoginDTO;
import com.cn.blogsystem.dto.RegisterDTO;
import com.cn.blogsystem.dto.UpdateProfileDTO;
import com.cn.blogsystem.entity.User;
import com.cn.blogsystem.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@RestController
@Tag(name = "用户接口", description = "用户相关接口")
public class UserController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "根据用户名和密码登录，返回 Token")
    public Result<String> login(@RequestBody @Valid LoginDTO loginDTO) {
        try {
            // 1. 构建未认证的 Token 对象
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword());
            // 2. 执行认证 (核心步骤)
            // 这里会触发 Spring Security 流程：
            // -> 调用 DBUserDetailsService.loadUserByUsername 查库
            // -> 自动使用 BCrypt 比对密码
            // -> 成功则返回完整的 Authentication 对象，失败则抛异常
            Authentication authentication = authenticationManager.authenticate(authToken);
            // 注意：此时密码已验证通过，用户一定存在
            User user = userService.getByUsername(loginDTO.getUsername());
            // 4. 提取用户 ID
            String userIdStr = user.getId().toString();
            // 3. 认证成功，生成 JWT Token
            // 假设 JwtUtil 中有 createToken 方法，传入用户名
            String token = jwtUtil.generateToken(userIdStr);
            return Result.success(token);
        } catch (Exception e) {
            // 5. 认证失败处理 (用户名不存在或密码错误)
            e.printStackTrace();
            return Result.error(401, "用户名或密码错误");
        }
    }


    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "根据用户名和密码注册，返回 Token")
    public Result<String> register(@RequestBody @Valid RegisterDTO registerDTO) {

        try {
            boolean success = userService.register(registerDTO);
            if (success == true) {
                return Result.success("注册成功");
            } else {
                return Result.error("注册失败");
            }
        } catch (RuntimeException e) {
            // 捕获业务异常（如用户名已存在）
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("系统繁忙，注册失败");
        }

    }

    @PostMapping("/profile/update")
    @Operation(summary = "更新个人资料", description = "登录后修改昵称、邮箱、头像")
    public Result<String> updateProfile(@RequestBody @Valid UpdateProfileDTO dto) {
        // 1. 从 Security 上下文获取当前认证对象
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Result.error("用户未登录");
        }

        // 2. 获取用户名 (这是过滤器里放入的 principal)
        String username = authentication.getName();

        // 3. (可选) 如果业务逻辑需要 userId，可以通过用户名查库，或者在过滤器中自定义 UserDetails 携带 ID
        // 这里假设你有一个 UserService 可以根据 username 查询 user 对象
        User currentUser = userService.getByUsername(username);
        if (currentUser == null) {
            return Result.error("用户不存在");
        }

        Long userId = currentUser.getId();

        // 4. 执行更新逻辑
        userService.updateProfile(userId, dto);

        return Result.success("修改成功");
    }


    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "将当前 Token 加入黑名单，使其立即失效")
    public Result<String> logout() {
        // 2. 从 RequestContextHolder 中获取请求对象 (Spring 推荐做法，避免直接作为参数传递)

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String token = (String) authentication.getDetails();
        try {
            // 4. 计算 Token 剩余有效期
            long remainingTime = jwtUtil.getRemainingExpiration(token);
            // 5. 如果 Token 还没过期，才加入黑名单
            if (remainingTime > 0) {
                jwtUtil.addTokenToBlacklist(token, remainingTime);
            }
            return Result.success("登出成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("登出失败：" + e.getMessage());
        }
    }


















}


