package com.cs.online.resource;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * parametersSchema 是 JSON Schema 的子集（type/properties/required），
 * 仅用于向模型和前端描述参数结构，ToolRuntime 不会用它做强制校验。
 */
public record ToolDefinition(
        String id,
        String version,
        String name,
        String description,
        JsonNode parametersSchema
) implements Resource {

    @Override
    public ResourceType type() {
        return ResourceType.TOOL;
    }
}
