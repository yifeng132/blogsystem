package com.cn.blogsystem.vo;


import lombok.Data;

@Data
public class ArticleDetailVO {
    private Long id;
    private String title;//文章标题
    private String content;//文章内容
    private String summary;//文章摘要
    private Long viewCount;//浏览次数
    private String createTime;
    private Long userId;//用户id
}
