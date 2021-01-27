package com.quantchi.sqlinject.annotation;

import java.lang.annotation.*;

/**
 * 暂未实现
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SqlInjectGroups {

    Logic logic() default Logic.AND;

    SqlInjectGroup[] value();

}
