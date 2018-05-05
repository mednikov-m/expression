package com.github.expression;

import com.github.expression.model.ExpressionTree;
import com.github.expression.service.ExpressionTreeBuilder;

import java.io.PrintWriter;

public class Application {
    public static void main(String[] args) throws Exception {
        ExpressionTreeBuilder parser;     // Query expression parser
        String      crit;       // Query expression
        ExpressionTree query;      // Query control object

        parser = new ExpressionTreeBuilder();
        crit = "date >= '2001-08-01' or transactionId like 'report%.pdf'";
        String queryStr = "transactionId = 1";
        query = parser.build(crit);
        PrintWriter writer = new PrintWriter(System.out);
        query.writeAsXml(writer);
    }
}
