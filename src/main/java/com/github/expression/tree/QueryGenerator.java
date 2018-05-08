package com.github.expression.tree;

import com.github.expression.tree.ExpressionTree;

import java.text.SimpleDateFormat;

public class QueryGenerator {

    public static final String DEFAULT_DATE_MASK = "yyyy-mm-dd";
    private final SimpleDateFormat formatter;

    public QueryGenerator() {
        this.formatter = new SimpleDateFormat(DEFAULT_DATE_MASK);
    }

    public QueryGenerator(String mask) {
        this.formatter = new SimpleDateFormat(mask);
    }

    public String generate(String prefix, ExpressionTree tree) {
        StringBuilder builder = new StringBuilder(prefix);
        builder.append(" where ");
        handleTreeNode(builder, tree);
        return builder.toString();
    }

    private void handleTreeNode(StringBuilder builder, ExpressionTree tree) {
        if (tree.getOperator().equals(ConstantHolder.OP_IN)) {
            handleIn(builder, tree);
        } else {
            handleLeft(builder, tree.getLeft());
            builder.append(" ").append(getInitialOperator(tree)).append(" ");
            handleRight(builder, tree.getRight(), tree);
        }
    }

    private void handleLeft(StringBuilder builder, ExpressionTree tree) {
        if (tree != null) {
            if (tree.getOperator().equals(ConstantHolder.OP_NOP)) {
                builder
                        .append("name")
                        .append(" = ")
                        .append('\'')
                        .append(tree.getValue())
                        .append("'");
            } else {
                builder.append("(");
                handleLeft(builder, tree.getLeft());
                builder.append(" and ");
                handleRight(builder, tree.getRight(), tree);
                builder.append(")");
            }
        }
    }

    private void handleRight(StringBuilder builder, ExpressionTree tree, ExpressionTree parent) {
        if (tree != null) {
            if (tree.getOperator().equals(ConstantHolder.OP_IN)) {
                handleIn(builder, tree);
            } else if (tree.getOperator().equals(ConstantHolder.OP_NOP)) {
                String columnName = defineColumn(tree.getValue());
                builder
                        .append(columnName).append(" ")
                        .append(getOperatorValue(parent.getOperator())).append(" ")
                        .append(tree.getValue()).append("");
            } else {
                builder.append("(");
                handleLeft(builder, tree.getLeft());
                builder.append(" and ");
                handleRight(builder, tree.getRight(), tree);
                builder.append(")");
            }
        }
    }

    private void handleIn(StringBuilder builder, ExpressionTree tree) {
        if (tree != null) {
            if (tree.getOperator().equals(ConstantHolder.OP_IN)) {
                builder.append("(");
                handleLeft(builder, tree.getLeft());
                builder.append(" and ");
                String columnName = defineColumn(tree.getRight().getLeft().getValue());
                builder
                        .append(columnName).append(" ")
                        .append("in").append(" (");
                handleIn(builder, tree.getRight());
                builder.delete(builder.length() - 2, builder.length());
                builder.append(")");
                builder.append(")");
            } else if (tree.getOperator().equals(ConstantHolder.OP_LIST)) {
                handleIn(builder, tree.getRight());
                handleIn(builder, tree.getLeft());
            } else if (tree.getOperator().equals(ConstantHolder.OP_NOP)) {
                builder.append(tree.getValue()).append(", ");
            }
        }
    }

    private String getOperatorValue(String operator) {
        return ConstantHolder.reversedOperatorsMap.get(operator);
    }

    private String defineColumn(String value) {
        try {
            formatter.parse(value.replaceAll("^\'|\'$", ""));
            return "date_value";
        } catch (Exception e) { }
        try {
            Long.valueOf(value);
            return "num_value";
        } catch (Exception e) {}
        return "str_value";
    }

    private String getInitialOperator(ExpressionTree tree) {
        int size = count(tree);
        return (size <= 2) ?
                "and" : getOperatorValue(tree.getOperator());
    }

    private int count(ExpressionTree node) {
        if (node == null)
            return 0;
        else {
            int lDepth = count(node.getLeft());
            int rDepth = count(node.getRight());

            if (lDepth > rDepth)
                return (lDepth + 1);
            else
                return (rDepth + 1);
        }
    }
}
