package com.quantchi.sqlinject.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SqlParseInjectGroup {

    Logic logic() default Logic.OR;

    SqlParseInject[] value();

}
