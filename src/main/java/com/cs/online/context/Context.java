package com.cs.online.context;

public class Context {

    private final Conversation conversation = new Conversation();
    private final VariableManager variables = new VariableManager();
    private final Memory memory = new Memory();
    private final Observation observation = new Observation();
    private final RuntimeInfo runtime = new RuntimeInfo();

    public Conversation conversation() {
        return conversation;
    }

    public VariableManager variables() {
        return variables;
    }

    public Memory memory() {
        return memory;
    }

    public Observation observation() {
        return observation;
    }

    public RuntimeInfo runtime() {
        return runtime;
    }
}
