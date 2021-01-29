package com.quantchi.sqlinject.annotation;

public enum FailoverStrategy {

    THROW("抛出异常"),
    IGNORE("忽略异常，使用原SQL继续执行"),
    REJECT("SQL注入1=2, 返回空结果");

    FailoverStrategy(String desc) { }
}
