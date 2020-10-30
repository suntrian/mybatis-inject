package com.quantchi.sqlinject.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SqlParseInject {

    boolean not() default false;

    MODE mode() default MODE.CUSTOM;

    String table();

    String field();

    String[] filter();


    enum MODE{
        EQUAL,
        IN,
        EXISTS,
        LIKE,
        BETWEEN,
        CUSTOM
    }

}
