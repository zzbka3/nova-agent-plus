package com.cs.online.resource;

import com.cs.online.runtime.workflow.Edge;
import com.cs.online.runtime.workflow.Node;

import java.util.List;

public record WorkflowDefinition(String id, String version, String name, List<Node> nodes, List<Edge> edges)
        implements Resource {

    @Override
    public ResourceType type() {
        return ResourceType.WORKFLOW;
    }
}
