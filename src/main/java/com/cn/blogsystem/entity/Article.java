package com.cn.blogsystem.entity;


import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Article {
    private Long id;
    private String title;//文章标题
    private String content;//文章内容
    private String summary;//文章摘要
    private Long viewCount;//浏览次数
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private Long userId;//用户id


}
