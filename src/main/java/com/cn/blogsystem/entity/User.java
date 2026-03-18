package com.cn.blogsystem.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
//@TableName("t_user")
public class User {
    @TableId(value = "id")
    private Long id;
    @TableField("username")
    private String username;
    private String password;
    private String email;
    private String avatar;
}
