package com.github.expression.token;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TokenExtractorTest {

    private TokenExtractor tokenExtractor;

    @Before
    public void init() {
        this.tokenExtractor = new TokenExtractor();
    }

    @Test
    public void tokensShouldBeExtracted() {
        List<String> tokens = tokenExtractor.getTokens("transactionId = 1");
        assertNotNull(tokens);
        assertEquals(3, tokens.size());
        assertTrue(tokens.contains("transactionId"));
        assertTrue(tokens.contains("="));
        assertTrue(tokens.contains("1"));
    }
}
