package com.cs.online.demo;

import com.cs.online.model.ModelProperties;
import com.cs.online.resource.AgentDefinition;
import com.cs.online.resource.WorkflowDefinition;
import com.cs.online.runtime.agent.AgentDefinitionService;
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
 * 应用启动时：注册 CalculatorTool（Java 实现，内存注册表），
 * 并把 demo Agent / demo Workflow 的 Definition 通过 Service 写入数据库
 * （与用户通过 REST API 创建的资源走同一条路径，避免特殊路径产生分歧）。
 */
@Component
public class DemoResourceInitializer implements CommandLineRunner {

    public static final String DEMO_AGENT_ID = "demo-calculator-agent";
    public static final String DEMO_WORKFLOW_ID = "demo-calculator-workflow";

    private final ToolRegistry toolRegistry;
    private final CalculatorTool calculatorTool;
    private final ModelProperties modelProperties;
    private final AgentDefinitionService agentDefinitionService;
    private final WorkflowDefinitionService workflowDefinitionService;
    private final ObjectMapper objectMapper;

    public DemoResourceInitializer(ToolRegistry toolRegistry,
                                    CalculatorTool calculatorTool,
                                    ModelProperties modelProperties,
                                    AgentDefinitionService agentDefinitionService,
                                    WorkflowDefinitionService workflowDefinitionService,
                                    ObjectMapper objectMapper) {
        this.toolRegistry = toolRegistry;
        this.calculatorTool = calculatorTool;
        this.modelProperties = modelProperties;
        this.agentDefinitionService = agentDefinitionService;
        this.workflowDefinitionService = workflowDefinitionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        toolRegistry.register(calculatorTool);

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
