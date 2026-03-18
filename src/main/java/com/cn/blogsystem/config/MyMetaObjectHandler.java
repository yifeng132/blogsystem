package com.cn.blogsystem.config;


import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时的填充策略
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // 参数说明：
        // 1. metaObject: 元对象
        // 2. "createTime": 实体类中的字段名 (必须与 @TableField 中的字段一致)
        // 3. LocalDateTime.class: 字段类型
        // 4. LocalDateTime.now(): 默认值
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 更新时的填充策略 (如果不需要自动更新，留空即可)
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 如果以后需要自动填充 updateTime，可以在这里写
        // this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}

