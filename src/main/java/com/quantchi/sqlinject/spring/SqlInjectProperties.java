package com.quantchi.sqlinject.spring;


import com.quantchi.sqlinject.annotation.FailoverStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mybatis.inject")
public class SqlInjectProperties {

    private Boolean enabled = true;

    private FailoverStrategy failoverStrategy = FailoverStrategy.THROW;

    //过滤条件的值为空或者null或者""之类的值时认为是错误
    private Boolean emptyValueAsFail = true;

    public void setFailoverStrategy(FailoverStrategy failoverStrategy) {
        this.failoverStrategy = failoverStrategy;
    }

    public void setEmptyValueAsFail(Boolean emptyValueAsFail) {
        this.emptyValueAsFail = emptyValueAsFail;
    }

    public FailoverStrategy getFailoverStrategy() {
        return failoverStrategy;
    }

    public Boolean getEmptyValueAsFail() {
        return emptyValueAsFail;
    }
}
