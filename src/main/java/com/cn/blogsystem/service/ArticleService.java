package com.cn.blogsystem.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cn.blogsystem.dto.ArticleInsertDTO;
import com.cn.blogsystem.dto.ArticleQueryDTO;
import com.cn.blogsystem.dto.ArticleUpdateDTO;
import com.cn.blogsystem.entity.Article;
import com.cn.blogsystem.vo.ArticleDetailVO;
import com.cn.blogsystem.vo.ArticleListVO;

import java.util.List;

public interface ArticleService {

    //列表查询（分页 + 筛选）：支持按标题模糊查询,用户ID精准查询,
    IPage<ArticleListVO> pageList(ArticleQueryDTO articleDTO);


    //详情查询：按ID查询
    ArticleDetailVO getDetailById(Long id);

    //新增
    boolean add(ArticleInsertDTO article);

    //修改
    boolean update(ArticleUpdateDTO article);

    //删除
    boolean deleteById(Long id);

    //批量删除
    boolean deleteBatch(Long[] ids);

    //获取热门文章
    List<ArticleListVO> getHotArticles();

    //限流
    boolean checkRateLimit(String api);





}
