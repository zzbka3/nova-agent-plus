package com.cs.online.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VariableManager {

    private final Map<String, Variable> variables = new ConcurrentHashMap<>();

    public void set(String name, VariableType type, Object value) {
        variables.put(name, new Variable(name, type, value));
    }

    public void set(Variable variable) {
        variables.put(variable.getName(), variable);
    }

    public Variable get(String name) {
        return variables.get(name);
    }

    public Object getValue(String name) {
        Variable variable = variables.get(name);
        return variable == null ? null : variable.getValue();
    }

    public Map<String, Variable> all() {
        return variables;
    }
}
