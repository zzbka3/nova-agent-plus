package com.cs.online.runtime.workflow;

/**
 * config 语义按 type 区分：
 * TOOL  -> {"toolId": "...", "args": {...}}
 * AGENT -> {"agentId": "...", "input": "..."}
 * END   -> 无需 config
 */
public record Node(String id, NodeType type, com.fasterxml.jackson.databind.JsonNode config) {
}
