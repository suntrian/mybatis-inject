package com.quantchi.sqlinject.spring;


import com.quantchi.sqlinject.SqlInjectOnce;
import com.quantchi.sqlinject.annotation.Dialect;
import com.quantchi.sqlinject.annotation.FailoverStrategy;
import com.quantchi.sqlinject.annotation.TreatBlankStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Optional;

@ConfigurationProperties(prefix = "mybatis.inject")
public class SqlInjectProperties {

    private Boolean enabled = true;

    private FailoverStrategy failoverStrategy = FailoverStrategy.THROW;

    //过滤条件的值为空或者null或者""之类的值时认为是错误
    private TreatBlankStrategy treatBlankStrategy = TreatBlankStrategy.REJECT;

    private Dialect dialect = Dialect.MYSQL;

    public void setFailoverStrategy(FailoverStrategy failoverStrategy) {
        this.failoverStrategy = failoverStrategy;
    }

    public FailoverStrategy getFailoverStrategy() {
        return Optional.ofNullable(SqlInjectOnce.failoverStrategy()).orElse(failoverStrategy);
    }

    public TreatBlankStrategy getTreatBlankStrategy() {
        return Optional.ofNullable(SqlInjectOnce.treatBlankStrategy()).orElse(treatBlankStrategy);
    }

    public void setTreatBlankStrategy(TreatBlankStrategy treatBlankStrategy) {
        this.treatBlankStrategy = treatBlankStrategy;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Dialect getDialect() {
        return dialect;
    }

    public void setDialect(Dialect dialect) {
        this.dialect = dialect;
    }

}
