package com.github.expression.service;

import com.github.expression.model.ExpressionTree;
import com.github.expression.util.ConstantHolder;

import java.text.ParseException;
import java.util.List;
import java.util.Objects;

public class ExpressionTreeBuilder {

    private TokenExtractor tokenExtractor = new TokenExtractor();

    private String[] expressionTokens;
    private int currentTokenIndex;

    public ExpressionTree build(String expr) throws ParseException {
        ExpressionTree tree;
        ExpressionTree res;

        List<String> tokens = tokenExtractor.getTokens(expr);

        expressionTokens = tokens.toArray(new String[tokens.size()]);

        // Parse the expression tokens
        currentTokenIndex = 0;
        tree = parse_or_expr();

        if (tree.getValue() != null) {
            res = new ExpressionTree(ConstantHolder.OP_VALUE, tree);
        } else {
            res = tree;
        }


        // Check for the end of the expression
        if (currentTokenIndex < expressionTokens.length)
            throw new ParseException("Malformed expression at: '"
                    + expressionTokens[currentTokenIndex] + "'", currentTokenIndex);

        return (res);
    }


    private ExpressionTree parse_or_expr()
            throws ParseException
    {
        ExpressionTree	res;

        // or_expr: and_expr ['OR' and_expr]...
        res = parse_and_expr();

        for (;;)
        {
            String	tok;
            String	keyword;

            // Check for end of expression
            if (currentTokenIndex >= expressionTokens.length)
                return (res);

            // Continue parsing
            tok = expressionTokens[currentTokenIndex++];
            keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());
            if (keyword == null)
                throw new ParseException("Bad operator: '" + tok + "'",
                        currentTokenIndex-1);

            // Parse a mul-op
            if (keyword == ConstantHolder.OP_OR)
            {
                ExpressionTree	expr;

                // or_expr: and_expr 'OR' and_expr
                expr = new ExpressionTree(keyword, res, parse_and_expr());
                res = expr;
            }
            else
            {
                currentTokenIndex--;
                return (res);
            }
        }
    }

    /***************************************************************************
     * Parse a query subexpression, converting it into an expression subtree.
     * See {@link #parse_or_expr} for further details.
     *
     * <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
     * <p>
     * <b> Syntax </b>
     * <p>
     * <pre>
     *    and_expr:
     *        not_expr
     *        not_expr 'AND' and_expr           op: OP_AND
     * </pre>
     *
     * @since	1.1, 2001-03-23
     */

    private ExpressionTree parse_and_expr()
            throws ParseException
    {
        ExpressionTree	res;

        // and_expr: not_expr ['AND' not_expr]...
        res = parse_not_expr();

        for (;;)
        {
            String	tok;
            String	keyword;

            // Check for end of expression
            if (currentTokenIndex >= expressionTokens.length)
                return (res);

            // Continue parsing
            tok = expressionTokens[currentTokenIndex++];
            keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());
            if (keyword == null)
                throw new ParseException("Bad operator: '" + tok + "'",
                        currentTokenIndex-1);

            // Parse a mul-op
            if (keyword == ConstantHolder.OP_AND)
            {
                ExpressionTree	expr;

                // and_expr: not_expr 'AND' not_expr
                expr = new ExpressionTree(keyword, res, parse_not_expr());
                res = expr;
            }
            else
            {
                currentTokenIndex--;
                return (res);
            }
        }
    }

    /***************************************************************************
     * Parse a query subexpression, converting it into an expression subtree.
     * See {@link #parse_or_expr} for further details.
     *
     * <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
     * <p>
     * <b> Syntax </b>
     * <p>
     * <pre>
     *    not_expr:
     *        cmp_expr
     *        'NOT' not_expr                    op: OP_NOT
     * </pre>
     *
     * @since	1.6, 2001-04-08
     */

    private ExpressionTree parse_not_expr()
            throws ParseException
    {
        ExpressionTree	res;
        String	tok;
        String	keyword;

        // Check for end of expression
        if (currentTokenIndex >= expressionTokens.length)
            return (null);

        // Continue parsing
        tok = expressionTokens[currentTokenIndex++];
        keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());

        if (keyword == ConstantHolder.OP_NOT)
        {
            ExpressionTree	expr;

            // not_expr: 'NOT' cmp_expr
            expr = new ExpressionTree(keyword, parse_not_expr());
            res = expr;
        }
        else
        {
            // not_expr: cmp_expr
            currentTokenIndex--;
            res = parse_cmp_expr();
        }

        return (res);
    }

    /***************************************************************************
     * Parse a query subexpression, converting it into an expression subtree.
     * See {@link #parse_or_expr} for further details.
     *
     * <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
     * <p>
     * <b> Syntax </b>
     * <p>
     * <pre>
     *    cmp_expr:
     *        add_expr
     *        add_expr 'IS' ['NOT'] 'NULL'                         op: OP_IS
     *        add_expr ['NOT'] '='  add_expr                       op: OP_EQ
     *        add_expr ['NOT'] '&lt;&gt;' add_expr                       op: OP_NE
     *        add_expr ['NOT'] '&lt;'  add_expr                       op: OP_LT
     *        add_expr ['NOT'] '&lt;=' add_expr                       op: OP_LE
     *        add_expr ['NOT'] '&gt;'  add_expr                       op: OP_GT
     *        add_expr ['NOT'] '&gt;=' add_expr                       op: OP_GE
     *        add_expr ['NOT'] 'CONTAINS' add_expr                 op: OP_CONTAINS
     *        add_expr ['NOT'] 'LIKE'     add_expr                 op: OP_LIKE
     *        add_expr ['NOT'] 'LIKEFILE' add_expr                 op: OP_LIKEFILE
     *        add_expr ['NOT'] 'BETWEEN'  add_expr 'AND' add_expr  op: OP_BETWEEN
     *        add_expr ['NOT'] 'IN' '(' expr_list ')'              op: OP_IN
     * </pre>
     *
     * @since	1.1, 2001-03-24
     */

    private ExpressionTree parse_cmp_expr()
            throws ParseException
    {
        ExpressionTree	res;
        String	tok;
        String	keyword;
        boolean	isCompl;

        // Parse 'add_expr'
        res = parse_add_expr();

        // Check for end of expression
        if (currentTokenIndex >= expressionTokens.length)
            return (res);

        // Continue parsing
        tok = expressionTokens[currentTokenIndex++];
        keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());

        if (keyword == null)
        {
            // Invalid operator
            throw new ParseException("Bad operator: '" + tok + "'", currentTokenIndex-1);
        }

        // Parse 'IS [NOT] NULL'
        if (keyword == ConstantHolder.OP_IS)
        {
            ExpressionTree	expr;

            // Parse '[NOT] NULL'
            if (currentTokenIndex < expressionTokens.length)
                tok = expressionTokens[currentTokenIndex++];
            else
                throw new ParseException("Missing operand following: '"
                        + tok + "'", currentTokenIndex-1);
            keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());

            expr = new ExpressionTree(ConstantHolder.OP_IS, res, new ExpressionTree(ConstantHolder.OP_NULL));
            res = expr;

            // Parse an optional 'NOT'
            if (keyword == ConstantHolder.OP_NOT)
            {
                // Consume a 'NOT'
                if (currentTokenIndex < expressionTokens.length)
                    tok = expressionTokens[currentTokenIndex++];
                else
                    throw new ParseException("Missing 'NULL' following: '"
                            + tok + "'", currentTokenIndex-1);
                keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());

                // cmp_expr: add_expr 'IS' ['NOT'] 'NULL'
                expr = new ExpressionTree(ConstantHolder.OP_NOT, res);
                res = expr;
            }

            // Parse 'NULL'
            if (keyword != ConstantHolder.OP_NULL)
                throw new ParseException("Missing 'NULL' at: '"
                        + tok + "'", currentTokenIndex-1);

            return (res);
        }

        // Parse an optional 'NOT'
        isCompl = false;
        if (keyword == ConstantHolder.OP_NOT)
        {
            // Consume a 'NOT'
            if (currentTokenIndex < expressionTokens.length)
                tok = expressionTokens[currentTokenIndex++];
            else
                throw new ParseException("Missing operator following: '"
                        + tok + "'", currentTokenIndex-1);

            isCompl = true;
            keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());
        }

        // Parse a compare-op
        if (keyword == ConstantHolder.OP_EQ  ||
                keyword == ConstantHolder.OP_NE  ||
                keyword == ConstantHolder.OP_LT  ||
                keyword == ConstantHolder.OP_LE  ||
                keyword == ConstantHolder.OP_GT  ||
                keyword == ConstantHolder.OP_GE  ||
                keyword == ConstantHolder.OP_CONTAINS ||
                keyword == ConstantHolder.OP_LIKE  ||
                keyword == ConstantHolder.OP_LIKEFILE)
        {
            ExpressionTree	expr;

            // cmp_expr: add_expr ['NOT'] compare-op add_expr
            expr = new ExpressionTree(keyword, res, parse_add_expr());
            res = expr;
        }
        else if (keyword == ConstantHolder.OP_BETWEEN)
        {
            ExpressionTree	expr;
            ExpressionTree	sub;
            ExpressionTree	lo;
            ExpressionTree	hi;

            // cmp_expr: add_expr1 ['NOT'] 'BETWEEN' add_expr2 'AND' add_expr3
            expr = new ExpressionTree(ConstantHolder.OP_BETWEEN, res);

            lo = parse_add_expr();

            // Consume an 'AND'
            tok = "<end>";
            keyword = null;
            if (currentTokenIndex < expressionTokens.length)
            {
                tok = expressionTokens[currentTokenIndex++];
                keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());
            }

            if (keyword != ConstantHolder.OP_AND)
                throw new ParseException("Missing expected 'AND' at: '"
                        + tok + "'", currentTokenIndex-1);

            hi = parse_add_expr();

            // Build an expr subtree: {BETWEEN op1 {AND op2 op3}}
            sub = new ExpressionTree(ConstantHolder.OP_AND, lo, hi);

            expr.setRight(sub);
            res = expr;
        }
        else if (keyword == ConstantHolder.OP_IN)
        {
            ExpressionTree	expr;

            // cmp_expr: add_expr ['NOT'] 'IN' '(' expr_list ')'
            expr = new ExpressionTree(ConstantHolder.OP_IN, res);

            // Consume a '('
            tok = "<end>";
            keyword = null;
            if (currentTokenIndex < expressionTokens.length)
            {
                tok = expressionTokens[currentTokenIndex++];
                keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());
            }

            if (keyword != ConstantHolder.OP_LP)
                throw new ParseException("Missing expected '(' at: '"
                        + tok + "'", currentTokenIndex-1);

            expr.setRight(parse_expr_list());

            // Consume a ')'
            tok = "<end>";
            keyword = null;
            if (currentTokenIndex < expressionTokens.length)
            {
                tok = expressionTokens[currentTokenIndex++];
                keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());
            }

            if (keyword != ConstantHolder.OP_RP)
                throw new ParseException("Missing expected ')' at: '"
                        + tok + "'", currentTokenIndex-1);

            res = expr;
        }
        else
            currentTokenIndex--;

        // Handle a 'NOT' operator modifier
        if (isCompl)
        {
            ExpressionTree	expr;

            // cmp_expr: add_expr 'NOT' compare-op add_expr
            expr = new ExpressionTree(ConstantHolder.OP_NOT, res);
            res = expr;
        }

        return (res);
    }

    /***************************************************************************
     * Parse a query subexpression, converting it into an expression subtree.
     * See {@link #parse_or_expr} for further details.
     *
     * <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
     * <p>
     * <b> Syntax </b>
     * <p>
     * <pre>
     *    add_expr:
     *        mul_expr
     *        mul_expr '+'  add_expr            op: OP_ADD
     *        mul_expr '-'  add_expr            op: OP_SUB
     *        mul_expr '||' add_expr            op: OP_CONCAT
     * </pre>
     *
     * @since	1.10, 2007-07-30
     */

    private ExpressionTree parse_add_expr()
            throws ParseException
    {
        ExpressionTree	res;

        // add_expr: mul_expr [add-op mul_expr]...
        res = parse_mul_expr();

        for (;;)
        {
            String	tok;
            String	keyword;

            // Check for end of expression
            if (currentTokenIndex >= expressionTokens.length)
                return (res);

            // Continue parsing
            tok = expressionTokens[currentTokenIndex++];
            keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());
            if (keyword == null)
                throw new ParseException("Bad operator: '" + tok + "'",
                        currentTokenIndex-1);

            // Parse an add-op
            if (keyword == ConstantHolder.OP_ADD  ||
                    keyword == ConstantHolder.OP_SUB  ||
                    keyword == ConstantHolder.OP_CONCAT)
            {
                ExpressionTree	expr;

                // add_expr: mul_expr add-op mul_expr
                expr = new ExpressionTree(keyword, res, parse_mul_expr());
                res = expr;
            }
            else
            {
                currentTokenIndex--;
                return (res);
            }
        }
    }

    /***************************************************************************
     * Parse a query subexpression, converting it into an expression subtree.
     * See {@link #parse_or_expr} for further details.
     *
     * <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
     * <p>
     * <b> Syntax </b>
     * <p>
     * <pre>
     *    mul_expr:
     *        unary_expr
     *        unary_expr '*'   mul_expr         op: OP_MUL
     *        unary_expr '/'   mul_expr         op: OP_DIV
     *        unary_expr 'MOD' mul_expr         op: OP_MOD
     * </pre>
     *
     * @since	1.10, 2007-07-30
     */

    private ExpressionTree parse_mul_expr()
            throws ParseException
    {
        ExpressionTree	res;

        // mul_expr: unary_expr [mul-op unary_expr]...
        res = parse_unary_expr();

        for (;;)
        {
            String	tok;
            String	keyword;

            // Check for end of expression
            if (currentTokenIndex >= expressionTokens.length)
                return (res);

            // Continue parsing
            tok = expressionTokens[currentTokenIndex++];
            keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());
            if (keyword == null)
                throw new ParseException("Bad operator: '" + tok + "'",
                        currentTokenIndex-1);

            // Parse a mul-op
            if (keyword == ConstantHolder.OP_MUL  ||
                    keyword == ConstantHolder.OP_DIV  ||
                    keyword == ConstantHolder.OP_MOD)
            {
                ExpressionTree	expr;

                // mul_expr: unary_expr mul-op unary_expr
                expr = new ExpressionTree(keyword, res, parse_unary_expr());
                res = expr;
            }
            else
            {
                currentTokenIndex--;
                return (res);
            }
        }
    }

    /***************************************************************************
     * Parse a query subexpression, converting it into an expression subtree.
     * See {@link #parse_or_expr} for further details.
     *
     * <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
     * <p>
     * <b> Syntax </b>
     * <p>
     * <pre>
     *    unary_expr:
     *        expo_expr
     *        '+' unary_expr                    op: OP_POS
     *        '-' unary_expr                    op: OP_NEG
     * </pre>
     *
     * @since	1.10, 2007-07-30
     */

    private ExpressionTree parse_unary_expr()
            throws ParseException
    {
        ExpressionTree	res;
        String	tok;
        String	keyword;

        // Check for end of expression
        if (currentTokenIndex >= expressionTokens.length)
            throw new ParseException("Missing operand/operator", currentTokenIndex-1);

        // Parse unary-op
        tok = expressionTokens[currentTokenIndex++];
        keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());

        // Parse a unary-op
        if (keyword == ConstantHolder.OP_ADD  ||
                keyword == ConstantHolder.OP_SUB)
        {
            ExpressionTree	expr;

            // unary_expr: unary-op unary_expr
            // Note: unary-op is right-associative, '--a' = '-(-a)'
            expr = new ExpressionTree(Objects.equals(keyword, ConstantHolder.OP_ADD) ? ConstantHolder.OP_POS : ConstantHolder.OP_NEG,
                    parse_unary_expr());
            res = expr;
        }
        else
        {
            // unary_expr: expo_expr
            currentTokenIndex--;
            res = parse_expo_expr();
        }

        return (res);
    }

    /***************************************************************************
     * Parse a query subexpression, converting it into an expression subtree.
     * See {@link #parse_or_expr} for further details.
     *
     * <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
     * <p>
     * <b> Syntax </b>
     * <p>
     * <pre>
     *    expo_expr:
     *        operand
     *        operand '**' unary_expr           op: OP_EXPO
     * </pre>
     *
     * @since	1.10, 2007-07-30
     */

    private ExpressionTree parse_expo_expr()
            throws ParseException
    {
        ExpressionTree	res;
        String	tok;
        String	keyword;

        // Parse 'operand'
        res = parse_operand();

        // Check for end of expression
        if (currentTokenIndex >= expressionTokens.length)
            return (res);

        // Continue parsing
        tok = expressionTokens[currentTokenIndex++];
        keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());

        if (keyword == null)
        {
            // Invalid operator
            throw new ParseException("Bad operator: '" + tok + "'", currentTokenIndex-1);
        }

        // Parse an expo-op
        if (keyword == ConstantHolder.OP_EXPO)
        {
            ExpressionTree	expr;

            // expo_expr: operand expo-op unary_expr
            // Note: expo-op is right-associative, 'a**b**c' = 'a**(b**c)'
            expr = new ExpressionTree(keyword, res, parse_unary_expr());
            res = expr;
        }
        else
            currentTokenIndex--;

        return (res);
    }

    /***************************************************************************
     * Parse a query subexpression, converting it into an expression subtree.
     * See {@link #parse_or_expr} for further details.
     *
     * <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
     * <p>
     * <b> Syntax </b>
     * <p>
     * <pre>
     *    operand:
     *        '(' or_expr ')'
     *        name_expr
     *        number                               return: String
     *        'NULL'                               return: String
     * </pre>
     *
     * @since	1.1, 2001-03-23
     */

    private ExpressionTree parse_operand()
            throws ParseException
    {
        ExpressionTree	res;
        String	tok;
        String	keyword;

        // Check for end of expression
        if (currentTokenIndex >= expressionTokens.length)
            return (null);

        // Continue parsing
        tok = expressionTokens[currentTokenIndex++];
        keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());

        if (keyword == null)
        {
            char	ch;

            // operand: name_expr | number | string
            ch = tok.charAt(0);
            if (ch == '"'  ||  ch == '\''  ||
                    Character.isJavaIdentifierPart(ch))
            {
                // operand: name_expr
                currentTokenIndex--;
                return (parse_name_expr());
            }
            else
            {
                // operand: number
                return new ExpressionTree(tok);
            }
        }
        else if (keyword == ConstantHolder.OP_NULL)
        {
            // operand: 'NULL'
            return new ExpressionTree(tok);
        }
        else if (keyword == ConstantHolder.OP_LP)
        {
            ExpressionTree	expr;

            // operand: '(' or_expr ')'
            res = parse_or_expr();

            // Consume a closing ')'
            tok = "<end>";
            keyword = null;
            if (currentTokenIndex < expressionTokens.length)
            {
                tok = expressionTokens[currentTokenIndex++];
                keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());
            }

            if (keyword != ConstantHolder.OP_RP)
                throw new ParseException("Missing closing ')' at: '"
                        + tok + "'", currentTokenIndex-1);
        }
        else
        {
            throw new ParseException("Bad operand: '" + tok + "'", currentTokenIndex-1);
        }

        return (res);
    }


    /***************************************************************************
     * Parse a query subexpression, converting it into an expression subtree.
     * See {@link #parse_or_expr} for further details.
     *
     * <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
     * <p>
     * <b> Syntax </b>
     * <p>
     * <pre>
     *    name_expr:
     *        name                                 return: String
     *        string                               return: String
     *        name   name_tail...
     *        string name_tail...
     *
     *    name_tail:
     *        '.' name                             op: OP_MEMBER
     *        '.' string                           op: OP_MEMBER
     *        '[' add_expr ']'                     op: OP_SUBSCR
     * </pre>
     *
     * @since	1.1, 2001-03-24
     */

    private ExpressionTree parse_name_expr()
            throws ParseException
    {
        ExpressionTree	res;
        String	tok;
        String	keyword;
        char	ch;

        // Check for end of expression
        if (currentTokenIndex >= expressionTokens.length)
            return (null);

        // Continue parsing
        tok = expressionTokens[currentTokenIndex++];
        keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());

        ch = tok.charAt(0);
        if (keyword != null  ||
                (ch != '"'  &&  ch != '\''  &&
                        !Character.isJavaIdentifierPart(ch)))
            throw new ParseException("Missing name or string at: '" + tok + "'",
                    currentTokenIndex-1);

        res = new ExpressionTree(tok);

        // Parse 'name_tail...'
        for (;;)
        {
            // Check for end of expression
            if (currentTokenIndex >= expressionTokens.length)
                return (res);

            // Parse 'name_tail'
            tok = expressionTokens[currentTokenIndex++];
            keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());

            // Strip enclosing quotes if necessary
            if (keyword == ConstantHolder.OP_MEMBER  ||
                    keyword == ConstantHolder.OP_SUBSCR)
            {
                // Strip enclosing quotes from the operand token
                if (res.getValue() != null)
                {
                    tok = res.getValue();
                    ch = tok.charAt(0);
                    if (ch == '"'  ||  ch == '\'')
                        res = new ExpressionTree(tok.substring(1, tok.length()-1));
                }
            }

            // Parse 'name_tail'
            if (keyword == ConstantHolder.OP_MEMBER)
            {
                ExpressionTree	expr;

                // name_tail: '.' name|string
                tok = "<end>";
                if (currentTokenIndex < expressionTokens.length)
                {
                    tok = expressionTokens[currentTokenIndex++];
                    keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());
                }

                ch = tok.charAt(0);
                if (ch == '"'  ||  ch == '\'')
                    tok = tok.substring(1, tok.length()-1);
                else if (keyword != null  ||
                        !Character.isJavaIdentifierPart(ch))
                    throw new ParseException("Missing name or string at: '"
                            + tok + "'", currentTokenIndex-1);

                expr = new ExpressionTree(ConstantHolder.OP_MEMBER, res, new ExpressionTree(tok));
                res = expr;
            }
            else if (keyword == ConstantHolder.OP_SUBSCR)
            {
                ExpressionTree	expr;

                // name_tail: '[' add_expr ']'
                expr = new ExpressionTree(ConstantHolder.OP_SUBSCR, res, parse_add_expr());
                res = expr;

                // Consume a closing ']'
                tok = "<end>";
                keyword = null;
                if (currentTokenIndex < expressionTokens.length)
                {
                    tok = expressionTokens[currentTokenIndex++];
                    keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());
                }

                if (keyword != ConstantHolder.OP_SUBSCR2)
                    throw new ParseException("Missing closing ']' at: '"
                            + tok + "'", currentTokenIndex-1);
            }
            else
            {
                currentTokenIndex--;
                break;
            }
        }

        return (res);
    }

    /***************************************************************************
     * Parse a query subexpression, converting it into an expression subtree.
     * See {@link #parse_or_expr} for further details.
     *
     * <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
     * <p>
     * <b> Syntax </b>
     * <p>
     * <pre>
     *    expr_list:
     *        add_expr                          lookahead: ')'
     *        add_expr [','] expr_list          lookahead: ')', op: OP_LIST
     * </pre>
     *
     * @since	1.1, 2001-03-24
     */

    private ExpressionTree parse_expr_list()
            throws ParseException
    {
        ExpressionTree		res;
        String		tok;
        String		keyword;
        ExpressionTree	expr;

        // Parse 'add_expr'
        res = parse_add_expr();

        // Check for premature end of expression
        if (currentTokenIndex >= expressionTokens.length)
            throw new ParseException("Missing closing ')'", currentTokenIndex-1);

        // expressionTokens parsing
        tok = expressionTokens[currentTokenIndex++];
        keyword = (String) ConstantHolder.operatorsMap.get(tok.toLowerCase());

        expr = new ExpressionTree(ConstantHolder.OP_LIST, res);
        res = expr;

        // Look for end of expr_list, closing ')'
        if (keyword == ConstantHolder.OP_RP)
        {
            // expr_list: add_expr
            // End of operand list
            currentTokenIndex--;
        }
        else
        {
            // Parse an optional ','
            if (keyword == ConstantHolder.OP_LIST)
            {
                // Consume a ','
                if (currentTokenIndex >= expressionTokens.length)
                    throw new ParseException("Missing 'IN' list or ')' at: '"
                            + tok + "'", currentTokenIndex-1);
            }
            else
                currentTokenIndex--;

            // expr_list: add_expr [','] expr_list
            //  '(a, b, c)' -> {list a {list b c}}
            expr.setRight(parse_expr_list());
            res = expr;
        }

        return (res);
    }
    
    
}
