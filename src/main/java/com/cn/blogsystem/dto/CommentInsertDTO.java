package com.cn.blogsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentInsertDTO {
    private Long articleId;
    @NotBlank(message = "评论内容不能为空")
    private String content;
}
