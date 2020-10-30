package com.quantchi.sqlinject.spring;

import com.github.pagehelper.PageHelper;
import com.quantchi.sqlinject.injector.SpringELHandler;
import com.quantchi.sqlinject.mybatis.DataPrivilegeInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
@ConditionalOnMissingClass(value = "com.github.pagehelper.PageHelper")
@EnableConfigurationProperties({MybatisSqlInjectProperties.class})
@ConditionalOnProperty(name = "mybatis.inject.enabled", havingValue = "true", matchIfMissing = true)
public class MybatisSqlInjectAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Interceptor dataPrivilegeInterceptor(SpringELHandler springELHandler,
                                                MybatisSqlInjectProperties properties) {
        return new DataPrivilegeInterceptor(springELHandler);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringELHandler springELHandler(AbstractBeanFactory beanFactory,
                                           MybatisSqlInjectProperties properties) {
        SpringELHandler springELHandler = new SpringELHandler(beanFactory, null);
        springELHandler.setFailoverStrategy(properties.getFailoverStrategy());
        return springELHandler;
    }


}
