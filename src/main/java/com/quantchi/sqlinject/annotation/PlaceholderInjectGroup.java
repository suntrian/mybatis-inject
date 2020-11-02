package com.quantchi.sqlinject.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PlaceholderInjectGroup {

    PlaceholderInject[] value();

}
