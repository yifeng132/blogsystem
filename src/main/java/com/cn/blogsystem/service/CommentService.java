package com.cn.blogsystem.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cn.blogsystem.dto.CommentInsertDTO;
import com.cn.blogsystem.dto.CommentQueryDTO;
import com.cn.blogsystem.dto.CommentUpdateDTO;
import com.cn.blogsystem.entity.Comment;
import com.cn.blogsystem.vo.CommentSelectVO;

import java.util.List;

public interface CommentService {
    void add(CommentInsertDTO comment);

    void deleteById(Long id);

    void deleteBatch(Long[] ids);

    void update(CommentUpdateDTO comment);

    IPage<CommentSelectVO> listByArticleId(CommentQueryDTO commentDTO);





}
