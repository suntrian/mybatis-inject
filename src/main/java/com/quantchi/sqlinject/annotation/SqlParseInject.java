package com.quantchi.sqlinject.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SqlParseInject {

    boolean not() default false;

    FilterMode mode() default FilterMode.CUSTOM;

    String table() default "";

    String field() default "";

    String[] filter();

    Dialect dialect() default Dialect.MYSQL;

}
