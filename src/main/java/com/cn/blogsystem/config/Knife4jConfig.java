package com.cn.blogsystem.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {
    @Bean
    public OpenAPI openAPI() {
        System.out.println("✅✅✅ Knife4jConfig openAPI Bean 正在加载..."); // 添加这行



        return new OpenAPI()
                .info(new Info()
                        .title("项目接口文档")
                        .version("1.0.0")
                        .description("后端接口文档")
                );
    }

    // 如果你需要指定扫描包，额外加这个 Bean（可选）
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .packagesToScan("com.cn.blogsystem.controller") // 改成你的 Controller 包路径
                .build();
    }

}
