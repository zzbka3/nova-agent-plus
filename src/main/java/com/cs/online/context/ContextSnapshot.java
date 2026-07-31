package com.cs.online.context;

import java.util.List;
import java.util.Map;

/**
 * Context 的可序列化快照，用于落库到 execution_record.context_json。
 * 不直接序列化 Context 本身，避免 Variable 里挂着的任意 Object（如 JsonNode）污染 JSON 结构。
 */
public record ContextSnapshot(
        List<Conversation.Message> conversation,
        Map<String, Object> variables,
        List<String> shortTermMemory,
        List<String> observationTraces
) {

    public static ContextSnapshot of(Context context) {
        Map<String, Object> variableValues = new java.util.LinkedHashMap<>();
        context.variables().all().forEach((name, variable) -> variableValues.put(name, variable.getValue()));

        return new ContextSnapshot(
                context.conversation().messages(),
                variableValues,
                context.memory().shortTerm(),
                context.observation().traces()
        );
    }
}
