package com.quantchi.sqlinject.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Repeatable(SqlInjectGroups.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SqlInjectGroup {

    Logic logic() default Logic.OR;

    @AliasFor("parseInject")
    SqlParseInject[] value() default {};

    @AliasFor("value")
    SqlParseInject[] parseInject() default {};

    PlaceholderInject[] placeholderInject() default {};

}
