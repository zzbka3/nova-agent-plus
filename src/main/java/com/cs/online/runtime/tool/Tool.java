package com.cs.online.runtime.tool;

import com.cs.online.resource.ToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;

public interface Tool {

    String id();

    ToolDefinition definition();

    ToolResult invoke(JsonNode args);
}
