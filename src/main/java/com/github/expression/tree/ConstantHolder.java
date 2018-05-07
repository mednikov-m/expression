package com.github.expression.tree;

import java.util.HashMap;
import java.util.Map;

class ConstantHolder {
    public static final String OP_NOP = "nop"; 
    public static final String OP_VALUE = "val";
    public static final String OP_NOT = "not";
    public static final String OP_AND = "and";
    public static final String OP_OR = "or";
    public static final String OP_LIST = "list";
    public static final String OP_IS = "is";
    public static final String OP_EQ = "eq";
    public static final String OP_NE = "ne";
    public static final String OP_LT = "lt";
    public static final String OP_LE = "le";
    public static final String OP_GT = "gt";
    public static final String OP_GE = "ge";
    public static final String OP_EXPO = "exp";
    public static final String OP_MUL = "mul";
    public static final String OP_DIV = "div";
    public static final String OP_MOD = "mod";
    public static final String OP_ADD = "add";
    public static final String OP_SUB = "sub";
    public static final String OP_CONCAT = "concat";
    public static final String OP_POS = "pos";
    public static final String OP_NEG = "neg";
    public static final String OP_MEMBER = "member";
    public static final String OP_SUBSCR = "subscr";
    public static final String OP_CONTAINS = "contains";
    public static final String OP_LIKE = "like";
    public static final String OP_LIKEFILE = "likefile"; 
    public static final String OP_IN = "in";
    public static final String OP_BETWEEN = "between";
    public static final String OP_NULL = "null";

    public static final String OP_LP = "(";
    public static final String OP_RP = ")";
    public static final String OP_SUBSCR2 = "]";

    public static final Map<String, String> operatorsMap;
    
    static {
       operatorsMap = new HashMap<>();
       operatorsMap.put("not", OP_NOT);
       operatorsMap.put("and", OP_AND);
       operatorsMap.put("or", OP_OR);
       operatorsMap.put("&", OP_AND);
       operatorsMap.put("|", OP_OR);
       operatorsMap.put("(", OP_LP);
       operatorsMap.put(")", OP_RP);
       operatorsMap.put(",", OP_LIST);
       operatorsMap.put("is", OP_IS);
       operatorsMap.put("=", OP_EQ);
       operatorsMap.put("!=", OP_NE);
       operatorsMap.put("<>", OP_NE);
       operatorsMap.put("<", OP_LT);
       operatorsMap.put("<=", OP_LE);
       operatorsMap.put(">", OP_GT);
       operatorsMap.put(">=", OP_GE);
       operatorsMap.put("**", OP_EXPO);
       operatorsMap.put("*", OP_MUL);
       operatorsMap.put("/", OP_DIV);
       operatorsMap.put("mod", OP_MOD);
       operatorsMap.put("+", OP_ADD);
       operatorsMap.put("-", OP_SUB);
       operatorsMap.put("||", OP_CONCAT);
       operatorsMap.put(".", OP_MEMBER);
       operatorsMap.put("[", OP_SUBSCR);
       operatorsMap.put("]", OP_SUBSCR2);
       operatorsMap.put("contains", OP_CONTAINS);
       operatorsMap.put("like", OP_LIKE);
       operatorsMap.put("likefile", OP_LIKEFILE);
       operatorsMap.put("in", OP_IN);
       operatorsMap.put("between", OP_BETWEEN);
       operatorsMap.put("null", OP_NULL);
    }
}
