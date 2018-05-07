package com.github.expression.formatter;

import com.github.expression.tree.ExpressionTree;


public class ExpressionTreeXmlFormatter {
    public String format(ExpressionTree tree) {
        StringBuilder builder = new StringBuilder();

        builder.append("<query>");
        builder.append("\n");

        writeAsXml(tree, builder, "  ");

        builder.append("</query>");
        builder.append("\n");
        return builder.toString();
    }

    private void writeAsXml(ExpressionTree tree, StringBuilder builder, String indent) {
        builder.append(indent);
        builder.append('<');
        builder.append(tree.getOperator());
        builder.append('>');
        builder.append("\n");
        String operator = tree.getOperator();
        ExpressionTree left = tree.getLeft();
        ExpressionTree right = tree.getRight();

        if (left != null  ||  right != null) {
            String indent2;
            indent2 = indent + "  ";

            if (left != null) {
                if (left.getValue() != null)
                    writeStringAsXml(builder, indent2, left.getValue());
                else
                    writeAsXml(left, builder, indent2);
            }

            if (right != null) {
                if (right.getValue() != null)
                    writeStringAsXml(builder, indent2, right.getValue());
                else
                    writeAsXml(right, builder, indent2);
            }
        }

        builder.append(indent);
        builder.append("</");
        builder.append(operator);
        builder.append('>');
        builder.append("\n");
    }

    private void writeStringAsXml(StringBuilder builder, String indent, String arg) {
        int	len;
        char quote = '"';

        builder.append(indent);
        builder.append("<arg>");

        len = arg.length();
        if (len > 0)
            quote = arg.charAt(0);

        for (int i = 0;  i < len;  i++) {
            int	ch;

            ch = arg.charAt(i);
            switch (ch) {
                case '<':
                    builder.append("&lt;");
                    break;

                case '>':
                    builder.append("&gt;");
                    break;

                case '&':
                    builder.append("&amp;");
                    break;

                case '\'':
                    if (ch == quote  &&  i > 0  &&  i < len-1)
                        builder.append('\\');
                    builder.append((char) ch);
                    break;

                case '"':
                    if (ch == quote  &&  i > 0  &&  i < len-1)
                        builder.append('\\');
                    builder.append((char) ch);
                    break;

                default:
                    if (ch <= 0x0020  ||  ch >= 0x0080  &&  ch <= 0x00A0) {
                        builder.append("&#");
                        builder.append(Integer.toString(ch));
                        builder.append(';');
                    } else
                        builder.append((char) ch);
                    break;
            }
        }

        builder.append("</arg>");
        builder.append("\n");
    }
}
