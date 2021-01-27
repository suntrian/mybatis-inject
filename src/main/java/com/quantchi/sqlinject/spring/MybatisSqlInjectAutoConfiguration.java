package com.quantchi.sqlinject.spring;

import com.quantchi.sqlinject.injector.SpringELHandler;
import com.quantchi.sqlinject.mybatis.DataPrivilegeInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean(SqlSessionFactory.class)
@EnableConfigurationProperties({SqlInjectProperties.class})
@ConditionalOnMissingClass(value = "com.github.pagehelper.PageHelper")
@ConditionalOnProperty(name = "mybatis.inject.enabled", havingValue = "true", matchIfMissing = true)
public class MybatisSqlInjectAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean
    public DataPrivilegeInterceptor dataPrivilegeInterceptor(SpringELHandler springELHandler,
                                                SqlInjectProperties properties) {
        return new DataPrivilegeInterceptor(springELHandler);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringELHandler springELHandler(AbstractBeanFactory beanFactory,
                                           SqlInjectProperties properties) {
        return new SpringELHandler(beanFactory, null, properties);
    }


}
