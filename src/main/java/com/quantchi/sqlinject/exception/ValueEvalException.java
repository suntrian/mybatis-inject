package com.quantchi.sqlinject.exception;

public class ValueEvalException extends RuntimeException {

    private Exception exception;

    public ValueEvalException(Exception exception) {
        this.exception = exception;
    }

}
