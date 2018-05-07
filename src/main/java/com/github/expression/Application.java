package com.github.expression;

import com.github.expression.model.ExpressionTree;
import com.github.expression.service.ExpressionTreeBuilder;

import java.io.PrintWriter;

public class Application {
    public static void main(String[] args) throws Exception {
        String expression = "(date >= '2001-08-01' or transactionId like 'report%.pdf') and transaction.id like '55'";
        ExpressionTree tree = new ExpressionTreeBuilder()
                .withExpression(expression)
                .build();
        PrintWriter writer = new PrintWriter(System.out);
        tree.writeAsXml(writer);

//        ExpressionTree tree2 = new ExpressionTreeBuilder()
//                .withExpression("transaction.id like '55'")
//                .build();
//        tree2.writeAsXml(writer);
    }
}
