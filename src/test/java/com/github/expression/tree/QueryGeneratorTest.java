package com.github.expression.tree;

import com.github.expression.tree.ExpressionTree;
import com.github.expression.tree.QueryGenerator;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QueryGeneratorTest {

    private static final String PREFIX = "select * from entry_tags";
    private QueryGenerator generator;

    @Before
    public void init() {
        this.generator = new QueryGenerator();
    }

    @Test
    public void simpleQueryShouldBeGenerated() {
        ExpressionTree tree = ExpressionTree.builder()
                .withExpression("transactionId = 1")
                .build();
        String sql = generator.generate(PREFIX, tree);
        assertEquals("select * from entry_tags where name = 'transactionId' and num_value = 1", sql);
    }

    @Test
    public void simpleInQueryShouldBeGenerated() {
        ExpressionTree tree = ExpressionTree.builder()
                .withExpression("transactionId in (1, 2, 3)")
                .build();
        String sql = generator.generate(PREFIX, tree);
        assertEquals("select * from entry_tags where (name = 'transactionId' and num_value in (3, 2, 1))", sql);
    }

    @Test
    public void simpleLikeQueryShouldBeGenerated() {
        ExpressionTree tree = ExpressionTree.builder()
                .withExpression("transactionRef like '%a'")
                .build();
        String sql = generator.generate(PREFIX, tree);
        assertEquals("select * from entry_tags where (name = 'transactionRef' and str_value like '%a') and (name = 'transactionId' and num_value in (3, 2, 1))", sql);
    }

    @Test
    public void likeAndInQueryShouldBeGenerated() {
        ExpressionTree tree = ExpressionTree.builder()
                .withExpression("transactionRef like '%a' and transactionId in (1, 2, 3)")
                .build();
        String sql = generator.generate(PREFIX, tree);
        assertEquals("select * from entry_tags where name = 'transactionRef' and str_value like '%a'", sql);
    }


    @Test
    public void complexQueryShouldBeGenerated() {
        ExpressionTree tree = ExpressionTree.builder()
                .withExpression("(date >= '2001-08-01' and transactionRef = '123-sdf') or userId = 1")
                .build();
        String sql = generator.generate(PREFIX, tree);
        assertEquals("select * from entry_tags where ((name = 'date' and date_value >= '2001-08-01') and (name = 'transactionRef' and str_value = '123-sdf')) or (name = 'userId' and num_value = 1)", sql);
    }


    @Test
    public void inQueryShouldBeGenerated() {
        ExpressionTree tree = ExpressionTree.builder()
                .withExpression("date >= '2001-08-01' and transactionRef = '123-sdf' and userId in (1, 2, 3)")
                .build();
        String sql = generator.generate(PREFIX, tree);
        assertEquals("select * from entry_tags where ((name = 'date' and date_value >= '2001-08-01') and (name = 'transactionRef' and str_value = '123-sdf')) and (name = 'userId' and num_value in (3, 2, 1))", sql);
    }

    @Test
    public void manyAndQueryShouldBeGenerated() {
        ExpressionTree tree = ExpressionTree.builder()
                .withExpression("date >= '2001-08-01' and transactionRef = '123-sdf' and userId = 1 and column like 'sdf'")
                .build();
        String sql = generator.generate(PREFIX, tree);
        assertEquals("select * from entry_tags where (((name = 'date' and date_value >= '2001-08-01') and (name = 'transactionRef' and str_value = '123-sdf')) and (name = 'userId' and num_value = 1)) and (name = 'column' and str_value like 'sdf')", sql);
    }
}
