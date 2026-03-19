package com.cn.blogsystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cn.blogsystem.common.Result;
import com.cn.blogsystem.dto.ArticleInsertDTO;
import com.cn.blogsystem.dto.ArticleQueryDTO;
import com.cn.blogsystem.dto.ArticleUpdateDTO;
import com.cn.blogsystem.entity.Article;
import com.cn.blogsystem.service.ArticleService;
import com.cn.blogsystem.vo.ArticleDetailVO;
import com.cn.blogsystem.vo.ArticleListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/article")
@Tag(name = "文章管理",description = "文章相关接口")
public class ArticleController {
    @Autowired
    private ArticleService articleService;

    //列表查询（分页 + 条件筛选）：支持按标题、内容、摘要模糊查询
    @Operation(summary = "列表条件构造，分页查询",description = "根据标题模糊查询,根据用户id精准查询")
    @GetMapping("/list")
    public Result<IPage> list(@RequestBody ArticleQueryDTO  articleDTO) {
        IPage<ArticleListVO> page = articleService.pageList(articleDTO);
        return Result.success(page);
    }

    //文章详情
    // ArticleController.java
    @Operation(summary = "文章详情", description = "根据 id 查询文章详情")
    @GetMapping("/{id}")
    public Result<ArticleDetailVO> getById(@PathVariable Long id) {
        ArticleDetailVO articleVO = articleService.getDetailById(id);
        if (articleVO == null) {
            return Result.error("文章不存在");
        }
        return Result.success(articleVO);
    }




    //添加文章
    @Operation(summary = "添加文章",description = "添加文章")
    @PostMapping("/add")
    public Result<String> add(@RequestBody @Valid ArticleInsertDTO article) {
        boolean flog = articleService.add(article);
        if (flog == false) {
            return Result.error("添加失败");
        }
        return Result.success("添加成功");
    }


    //修改文章
    @Operation(summary = "修改文章",description = "修改文章")
    @PutMapping("/update")
    public Result<String> update(@RequestBody ArticleUpdateDTO articleDTO) {
        boolean flog = articleService.update(articleDTO);
        if (flog == false) {
            return Result.error("修改失败");
        }
        return Result.success("修改成功");
    }


    //删除单个文章
    @Operation(summary = "删除文章", description = "根据 id 删除文章，需校验权限")
    @DeleteMapping("/{id}")
    public Result<String> deleteById(@PathVariable Long id) {
        boolean success = articleService.deleteById(id);
        if (!success) {
            return Result.error("删除失败，可能无权操作或文章不存在");
        }
        return Result.success("删除成功");
    }

    //批量删除
    @Operation(summary = "批量删除文章",description = "批量删除文章")
    @DeleteMapping("/deleteBatch")
    public Result<String> deleteBatch(@RequestParam Long[] ids) {
        boolean flog = articleService.deleteBatch(ids);
        if (flog == false) {
            return Result.error("批量删除失败");
        }
        return Result.success("批量删除成功");
    }











}
