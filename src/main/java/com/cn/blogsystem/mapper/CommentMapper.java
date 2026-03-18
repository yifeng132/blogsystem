package com.cn.blogsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cn.blogsystem.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
