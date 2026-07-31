package com.cs.online.runtime.tool;

import com.fasterxml.jackson.databind.JsonNode;

public interface Tool {

    String id();

    ToolResult invoke(JsonNode args);
}
