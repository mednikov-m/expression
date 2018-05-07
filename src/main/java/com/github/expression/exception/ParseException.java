package com.github.expression.exception;

public class ParseException extends RuntimeException {
    private int errorOffset;

    public ParseException(String message, int errorOffset) {
        super(message);
        this.errorOffset = errorOffset;
    }

    public int getErrorOffset() {
        return this.errorOffset;
    }
}