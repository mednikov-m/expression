package com.github.expression.model;

import com.github.expression.util.ConstantHolder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

public class ExpressionTree {

    private String operator = ConstantHolder.OP_NOP;

    private ExpressionTree left;
    private ExpressionTree right;
    private String value;

    public ExpressionTree(String operator, ExpressionTree left, ExpressionTree right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    public ExpressionTree(String operator, ExpressionTree left) {
        this.operator = operator;
        this.left = left;
    }

    public ExpressionTree(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setRight(ExpressionTree right) {
        this.right = right;
    }

    public void writeAsXml(Writer out)
            throws IOException
    {
        BufferedWriter os;

        if (out instanceof BufferedWriter)
            os = (BufferedWriter) out;
        else
            os = new BufferedWriter(out);

        // Write this expression tree as XML
        os.write("<query>");
        os.newLine();

        writeAsXml(os, "  ");

        os.write("</query>");
        os.newLine();

        os.flush();
    }

    protected void writeAsXml(BufferedWriter out, String indent)
            throws IOException
    {
        // Write this expression tree as XML
        out.write(indent);
        out.write('<');
        out.write(operator);
        out.write('>');
        out.newLine();

        if (left != null  ||  right != null)
        {
            String	indent2;

            indent2 = indent + "  ";

            if (left != null)
            {
                if (left.value != null)
                    writeStringAsXml(out, indent2, left.value);
                else
                    (left).writeAsXml(out, indent2);
            }

            if (right != null)
            {
                if (right.value != null)
                    writeStringAsXml(out, indent2, right.value);
                else
                    right.writeAsXml(out, indent2);
            }
        }

        out.write(indent);
        out.write("</");
        out.write(operator);
        out.write('>');
        out.newLine();
    }

    protected void writeStringAsXml(BufferedWriter out, String indent,
                                    String arg)
            throws IOException
    {
        int	len;
        char	quote =	'"';

        // Write a single expression operand as XML text
        out.write(indent);
        out.write("<arg>");

        len = arg.length();
        if (len > 0)
            quote = arg.charAt(0);

        for (int i = 0;  i < len;  i++)
        {
            int		ch;

            ch = arg.charAt(i);
            switch (ch)
            {
                case '<':
                    out.write("&lt;");
                    break;

                case '>':
                    out.write("&gt;");
                    break;

                case '&':
                    out.write("&amp;");
                    break;

                case '\'':
                    if (ch == quote  &&  i > 0  &&  i < len-1)
                        out.write('\\');
                    out.write((char) ch);
                    break;

                case '"':
                    if (ch == quote  &&  i > 0  &&  i < len-1)
                        out.write('\\');
                    out.write((char) ch);
                    break;

                default:
                    if (ch <= 0x0020  ||  ch >= 0x0080  &&  ch <= 0x00A0)
                    {
                        out.write("&#");
                        out.write(Integer.toString(ch));
                        out.write(';');
                    }
                    else
                        out.write((char) ch);
                    break;
            }
        }

        out.write("</arg>");
        out.newLine();
    }
}
