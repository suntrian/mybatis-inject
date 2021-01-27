package com.quantchi.sqlinject.spring;

import com.quantchi.sqlinject.mybatis.DataPrivilegeInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

@Configuration
@ConditionalOnBean({SqlSessionFactory.class})
@AutoConfigureAfter({MybatisAutoConfiguration.class})
@ConditionalOnProperty(name = "mybatis.inject.enabled", havingValue = "true", matchIfMissing = true)
public class MybatisInterceptorEnsureConfiguration {

    @Autowired
    private List<SqlSessionFactory> sqlSessionFactories;

    @Autowired
    private DataPrivilegeInterceptor dataPrivilegeInterceptor;

    @PostConstruct
    public void addDataPrivilegeInterceptor() {
        if (sqlSessionFactories != null && dataPrivilegeInterceptor != null) {
            for (SqlSessionFactory sqlSessionFactory : sqlSessionFactories) {
                if (sqlSessionFactory.getConfiguration().getInterceptors().stream()
                        .noneMatch(it->it instanceof DataPrivilegeInterceptor)) {
                    sqlSessionFactory.getConfiguration().addInterceptor(dataPrivilegeInterceptor);
                }
            }
        }
    }

}
