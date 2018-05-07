package com.github.expression.service;

import com.github.expression.model.ExpressionTree;
import com.github.expression.util.ConstantHolder;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ExpressionTreeBuilder {

    private TokenExtractor tokenExtractor;
    private List<String> compareOperators = Arrays.asList(ConstantHolder.OP_EQ, ConstantHolder.OP_NE, ConstantHolder.OP_LT,
            ConstantHolder.OP_LE, ConstantHolder.OP_GT, ConstantHolder.OP_GE,
            ConstantHolder.OP_CONTAINS, ConstantHolder.OP_LIKE, ConstantHolder.OP_LIKEFILE);

    private String[] expressionTokens;
    private int currentTokenIndex;

    public ExpressionTreeBuilder() {
        this.tokenExtractor = new TokenExtractor();
    }

    public ExpressionTreeBuilder withExpression(String expr) throws ParseException {
        List<String> tokens = tokenExtractor.getTokens(expr);
        if (tokens.isEmpty()) {
            throw new ParseException("Tokens not found", -1);
        }
        expressionTokens = tokens.toArray(new String[tokens.size()]);
        currentTokenIndex = 0;
        return this;
    }

    public ExpressionTree build() throws ParseException {
        ExpressionTree tree = parseOrExpression();
        validateEndOfExpression();
        return tree.getValue() != null ? new ExpressionTree(ConstantHolder.OP_VALUE, tree) : tree;
    }

    private void validateEndOfExpression() throws ParseException {
        if (currentTokenIndex < expressionTokens.length) {
            throw new ParseException("Malformed expression at: '"
                    + expressionTokens[currentTokenIndex] + "'", currentTokenIndex);
        }
    }

    private ExpressionTree parseOrExpression() throws ParseException {
        ExpressionTree result = parseAndExpression();

        while (currentTokenIndex < expressionTokens.length) {
            String	token = expressionTokens[currentTokenIndex++];
            String	keyword = ConstantHolder.operatorsMap.get(token.toLowerCase());
            if (keyword == null) {
                throw new ParseException("Bad operator: '" + token + "'", currentTokenIndex-1);
            }

            if (Objects.equals(keyword, ConstantHolder.OP_OR)) {
                result = new ExpressionTree(keyword, result, parseAndExpression());
            } else {
                currentTokenIndex--;
                return result;
            }
        }
        return result;
    }


    private ExpressionTree parseAndExpression() throws ParseException {
        ExpressionTree result = parseNotExpression();

        while (currentTokenIndex < expressionTokens.length) {
            String token = expressionTokens[currentTokenIndex++];
            String keyword = ConstantHolder.operatorsMap.get(token.toLowerCase());
            if (keyword == null) {
                throw new ParseException("Bad operator: '" + token + "'", currentTokenIndex-1);
            }

            if (Objects.equals(keyword, ConstantHolder.OP_AND)) {
                result = new ExpressionTree(keyword, result, parseNotExpression());
            } else {
                currentTokenIndex--;
                return result;
            }
        }
        return result;
    }


    private ExpressionTree parseNotExpression() throws ParseException {
        ExpressionTree result;
        if (currentTokenIndex >= expressionTokens.length) {
            return null;
        }
        String token = expressionTokens[currentTokenIndex++];
        String keyword = ConstantHolder.operatorsMap.get(token.toLowerCase());

        if (Objects.equals(keyword, ConstantHolder.OP_NOT)) {
            result = new ExpressionTree(keyword, parseNotExpression());
        } else {
            currentTokenIndex--;
            result = parseCompareExpression();
        }
        return result;
    }

    private ExpressionTree parseCompareExpression() throws ParseException {
        ExpressionTree result = parse_add_expr();

        if (currentTokenIndex >= expressionTokens.length) {
            return result;
        }

        // Continue parsing
        String token = expressionTokens[currentTokenIndex++];
        String keyword = ConstantHolder.operatorsMap.get(token.toLowerCase());

        if (keyword == null) {
            throw new ParseException("Bad operator: '" + token + "'", currentTokenIndex-1);
        }

        if (Objects.equals(keyword, ConstantHolder.OP_IS)) {

            if (currentTokenIndex < expressionTokens.length) {
                token = expressionTokens[currentTokenIndex++];
            } else {
                throw new ParseException("Missing operand following: '" + token + "'", currentTokenIndex-1);
            }
            keyword = ConstantHolder.operatorsMap.get(token.toLowerCase());

            result = new ExpressionTree(ConstantHolder.OP_IS, result, new ExpressionTree(ConstantHolder.OP_NULL));

            if (Objects.equals(keyword, ConstantHolder.OP_NOT)) {
                if (currentTokenIndex < expressionTokens.length) {
                    token = expressionTokens[currentTokenIndex++];
                } else {
                    throw new ParseException("Missing 'NULL' following: '" + token + "'", currentTokenIndex-1);
                }
                keyword = ConstantHolder.operatorsMap.get(token.toLowerCase());

                result = new ExpressionTree(ConstantHolder.OP_NOT, result);
            }

            if (!Objects.equals(keyword, ConstantHolder.OP_NULL)) {
                throw new ParseException("Missing 'NULL' at: '" + token + "'", currentTokenIndex-1);
            }

            return result;
        }

        boolean isCompl = false;
        if (Objects.equals(keyword, ConstantHolder.OP_NOT)) {
            if (currentTokenIndex < expressionTokens.length) {
                token = expressionTokens[currentTokenIndex++];
            } else {
                throw new ParseException("Missing operator following: '" + token + "'", currentTokenIndex-1);
            }
            isCompl = true;
            keyword = ConstantHolder.operatorsMap.get(token.toLowerCase());
        }

        // Parse a compare-op
        if (compareOperators.contains(keyword)) {
            result = new ExpressionTree(keyword, result, parse_add_expr());
        } else if (Objects.equals(keyword, ConstantHolder.OP_BETWEEN)) {
            ExpressionTree	sub;
            ExpressionTree	lo;
            ExpressionTree	hi;
            lo = parse_add_expr();
            token = "<end>";
            keyword = null;
            if (currentTokenIndex < expressionTokens.length) {
                token = expressionTokens[currentTokenIndex++];
                keyword = ConstantHolder.operatorsMap.get(token.toLowerCase());
            }

            if (!Objects.equals(keyword, ConstantHolder.OP_AND)) {
                throw new ParseException("Missing expected 'AND' at: '" + token + "'", currentTokenIndex-1);
            }
            hi = parse_add_expr();
            sub = new ExpressionTree(ConstantHolder.OP_AND, lo, hi);
            result = new ExpressionTree(ConstantHolder.OP_BETWEEN, result, sub);
        } else if (Objects.equals(keyword, ConstantHolder.OP_IN)) {
            ExpressionTree	expr;

            expr = new ExpressionTree(ConstantHolder.OP_IN, result);

            token = "<end>";
            keyword = null;
            if (currentTokenIndex < expressionTokens.length) {
                token = expressionTokens[currentTokenIndex++];
                keyword = ConstantHolder.operatorsMap.get(token.toLowerCase());
            }

            if (!Objects.equals(keyword, ConstantHolder.OP_LP)) {
                throw new ParseException("Missing expected '(' at: '" + token + "'", currentTokenIndex-1);
            }

            expr.setRight(parse_expr_list());

            token = "<end>";
            keyword = null;
            if (currentTokenIndex < expressionTokens.length) {
                token = expressionTokens[currentTokenIndex++];
                keyword = ConstantHolder.operatorsMap.get(token.toLowerCase());
            }

            if (!Objects.equals(keyword, ConstantHolder.OP_RP)) {
                throw new ParseException("Missing expected ')' at: '"
                        + token + "'", currentTokenIndex-1);
            }

            result = expr;
        }
        else {
            currentTokenIndex--;
        }

        if (isCompl) {
            result = new ExpressionTree(ConstantHolder.OP_NOT, result);
        }

        return result;
    }


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
     * See {@link #parseOrExpression} for further details.
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
     * See {@link #parseOrExpression} for further details.
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
     * See {@link #parseOrExpression} for further details.
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
     * See {@link #parseOrExpression} for further details.
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
            res = parseOrExpression();

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
     * See {@link #parseOrExpression} for further details.
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
     * See {@link #parseOrExpression} for further details.
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
