package com.cn.blogsystem.service.serviceImpl;

import com.cn.blogsystem.common.BusinessException;
import com.cn.blogsystem.dto.CommentInsertDTO;
import com.cn.blogsystem.entity.Article;
import com.cn.blogsystem.entity.Comment;
import com.cn.blogsystem.mapper.ArticleMapper;
import com.cn.blogsystem.mapper.CommentMapper;
import com.cn.blogsystem.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ArticleMapper articleMapper;


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
