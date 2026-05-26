package com.twotrack.notebook.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.twotrack.notebook.mapper")
public class MybatisPlusConfig {
    // 分页插件在 MyBatis-Plus 3.5.9 中已内置，无需额外配置
    // 如需自定义分页行为，可在此添加 MybatisPlusInterceptor Bean
}
