package com.cn.blogsystem.dto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleUpdateDTO {
    private Long id;
    private String title;//文章标题
    private String content;//文章内容
    private String summary;//文章摘要
}
