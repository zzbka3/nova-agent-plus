package com.cs.online.demo;

import com.cs.online.runtime.tool.Tool;
import com.cs.online.runtime.tool.ToolResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Demo Tool：四则运算，用于验证 ToolRuntime / WorkflowRuntime / AgentRuntime 链路。
 * 入参：{"operator": "+|-|*|/", "a": number, "b": number}
 */
@Component
public class CalculatorTool implements Tool {

    public static final String ID = "calculator";

    @Override
    public String id() {
        return ID;
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
