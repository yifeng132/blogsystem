package com.cn.blogsystem.dto;

import lombok.Data;

@Data
public class CommentInsertDTO {
    private Long articleId;
    private String content;
}
