package com.quantchi.sqlinject.annotation;

public enum TreatBlankStrategy {

    THROW( FailoverStrategy.THROW,  "抛出异常"),
    REJECT(FailoverStrategy.REJECT,  "SQL注入 1=2，返回空结果"),
    ACCEPT(FailoverStrategy.IGNORE, "多管闲事，我要用的就是空值" ),
    IGNORE(FailoverStrategy.IGNORE, "忽略空值，使用原SQL继续执行");

    private final FailoverStrategy failoverStrategy;

    TreatBlankStrategy(FailoverStrategy failoverStrategy, String desc) {
        this.failoverStrategy = failoverStrategy;
    }

    public FailoverStrategy getFailoverStrategy() {
        return failoverStrategy;
    }
}
