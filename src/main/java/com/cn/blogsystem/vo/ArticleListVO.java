package com.cn.blogsystem.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleListVO {
    private Long id;
    private String title;
    private String summary;
    private LocalDateTime createTime;
    private Integer viewCount;
    private Long userId;
}
