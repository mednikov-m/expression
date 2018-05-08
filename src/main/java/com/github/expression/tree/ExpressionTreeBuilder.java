package com.github.expression.tree;

import com.github.expression.exception.ParseException;
import com.github.expression.token.TokenExtractor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ExpressionTreeBuilder {

    private List<String> compareOperators = Arrays.asList(ConstantHolder.OP_EQ, ConstantHolder.OP_NE, ConstantHolder.OP_LT,
            ConstantHolder.OP_LE, ConstantHolder.OP_GT, ConstantHolder.OP_GE,
            ConstantHolder.OP_CONTAINS, ConstantHolder.OP_LIKE, ConstantHolder.OP_LIKEFILE);

    private List<String> addOperators = Arrays.asList(ConstantHolder.OP_ADD, ConstantHolder.OP_SUB, ConstantHolder.OP_CONCAT);

    private List<String> mulDivModOperators = Arrays.asList(ConstantHolder.OP_MUL, ConstantHolder.OP_DIV, ConstantHolder.OP_MOD);

    private TokenExtractor tokenExtractor;

    private String[] expressionTokens;
    private int currentTokenIndex;

    ExpressionTreeBuilder() {
        this.tokenExtractor = new TokenExtractor();
    }

    public ExpressionTreeBuilder withExpression(String expr) {
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
        ExpressionTree result = parseAddExpression();

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
            result = new ExpressionTree(keyword, result, parseAddExpression());
        } else if (Objects.equals(keyword, ConstantHolder.OP_BETWEEN)) {
            ExpressionTree	sub;
            ExpressionTree	lo;
            ExpressionTree	hi;
            lo = parseAddExpression();
            token = "<end>";
            keyword = null;
            if (currentTokenIndex < expressionTokens.length) {
                token = expressionTokens[currentTokenIndex++];
                keyword = ConstantHolder.operatorsMap.get(token.toLowerCase());
            }

            if (!Objects.equals(keyword, ConstantHolder.OP_AND)) {
                throw new ParseException("Missing expected 'AND' at: '" + token + "'", currentTokenIndex - 1);
            }
            hi = parseAddExpression();
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

            expr.setRight(parseExpressionList());

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


    private ExpressionTree parseAddExpression() throws ParseException {
        ExpressionTree result = parseMultiplyDivideModExpresson();
        while (currentTokenIndex < expressionTokens.length) {
            String token = expressionTokens[currentTokenIndex++];
            String keyword = ConstantHolder.operatorsMap.get(token.toLowerCase());
            if (keyword == null)
                throw new ParseException("Bad operator: '" + token + "'", currentTokenIndex-1);
            if (addOperators.contains(keyword)) {
                result = new ExpressionTree(keyword, result, parseMultiplyDivideModExpresson());
            } else {
                currentTokenIndex--;
                return result;
            }
        }
        return result;
    }

    private ExpressionTree parseMultiplyDivideModExpresson() throws ParseException {
        ExpressionTree result = parseUnaryExpression();

        while (currentTokenIndex < expressionTokens.length) {
            String token = expressionTokens[currentTokenIndex++];
            String keyword = ConstantHolder.operatorsMap.get(token.toLowerCase());
            if (keyword == null)
                throw new ParseException("Bad operator: '" + token + "'", currentTokenIndex-1);

            if (mulDivModOperators.contains(keyword)) {
                result = new ExpressionTree(keyword, result, parseUnaryExpression());
            } else {
                currentTokenIndex--;
                return result;
            }
        }
        return result;
    }

    private ExpressionTree parseUnaryExpression() throws ParseException {
        ExpressionTree result;

        if (currentTokenIndex >= expressionTokens.length)
            throw new ParseException("Missing operand/operator", currentTokenIndex-1);

        String token = expressionTokens[currentTokenIndex++];
        String keyword = ConstantHolder.operatorsMap.get(token.toLowerCase());

        if (Objects.equals(keyword, ConstantHolder.OP_ADD) ||
                Objects.equals(keyword, ConstantHolder.OP_SUB)) {
            result = new ExpressionTree(Objects.equals(keyword, ConstantHolder.OP_ADD) ? ConstantHolder.OP_POS : ConstantHolder.OP_NEG,
                    parseUnaryExpression());
        } else {
            currentTokenIndex--;
            result = parseExpoExpression();
        }

        return result;
    }

    private ExpressionTree parseExpoExpression() throws ParseException {
        ExpressionTree result = parseOperand();

        if (currentTokenIndex >= expressionTokens.length)
            return result;

        String token = expressionTokens[currentTokenIndex++];
        String keyword = ConstantHolder.operatorsMap.get(token.toLowerCase());

        if (keyword == null) {
            throw new ParseException("Bad operator: '" + result + "'", currentTokenIndex-1);
        }

        // Parse an expo-op
        if (Objects.equals(keyword, ConstantHolder.OP_EXPO)) {
            result = new ExpressionTree(keyword, result, parseUnaryExpression());
        }
        else
            currentTokenIndex--;

        return result;
    }


    private ExpressionTree parseOperand() throws ParseException {
        ExpressionTree	res;
        // Check for end of expression
        if (currentTokenIndex >= expressionTokens.length)
            return null;


        String token = expressionTokens[currentTokenIndex++];
        String keyword = ConstantHolder.operatorsMap.get(token.toLowerCase());

        if (keyword == null) {
            char ch = token.charAt(0);
            if (ch == '"'  ||  ch == '\''  ||
                    Character.isJavaIdentifierPart(ch)) {
                currentTokenIndex--;
                return (parseNameExpression());
            } else {
                return new ExpressionTree(token);
            }
        } else if (Objects.equals(keyword, ConstantHolder.OP_NULL)) {
            return new ExpressionTree(token);
        }
        else if (Objects.equals(keyword, ConstantHolder.OP_LP)) {
            res = parseOrExpression();
            token = "<end>";
            keyword = null;
            if (currentTokenIndex < expressionTokens.length) {
                token = expressionTokens[currentTokenIndex++];
                keyword = ConstantHolder.operatorsMap.get(token.toLowerCase());
            }

            if (!Objects.equals(keyword, ConstantHolder.OP_RP))
                throw new ParseException("Missing closing ')' at: '" + token + "'", currentTokenIndex-1);
        } else {
            throw new ParseException("Bad operand: '" + token + "'", currentTokenIndex-1);
        }

        return res;
    }

    private ExpressionTree parseNameExpression()
            throws ParseException
    {
        ExpressionTree res;

        if (currentTokenIndex >= expressionTokens.length)
            return null;

        String token = expressionTokens[currentTokenIndex++];
        String keyword = (String) ConstantHolder.operatorsMap.get(token.toLowerCase());

        char ch = token.charAt(0);
        if (keyword != null  ||
                (ch != '"'  &&  ch != '\''  &&
                        !Character.isJavaIdentifierPart(ch)))
            throw new ParseException("Missing name or string at: '" + token + "'",
                    currentTokenIndex-1);

        res = new ExpressionTree(token);

        while (currentTokenIndex < expressionTokens.length) {
            token = expressionTokens[currentTokenIndex++];
            keyword = ConstantHolder.operatorsMap.get(token.toLowerCase());

            if (Objects.equals(keyword, ConstantHolder.OP_MEMBER) ||
                    Objects.equals(keyword, ConstantHolder.OP_SUBSCR)) {
                if (res.getValue() != null) {
                    token = res.getValue();
                    ch = token.charAt(0);
                    if (ch == '"'  ||  ch == '\'')
                        res = new ExpressionTree(token.substring(1, token.length()-1));
                }
            }


            if (Objects.equals(keyword, ConstantHolder.OP_MEMBER)) {

                token = "<end>";
                if (currentTokenIndex < expressionTokens.length) {
                    token = expressionTokens[currentTokenIndex++];
                    keyword = ConstantHolder.operatorsMap.get(token.toLowerCase());
                }

                ch = token.charAt(0);
                if (ch == '"'  ||  ch == '\'')
                    token = token.substring(1, token.length()-1);
                else if (keyword != null  ||
                        !Character.isJavaIdentifierPart(ch))
                    throw new ParseException("Missing name or string at: '" + token + "'", currentTokenIndex-1);
                res  = new ExpressionTree(ConstantHolder.OP_MEMBER, res, new ExpressionTree(token));
            }
            else if (Objects.equals(keyword, ConstantHolder.OP_SUBSCR)) {
                res  = new ExpressionTree(ConstantHolder.OP_SUBSCR, res, parseAddExpression());

                token = "<end>";
                keyword = null;
                if (currentTokenIndex < expressionTokens.length) {
                    token = expressionTokens[currentTokenIndex++];
                    keyword = ConstantHolder.operatorsMap.get(token.toLowerCase());
                }

                if (!Objects.equals(keyword, ConstantHolder.OP_SUBSCR2))
                    throw new ParseException("Missing closing ']' at: '" + token + "'", currentTokenIndex-1);
            } else {
                currentTokenIndex--;
                break;
            }
        }

        return res;
    }


    private ExpressionTree parseExpressionList() throws ParseException {
        ExpressionTree res = parseAddExpression();

        if (currentTokenIndex >= expressionTokens.length)
            throw new ParseException("Missing closing ')'", currentTokenIndex-1);

        String tok = expressionTokens[currentTokenIndex++];
        String keyword = ConstantHolder.operatorsMap.get(tok.toLowerCase());

        res  = new ExpressionTree(ConstantHolder.OP_LIST, res);

        if (Objects.equals(keyword, ConstantHolder.OP_RP)) {
            currentTokenIndex--;
        } else {
            if (Objects.equals(keyword, ConstantHolder.OP_LIST)) {
                if (currentTokenIndex >= expressionTokens.length)
                    throw new ParseException("Missing 'IN' list or ')' at: '" + tok + "'", currentTokenIndex-1);
            }
            else
                currentTokenIndex--;
            res.setRight(parseExpressionList());
        }

        return res;
    }
    
    
}
