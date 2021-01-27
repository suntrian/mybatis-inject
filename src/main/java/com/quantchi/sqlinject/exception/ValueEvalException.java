package com.quantchi.sqlinject.exception;

public class ValueEvalException extends RuntimeException {

    public ValueEvalException(Exception exception) {
        super(exception);
    }

}
