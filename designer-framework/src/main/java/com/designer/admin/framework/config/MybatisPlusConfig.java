package com.designer.admin.framework.config;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;

/**
 * MyBatis-Plus 扩展：为默认数据源 db1 注册分页插件与全表更新/删除防护
 *
 * @author 雨夜终端
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public void db1Cfg(@Db("db1") MybatisConfiguration cfg) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 不写死 DbType：由 PaginationInnerInterceptor 按 JDBC URL 自动识别方言，
        // 换库（MySQL / PostgreSQL / 达梦 …）时无需改代码
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor();
        pagination.setMaxLimit(500L);
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);

        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        cfg.addInterceptor(interceptor);
    }
}
