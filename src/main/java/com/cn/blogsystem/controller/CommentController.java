package com.cn.blogsystem.controller;

import com.cn.blogsystem.common.BusinessException;
import com.cn.blogsystem.common.Result;
import com.cn.blogsystem.dto.CommentInsertDTO;
import com.cn.blogsystem.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "评论接口", description = "评论相关接口")
public class CommentController {

    @Autowired
    private CommentService commentService;



    //添加评论
    @Operation(summary = "添加评论", description = "添加评论")
    @PostMapping // 建议显式加上 @PostMapping("/comments") 等路径
    public Result<String> add(@RequestBody CommentInsertDTO comment) {
        // 直接调用，如果 Service 层抛出 BusinessException，会被全局异常捕获并返回特定错误信息
        // 不需要手动判断 boolean 返回值，除非你的业务逻辑特殊要求静默失败
        try {
            commentService.add(comment);
            return Result.success("添加成功");
        } catch (BusinessException e) {
            // 如果不想依赖全局异常处理，也可以在这里手动捕获返回
            return Result.error(e.getMessage());
        }
    }





}
