package com.github.expression.model;

public class TokenData {
    private String extractedToken;
    private int position;

    public TokenData(String extractedToken, int position) {
        this.extractedToken = extractedToken;
        this.position = position;
    }

    public String getExtractedToken() {
        return extractedToken;
    }

    public int getPosition() {
        return position;
    }
}