package com.cs.online.demo;

import com.cs.online.resource.ToolDefinition;
import com.cs.online.runtime.tool.Tool;
import com.cs.online.runtime.tool.ToolResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Demo Tool：四则运算，用于验证 ToolRuntime / WorkflowRuntime / AgentRuntime 链路。
 * 入参：{"operator": "+|-|*|/", "a": number, "b": number}
 */
@Component
public class CalculatorTool implements Tool {

    public static final String ID = "calculator";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ToolDefinition definition() {
        JsonNode schema = MAPPER.valueToTree(Map.of(
                "type", "object",
                "properties", Map.of(
                        "operator", Map.of("type", "string", "description", "运算符：+ - * /"),
                        "a", Map.of("type", "number", "description", "第一个操作数"),
                        "b", Map.of("type", "number", "description", "第二个操作数")
                ),
                "required", List.of("operator", "a", "b")
        ));
        return new ToolDefinition(ID, "1.0", "Calculator", "四则运算工具", schema);
    }

    @Override
    public ToolResult invoke(JsonNode args) {
        if (!args.hasNonNull("operator") || !args.hasNonNull("a") || !args.hasNonNull("b")) {
            return ToolResult.failure("missing required fields: operator, a, b");
        }
        String operator = args.get("operator").asText();
        double a = args.get("a").asDouble();
        double b = args.get("b").asDouble();

        double result;
        switch (operator) {
            case "+" -> result = a + b;
            case "-" -> result = a - b;
            case "*" -> result = a * b;
            case "/" -> {
                if (b == 0) {
                    return ToolResult.failure("division by zero");
                }
                result = a / b;
            }
            default -> {
                return ToolResult.failure("unsupported operator: " + operator);
            }
        }
        return ToolResult.success(result);
    }
}
