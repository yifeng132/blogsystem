package com.cn.blogsystem.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cn.blogsystem.common.BusinessException;
import com.cn.blogsystem.dto.CommentInsertDTO;
import com.cn.blogsystem.dto.CommentQueryDTO;
import com.cn.blogsystem.dto.CommentUpdateDTO;
import com.cn.blogsystem.entity.Article;
import com.cn.blogsystem.entity.Comment;
import com.cn.blogsystem.mapper.ArticleMapper;
import com.cn.blogsystem.mapper.CommentMapper;
import com.cn.blogsystem.service.CommentService;
import com.cn.blogsystem.vo.CommentSelectVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ArticleMapper articleMapper;


    //添加
    @Override
    public void add(CommentInsertDTO commentDTO) {
        // 1. 基础校验：内容不能为空
        if (!StringUtils.hasText(commentDTO.getContent())) {
            throw new BusinessException("评论内容不能为空");
        }

        // 2. 校验文章是否存在 (修复逻辑漏洞)
        Article article = articleMapper.selectById(commentDTO.getArticleId());
        if (article == null) {
            throw new BusinessException("文章不存在，无法评论");
        }

        Comment comment = new Comment();
        comment.setArticleId(commentDTO.getArticleId());
        comment.setContent(commentDTO.getContent());

        //设置用户 ID
        Long currentUserId = getCurrentUserId();
        comment.setUserId(currentUserId);

        int insert = commentMapper.insert(comment);

    }

    //删除
    @Override
    public void deleteById(Long id) {
        Long currentUserId = getCurrentUserId();
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (!comment.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权删除该评论");
        }
        int delete = commentMapper.deleteById(id);

    }

    //批量删除
    @Override
    public void deleteBatch(Long[] idss) {
        // 1. 获取当前登录用户 ID
        Long currentUserId = getCurrentUserId();

        //list方便遍历
        List<Long> ids = new ArrayList<>();
        for (Long id : idss) {
            ids.add(id);
        }

        // 2. 校验权限
        List<Comment> comments = this.listByIds(ids);
        if (comments.size() != idss.length) {
            throw new BusinessException("存在无效的评论 ID");
        }


        for (Comment comment : comments) {
            if (!comment.getUserId().equals(currentUserId)){
                throw new BusinessException("无权修改该评论");
            }
        }

        commentMapper.deleteByIds(ids);

    }

    //修改
    @Override
    public void update(CommentUpdateDTO commentDTO) {
        Long currentUserId = getCurrentUserId();
        Comment comment = commentMapper.selectById(commentDTO.getId());
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (!comment.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权修改该评论");
        }
        comment.setContent(commentDTO.getContent());

    }

    @Override
    public IPage<CommentSelectVO> listByArticleId(CommentQueryDTO commentDTO) {
        // 1. 参数校验
        if (commentDTO == null || commentDTO.getArticleId() == null) {
            throw new BusinessException("参数错误：文章 ID 不能为空");
        }

        // 在 listByArticleId 方法开头添加
        Article article = articleMapper.selectById(commentDTO.getArticleId());
        if (article == null) {
            throw new BusinessException("文章不存在，无法查看评论");
        }


        // 确保分页参数安全
        long current = commentDTO.getCurrent() != null ? commentDTO.getCurrent() : 1;
        long size = commentDTO.getSize() != null ? commentDTO.getSize() : 10;


        //分页查询评论
        Page<Comment> page = new Page<>(commentDTO.getCurrent(), commentDTO.getSize());
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getArticleId, commentDTO.getArticleId());
        wrapper.orderByDesc(Comment::getCreateTime);

        Page<Comment> commentPage = this.page(page, wrapper);
        IPage<CommentSelectVO> pageVO = new Page<>();
        BeanUtils.copyProperties(commentPage, pageVO, "records");

        List<CommentSelectVO> voList = commentPage.getRecords().stream().map(comment -> {
            CommentSelectVO vo = new CommentSelectVO();
            BeanUtils.copyProperties(comment, vo);
            return vo;
        }).collect(Collectors.toList());

        pageVO.setRecords(voList);

        return pageVO;
    }


    //获取当前登录用户 ID
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 防御性检查：防止未登录或上下文为空
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException("用户未登录或身份无效");
        }

        String userId = (String) authentication.getPrincipal();
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new BusinessException("用户身份解析失败，非法的用户 ID 格式：" + userId);
        }
    }


}
