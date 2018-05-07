package com.github.expression;

import com.github.expression.formatter.ExpressionTreeXmlFormatter;
import com.github.expression.tree.ExpressionTree;
import com.github.expression.tree.ExpressionTreeBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExpressionTreeBuilderTest {

    @Test
    public void treeShouldBeBuiltAndFormattedToXml() {
        String expression = "(date >= '2001-08-01' or transactionId like 'report%.pdf') and transaction.id like '55'";
        ExpressionTree tree = ExpressionTree.builder()
                .withExpression(expression)
                .build();
        ExpressionTreeXmlFormatter formatter = new ExpressionTreeXmlFormatter();
        String xml = formatter.format(tree);
        assertEquals("<query>\n" +
                "  <and>\n" +
                "    <or>\n" +
                "      <ge>\n" +
                "        <arg>date</arg>\n" +
                "        <arg>'2001-08-01'</arg>\n" +
                "      </ge>\n" +
                "      <like>\n" +
                "        <arg>transactionId</arg>\n" +
                "        <arg>'report%.pdf'</arg>\n" +
                "      </like>\n" +
                "    </or>\n" +
                "    <like>\n" +
                "      <member>\n" +
                "        <arg>transaction</arg>\n" +
                "        <arg>id</arg>\n" +
                "      </member>\n" +
                "      <arg>'55'</arg>\n" +
                "    </like>\n" +
                "  </and>\n" +
                "</query>\n", xml);
    }
}
