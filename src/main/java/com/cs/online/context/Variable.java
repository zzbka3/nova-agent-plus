package com.cs.online.context;

public class Variable {

    private final String name;
    private final VariableType type;
    private final Object value;

    public Variable(String name, VariableType type, Object value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public VariableType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }
}
