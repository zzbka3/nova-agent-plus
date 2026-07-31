package com.cs.online.runtime.workflow;

/**
 * branchKey 为 null 表示无条件边（普通节点的唯一出边）。
 * IF 节点的出边必须带 branchKey，特殊值 "default" 表示无分支命中时的兜底边。
 */
public record Edge(String from, String to, String branchKey) {

    public Edge(String from, String to) {
        this(from, to, null);
    }
}
