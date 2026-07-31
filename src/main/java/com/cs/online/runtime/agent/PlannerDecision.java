package com.cs.online.runtime.agent;

/**
 * Planner 的决策结果：要么调用一个 Tool，要么直接给出最终回答。
 */
public record PlannerDecision(Type type, String toolId, String toolArgsJson, String finalAnswer) {

    public enum Type {
        ACTION,
        FINISH
    }

    public static PlannerDecision action(String toolId, String toolArgsJson) {
        return new PlannerDecision(Type.ACTION, toolId, toolArgsJson, null);
    }

    public static PlannerDecision finish(String finalAnswer) {
        return new PlannerDecision(Type.FINISH, null, null, finalAnswer);
    }
}
