package com.cn.blogsystem.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleQueryDTO {
    // 页码，默认 1
    private Long current = 1L;

    // 每页条数，默认 10
    private Long size = 10L;

    // 标题（模糊查询）
    private String title;

    // 作者ID
    private Long userId;

    // 开始时间（创建时间 >= ）
    private LocalDateTime startTime;

    // 结束时间（创建时间 <= ）
    private LocalDateTime endTime;
}
