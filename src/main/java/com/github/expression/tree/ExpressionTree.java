package com.github.expression.tree;

public class ExpressionTree {

    private String operator = ConstantHolder.OP_NOP;

    private ExpressionTree left;
    private ExpressionTree right;
    private String value;

    ExpressionTree(String operator, ExpressionTree left, ExpressionTree right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    ExpressionTree(String operator, ExpressionTree left) {
        this.operator = operator;
        this.left = left;
    }

    ExpressionTree(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    void setRight(ExpressionTree right) {
        this.right = right;
    }

    public String getOperator() {
        return operator;
    }

    public ExpressionTree getLeft() {
        return left;
    }

    public ExpressionTree getRight() {
        return right;
    }

    public static ExpressionTreeBuilder builder() {
        return new ExpressionTreeBuilder();
    }
}
