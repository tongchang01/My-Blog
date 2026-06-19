package com.tyb.myblog.v2.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 基础配置。
 *
 * <p>Mapper 扫描限定为带 {@link Mapper} 注解的接口，避免把 domain 层业务端口
 * 误注册为数据库 Mapper。业务 Mapper 统一位于各模块的
 * {@code infrastructure.persistence.mapper} 包。</p>
 */
@Configuration
@MapperScan(basePackages = "com.tyb.myblog.v2", annotationClass = Mapper.class)
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器。
     *
     * <p>先启用分页插件，并明确数据库类型为 MySQL。测试环境 H2 使用 MySQL 兼容模式，
     * 因此可以沿用同一分页方言，避免本地测试和真实库分页语义分裂。</p>
     */
    @Bean
    MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
