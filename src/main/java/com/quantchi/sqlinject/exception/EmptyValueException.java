package com.quantchi.sqlinject.exception;

public class EmptyValueException extends RuntimeException{

    private final String expression;
    private final Object value;

    public EmptyValueException(String expression, Object value) {
        this.expression = expression;
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String getMessage() {
        return expression + "计算值为" + value;
    }
}
