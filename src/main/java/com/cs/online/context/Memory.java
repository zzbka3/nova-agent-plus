package com.cs.online.context;

import java.util.ArrayList;
import java.util.List;

/**
 * V1 占位实现：短期记忆先用消息列表，长期记忆留给 V2 MemoryRuntime。
 */
public class Memory {

    private final List<String> shortTerm = new ArrayList<>();

    public void remember(String fact) {
        shortTerm.add(fact);
    }

    public List<String> shortTerm() {
        return shortTerm;
    }
}
