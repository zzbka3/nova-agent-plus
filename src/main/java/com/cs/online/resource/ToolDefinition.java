package com.cs.online.resource;

public record ToolDefinition(String id, String version, String name, String description) implements Resource {

    @Override
    public ResourceType type() {
        return ResourceType.TOOL;
    }
}
