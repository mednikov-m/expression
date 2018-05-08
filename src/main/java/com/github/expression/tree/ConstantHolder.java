package com.github.expression.tree;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

class ConstantHolder {
    static final String OP_NOP = "nop"; 
     static final String OP_VALUE = "val";
     static final String OP_NOT = "not";
     static final String OP_AND = "and";
     static final String OP_OR = "or";
     static final String OP_LIST = "list";
     static final String OP_IS = "is";
     static final String OP_EQ = "eq";
     static final String OP_NE = "ne";
     static final String OP_LT = "lt";
     static final String OP_LE = "le";
     static final String OP_GT = "gt";
     static final String OP_GE = "ge";
     static final String OP_EXPO = "exp";
     static final String OP_MUL = "mul";
     static final String OP_DIV = "div";
     static final String OP_MOD = "mod";
     static final String OP_ADD = "add";
     static final String OP_SUB = "sub";
     static final String OP_CONCAT = "concat";
     static final String OP_POS = "pos";
     static final String OP_NEG = "neg";
     static final String OP_MEMBER = "member";
     static final String OP_SUBSCR = "subscr";
     static final String OP_CONTAINS = "contains";
     static final String OP_LIKE = "like";
     static final String OP_LIKEFILE = "likefile"; 
     static final String OP_IN = "in";
     static final String OP_BETWEEN = "between";
     static final String OP_NULL = "null";

     static final String OP_LP = "(";
     static final String OP_RP = ")";
     static final String OP_SUBSCR2 = "]";

    static final Map<String, String> operatorsMap;
    static final Map<String, String> reversedOperatorsMap;
    
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

       reversedOperatorsMap = inverseMap(operatorsMap).entrySet()
               .stream()
               .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
    }

    public static <Key, Value> Map<Value, List<Key>> inverseMap(Map<Key, Value> map) {
        return map.entrySet().stream()
                .collect(groupingBy(Map.Entry::getValue, mapping(Map.Entry::getKey, toList())));
    }
}
