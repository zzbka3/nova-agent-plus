package com.cs.online.runtime.agent;

import com.cs.online.model.ModelRuntime;
import com.cs.online.resource.AgentDefinition;
import com.cs.online.resource.ToolDefinition;
import com.cs.online.runtime.tool.ToolDefinitionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Planner 用固定的 ReAct 文本协议驱动模型输出下一步动作：
 * 要求模型只回复以下两种格式之一：
 *   ACTION: {"tool": "toolId", "args": {...}}
 *   FINISH: <final answer text>
 * system prompt 里会把每个可用 tool 的完整参数 schema 拼进去，让模型知道具体要传什么参数，
 * 而不是只给一个 tool id 列表让模型瞎猜。
 */
@Component
public class Planner {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ModelRuntime modelRuntime;
    private final ToolDefinitionService toolDefinitionService;

    public Planner(ModelRuntime modelRuntime, ToolDefinitionService toolDefinitionService) {
        this.modelRuntime = modelRuntime;
        this.toolDefinitionService = toolDefinitionService;
    }

    public PlannerDecision decide(AgentDefinition agent, String taskInput, String observationSoFar) {
        String systemPrompt = buildSystemPrompt(agent);
        String userPrompt = observationSoFar == null || observationSoFar.isEmpty()
                ? taskInput
                : taskInput + "\n\nObservation so far:\n" + observationSoFar;

        String raw = modelRuntime.chat(systemPrompt, userPrompt).strip();
        return parse(raw);
    }

    private String buildSystemPrompt(AgentDefinition agent) {
        StringBuilder sb = new StringBuilder();
        if (agent.systemPrompt() != null) {
            sb.append(agent.systemPrompt()).append("\n\n");
        }
        sb.append("You are operating under the ReAct protocol. Reply with EXACTLY ONE of the following formats:\n")
                .append("ACTION: {\"tool\": \"<toolId>\", \"args\": {...}}\n")
                .append("FINISH: <final answer>\n")
                .append("Available tools:\n")
                .append(describeTools(agent))
                .append("Do not output anything else besides one of these two lines.");
        return sb.toString();
    }

    private String describeTools(AgentDefinition agent) {
        if (agent.tools() == null || agent.tools().isEmpty()) {
            return "(none)\n";
        }
        StringBuilder sb = new StringBuilder();
        for (String toolId : agent.tools()) {
            ToolDefinition definition = toolDefinitionService.get(toolId);
            if (definition == null) {
                continue;
            }
            sb.append("- ").append(definition.id()).append(": ").append(definition.description()).append("\n")
                    .append("  Parameters (JSON Schema): ").append(definition.parametersSchema()).append("\n");
        }
        return sb.length() == 0 ? "(none)\n" : sb.toString();
    }

    private PlannerDecision parse(String raw) {
        if (raw.startsWith("FINISH:")) {
            return PlannerDecision.finish(raw.substring("FINISH:".length()).strip());
        }
        if (raw.startsWith("ACTION:")) {
            String json = raw.substring("ACTION:".length()).strip();
            try {
                JsonNode node = MAPPER.readTree(json);
                String tool = node.get("tool").asText();
                String argsJson = node.get("args").toString();
                return PlannerDecision.action(tool, argsJson);
            } catch (Exception e) {
                return PlannerDecision.finish("Planner produced invalid ACTION payload: " + raw);
            }
        }
        // 模型未遵循协议时，直接当作最终回答返回，避免死循环
        return PlannerDecision.finish(raw);
    }
}
