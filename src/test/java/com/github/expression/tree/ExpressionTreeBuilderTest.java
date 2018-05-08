package com.github.expression.tree;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ExpressionTreeBuilderTest {

    @Test
    public void treeShouldBeBuilt() {
        ExpressionTree tree = ExpressionTree.builder()
                .withExpression("transactionId = 1")
                .build();
        assertNotNull(tree);
    }
}
