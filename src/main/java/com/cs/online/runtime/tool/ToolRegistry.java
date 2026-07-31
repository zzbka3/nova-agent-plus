package com.cs.online.runtime.tool;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ToolRegistry {

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    public void register(Tool tool) {
        tools.put(tool.id(), tool);
    }

    public Tool get(String toolId) {
        return tools.get(toolId);
    }

    public Map<String, Tool> all() {
        return tools;
    }
}
