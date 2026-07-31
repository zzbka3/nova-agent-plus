package com.cs.online.runtime.tool;

public record ToolResult(boolean success, Object data, String errorMessage) {

    public static ToolResult success(Object data) {
        return new ToolResult(true, data, null);
    }

    public static ToolResult failure(String errorMessage) {
        return new ToolResult(false, null, errorMessage);
    }
}
