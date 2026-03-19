package com.cn.blogsystem.vo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentSelectVO {
    private Long id;
    private String content;
    private Long articleId;
    private Long userId;
    private LocalDateTime createTime;
}
