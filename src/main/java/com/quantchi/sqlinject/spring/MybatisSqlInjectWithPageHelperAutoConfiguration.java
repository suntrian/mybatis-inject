package com.quantchi.sqlinject.spring;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.quantchi.sqlinject.injector.SpringELHandler;
import com.quantchi.sqlinject.mybatis.DataPrivilegeInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Configuration
@ConditionalOnBean(SqlSessionFactory.class)
@ConditionalOnClass(PageHelper.class)
@EnableConfigurationProperties({MybatisSqlInjectProperties.class})
@ConditionalOnProperty(name = "mybatis.inject.enabled", havingValue = "true", matchIfMissing = true)
public class MybatisSqlInjectWithPageHelperAutoConfiguration {

    private static final ThreadLocal<Page<?>> tempPage = new ThreadLocal<>();

    private Method setLocalPageMethod;

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public Interceptor dataPrivilegeInterceptor(SpringELHandler springELHandler,
                                                MybatisSqlInjectProperties properties,
                                                Environment env) {
        DataPrivilegeInterceptor dataPrivilegeInterceptor = new DataPrivilegeInterceptor(springELHandler);
        dataPrivilegeInterceptor.setInterceptPageHelperCountMethod(true);
        String pageHelperCountSuffix = env.getProperty("pagehelper.properties.countSuffix");
        if (!StringUtils.isEmpty(pageHelperCountSuffix)) {
            dataPrivilegeInterceptor.setPageHelperCountSuffix(pageHelperCountSuffix);;
        }
        return dataPrivilegeInterceptor;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public SpringELHandler springELHandler(AbstractBeanFactory beanFactory,
                                           MybatisSqlInjectProperties properties) {
        SpringELHandler springELHandler= new SpringELHandler(beanFactory, null);
        springELHandler.setFailoverStrategy(properties.getFailoverStrategy());
        springELHandler.setPreHandler(()-> {
            tempPage.set(PageHelper.getLocalPage());
            PageHelper.clearPage();
        });
        springELHandler.setPostHandler(()-> {
            Page<?> page = tempPage.get();
            if (setLocalPageMethod == null) {
                setLocalPageMethod = BeanUtils.findMethod(PageHelper.class, "setLocalPage", Page.class);
                if (setLocalPageMethod == null ) {
                    if (page != null) {
                        if (page.getOrderBy() != null) {
                            PageHelper.startPage(page.getPageNum(), page.getPageSize(), page.getOrderBy());
                        } else {
                            PageHelper.startPage(page.getPageNum(), page.getPageSize(), page.isCount(), page.getReasonable(), page.getPageSizeZero() );
                        }
                    }
                    return;
                } else {
                    setLocalPageMethod.setAccessible(true);
                }
            }
            try {
                setLocalPageMethod.invoke(null, page);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            tempPage.remove();
        });
        return springELHandler;
    }

}
