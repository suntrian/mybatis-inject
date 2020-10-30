package com.quantchi.sqlinject.exception;

public class EmptyValueException extends RuntimeException{

    private Object value;

    public EmptyValueException(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
