package com.github.expression.model;

public class LineMetadata {
    private int currentPosition;
    private char currentChar;

    public LineMetadata(int currentPosition, char currentChar) {
        this.currentPosition = currentPosition;
        this.currentChar = currentChar;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public char getCurrentChar() {
        return currentChar;
    }
}
