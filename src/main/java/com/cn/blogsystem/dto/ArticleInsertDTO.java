package com.cn.blogsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ArticleInsertDTO {
    @NotBlank(message = "文章标题不能为空")
    private String title;//文章标题
    @NotBlank(message = "文章内容不能为空")
    private String content;//文章内容
    private String summary;//文章摘要
}
