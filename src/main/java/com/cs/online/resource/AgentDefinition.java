package com.cs.online.resource;

import java.util.List;

public record AgentDefinition(
        String id,
        String version,
        String name,
        String model,
        String systemPrompt,
        List<String> tools,
        int maxSteps
) implements Resource {

    @Override
    public ResourceType type() {
        return ResourceType.AGENT;
    }
}
