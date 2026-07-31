package com.cs.online.demo;

import com.cs.online.model.ModelProperties;
import com.cs.online.persistence.ResourceRepository;
import com.cs.online.resource.AgentDefinition;
import com.cs.online.resource.ToolDefinition;
import com.cs.online.resource.WorkflowDefinition;
import com.cs.online.runtime.agent.AgentDefinitionService;
import com.cs.online.runtime.tool.Tool;
import com.cs.online.runtime.tool.ToolRegistry;
import com.cs.online.runtime.workflow.Edge;
import com.cs.online.runtime.workflow.Node;
import com.cs.online.runtime.workflow.NodeType;
import com.cs.online.runtime.workflow.WorkflowDefinitionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 应用启动时：
 * 1. 把所有 Tool bean 注册进内存 ToolRegistry（执行用），并把其 definition() 落库（元信息展示/Planner 拼 prompt 用）
 * 2. 把 demo Agent / demo Workflow 的 Definition 通过 Service 写入数据库
 * （与用户通过 REST API 创建的资源走同一条路径，避免特殊路径产生分歧）。
 */
@Component
public class DemoResourceInitializer implements CommandLineRunner {

    public static final String DEMO_AGENT_ID = "demo-calculator-agent";
    public static final String DEMO_WORKFLOW_ID = "demo-calculator-workflow";

    private final ToolRegistry toolRegistry;
    private final List<Tool> tools;
    private final ModelProperties modelProperties;
    private final AgentDefinitionService agentDefinitionService;
    private final WorkflowDefinitionService workflowDefinitionService;
    private final ResourceRepository resourceRepository;
    private final ObjectMapper objectMapper;

    public DemoResourceInitializer(ToolRegistry toolRegistry,
                                    List<Tool> tools,
                                    ModelProperties modelProperties,
                                    AgentDefinitionService agentDefinitionService,
                                    WorkflowDefinitionService workflowDefinitionService,
                                    ResourceRepository resourceRepository,
                                    ObjectMapper objectMapper) {
        this.toolRegistry = toolRegistry;
        this.tools = tools;
        this.modelProperties = modelProperties;
        this.agentDefinitionService = agentDefinitionService;
        this.workflowDefinitionService = workflowDefinitionService;
        this.resourceRepository = resourceRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        for (Tool tool : tools) {
            toolRegistry.register(tool);
            registerToolDefinition(tool.definition());
        }

        if (agentDefinitionService.get(DEMO_AGENT_ID) == null) {
            AgentDefinition demoAgent = new AgentDefinition(
                    DEMO_AGENT_ID,
                    "1.0",
                    "Demo Calculator Agent",
                    modelProperties.getModelName(),
                    "You are a helpful agent that can perform arithmetic using the calculator tool when needed.",
                    List.of(CalculatorTool.ID),
                    5
            );
            agentDefinitionService.create(demoAgent);
        }

        if (workflowDefinitionService.get(DEMO_WORKFLOW_ID) == null) {
            workflowDefinitionService.create(buildDemoWorkflow());
        }
    }

    private void registerToolDefinition(ToolDefinition definition) throws com.fasterxml.jackson.core.JsonProcessingException {
        resourceRepository.save(definition, definition.name(), objectMapper.writeValueAsString(definition));
    }

    private WorkflowDefinition buildDemoWorkflow() {
        JsonNode defaultArgs = objectMapper.valueToTree(Map.of("operator", "+", "a", 1, "b", 2));
        JsonNode calcConfig = objectMapper.valueToTree(Map.of("toolId", CalculatorTool.ID, "args", defaultArgs));

        Node calcNode = new Node("calc", NodeType.TOOL, calcConfig);
        Node endNode = new Node("end", NodeType.END, objectMapper.createObjectNode());

        return new WorkflowDefinition(
                DEMO_WORKFLOW_ID,
                "1.0",
                "Demo Calculator Workflow",
                List.of(calcNode, endNode),
                List.of(new Edge("calc", "end"))
        );
    }
}
