package com.cs.online.resource;

import com.cs.online.runtime.tool.ToolRegistry;
import com.cs.online.runtime.workflow.Edge;
import com.cs.online.runtime.workflow.Node;
import com.cs.online.runtime.workflow.NodeType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 前端 JSON 落库后立刻可执行，因此在写入前做最小校验：
 * 引用的 tool 必须已在 ToolRegistry 注册，Workflow 的 Node/Edge 必须自洽。
 * 不做过度校验（如 JSON Schema），只挡住"落库即崩"的明显错误。
 */
@Component
public class DefinitionValidator {

    private final ToolRegistry toolRegistry;

    public DefinitionValidator(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public void validate(AgentDefinition agent) {
        if (agent.id() == null || agent.id().isBlank()) {
            throw new DefinitionValidationException("Agent id must not be blank");
        }
        if (agent.model() == null || agent.model().isBlank()) {
            throw new DefinitionValidationException("Agent model must not be blank");
        }
        if (agent.tools() != null) {
            for (String toolId : agent.tools()) {
                if (toolRegistry.get(toolId) == null) {
                    throw new DefinitionValidationException("Agent references unknown tool: " + toolId);
                }
            }
        }
    }

    public void validate(WorkflowDefinition workflow) {
        if (workflow.id() == null || workflow.id().isBlank()) {
            throw new DefinitionValidationException("Workflow id must not be blank");
        }
        if (workflow.nodes() == null || workflow.nodes().isEmpty()) {
            throw new DefinitionValidationException("Workflow must have at least one node");
        }

        Set<String> nodeIds = new HashSet<>();
        Map<String, Node> nodesById = new HashMap<>();
        for (Node node : workflow.nodes()) {
            if (node.id() == null || node.id().isBlank()) {
                throw new DefinitionValidationException("Node id must not be blank");
            }
            if (!nodeIds.add(node.id())) {
                throw new DefinitionValidationException("Duplicate node id: " + node.id());
            }
            nodesById.put(node.id(), node);

            if (node.type() == NodeType.TOOL) {
                if (node.config() == null || !node.config().hasNonNull("toolId")) {
                    throw new DefinitionValidationException("TOOL node requires config.toolId: " + node.id());
                }
                String toolId = node.config().get("toolId").asText();
                if (toolRegistry.get(toolId) == null) {
                    throw new DefinitionValidationException("Node " + node.id() + " references unknown tool: " + toolId);
                }
            }
            if (node.type() == NodeType.IF) {
                if (node.config() == null || !node.config().hasNonNull("expression")
                        || node.config().get("expression").asText().isBlank()) {
                    throw new DefinitionValidationException("IF node requires non-blank config.expression: " + node.id());
                }
            }
        }

        List<Edge> edges = workflow.edges() == null ? List.of() : workflow.edges();
        for (Edge edge : edges) {
            if (!nodeIds.contains(edge.from()) || !nodeIds.contains(edge.to())) {
                throw new DefinitionValidationException("Edge references unknown node: " + edge.from() + " -> " + edge.to());
            }
        }

        Map<String, List<Edge>> edgesByFrom = new HashMap<>();
        for (Edge edge : edges) {
            edgesByFrom.computeIfAbsent(edge.from(), k -> new java.util.ArrayList<>()).add(edge);
        }

        for (Node node : workflow.nodes()) {
            if (node.type() == NodeType.END) {
                continue;
            }
            List<Edge> outgoing = edgesByFrom.getOrDefault(node.id(), List.of());

            if (node.type() == NodeType.IF) {
                Set<String> branchKeys = new HashSet<>();
                for (Edge edge : outgoing) {
                    if (edge.branchKey() == null || edge.branchKey().isBlank()) {
                        throw new DefinitionValidationException("IF node " + node.id() + " has an edge without branchKey: -> " + edge.to());
                    }
                    if (!branchKeys.add(edge.branchKey())) {
                        throw new DefinitionValidationException("IF node " + node.id() + " has duplicate branchKey: " + edge.branchKey());
                    }
                }
            } else {
                if (outgoing.size() > 1) {
                    throw new DefinitionValidationException("Node " + node.id() + " has multiple outgoing edges but is not an IF node");
                }
            }
        }
    }
}
