package com.quantchi.sqlinject.annotation;

public enum FailoverStrategy {

    THROW("抛出异常"),
    IGNORE("忽略异常，按原路径继续执行"),
    REJECT("不再执行，返回空或者null值");

    private String desc;

    FailoverStrategy(String desc) {
        this.desc = desc;
    }
}
