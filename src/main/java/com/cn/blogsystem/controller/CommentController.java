package com.cn.blogsystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cn.blogsystem.common.Result;
import com.cn.blogsystem.dto.CommentInsertDTO;
import com.cn.blogsystem.dto.CommentQueryDTO;
import com.cn.blogsystem.dto.CommentUpdateDTO;
import com.cn.blogsystem.service.CommentService;
import com.cn.blogsystem.vo.CommentSelectVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "评论接口", description = "评论相关接口")
@RequestMapping("/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    //根据文章 ID 查询所有评论
    @Operation(summary = "根据文章ID查询所有评论", description = "根据文章ID查询所有评论")
    @GetMapping("/list")
    public Result<IPage> list(@RequestBody CommentQueryDTO commentDTO) {
        IPage<CommentSelectVO> page = commentService.listByArticleId(commentDTO);
        return Result.success(page);
    }


    //添加评论
    @Operation(summary = "添加评论", description = "添加评论")
    @PostMapping("/add")// 建议显式加上 @PostMapping("/comments") 等路径
    public Result<String> add(@RequestBody CommentInsertDTO comment) {
            commentService.add(comment);
            return Result.success("添加成功");
    }



    @Operation(summary = "删除评论", description = "删除评论")
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable("id") Long id) {
            commentService.deleteById(id);
            return Result.success("删除成功");
    }


    @Operation(summary = "修改评论", description = "修改评论")
    @PutMapping("/update")
    public Result<String> update(@RequestBody CommentUpdateDTO comment) {
            commentService.update(comment);
            return Result.success("修改成功");
    }


    //批量删除
    @Operation(summary = "批量删除评论", description = "批量删除评论")
    @DeleteMapping("/deleteBatch")
    public Result<String> deleteBatch(@RequestParam Long[] ids) {
            commentService.deleteBatch(ids);
            return Result.success("批量删除成功");
    }


}
