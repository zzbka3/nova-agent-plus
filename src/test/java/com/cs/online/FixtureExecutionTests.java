package com.cs.online;

import com.cs.online.context.Context;
import com.cs.online.demo.CalculatorTool;
import com.cs.online.execution.Execution;
import com.cs.online.execution.ExecutionStatus;
import com.cs.online.fixtures.FixtureLoader;
import com.cs.online.resource.AgentDefinition;
import com.cs.online.resource.ToolDefinition;
import com.cs.online.resource.WorkflowDefinition;
import com.cs.online.runtime.agent.AgentRuntime;
import com.cs.online.runtime.tool.ToolDefinitionService;
import com.cs.online.runtime.workflow.WorkflowRuntime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 本地手工验证用：把 resources/fixtures 下的 JSON 落库，再触发 Runtime 执行。
 * 需要连接本机真实的 MySQL（见 application.yml），不是隔离的单元测试，用于代替手工 curl。
 */
@SpringBootTest
class FixtureExecutionTests {

    @Autowired
    private FixtureLoader fixtureLoader;

    @Autowired
    private WorkflowRuntime workflowRuntime;

    @Autowired
    private AgentRuntime agentRuntime;

    @Autowired
    private ToolDefinitionService toolDefinitionService;

    @Test
    void loadAllFixturesIntoDatabase() {
        var agentIds = fixtureLoader.loadAllAgents();
        var workflowIds = fixtureLoader.loadAllWorkflows();

        assertThat(agentIds).isNotEmpty();
        assertThat(workflowIds).isNotEmpty();
    }

    @Test
    void runDemoCalculatorWorkflow() {
        WorkflowDefinition workflow = fixtureLoader.loadWorkflow("demo-calculator-workflow.json");

        Execution execution = workflowRuntime.execute(workflow, new Context());

        assertThat(execution.status()).isEqualTo(ExecutionStatus.SUCCESS);
    }

    @Test
    void runIfBranchWorkflow() {
        WorkflowDefinition workflow = fixtureLoader.loadWorkflow("if-demo-workflow.json");

        Execution execution = workflowRuntime.execute(workflow, new Context());

        assertThat(execution.status()).isEqualTo(ExecutionStatus.SUCCESS);
        // 50 + 60 = 110 > 100 -> big branch -> 2 * 2 = 4
        assertThat(execution.context().observation().traces())
                .anyMatch(trace -> trace.contains("branch=big"));
    }

    @Test
    void runDemoCalculatorAgent() {
        AgentDefinition agent = fixtureLoader.loadAgent("demo-calculator-agent.json");

        Context context = new Context();
        context.conversation().addUserMessage("帮我计算 23 加 19 等于多少");

        Execution execution = agentRuntime.execute(agent, context);

        assertThat(execution.status()).isEqualTo(ExecutionStatus.SUCCESS);
    }

    @Test
    void calculatorToolDefinitionIsPersistedWithParametersSchema() {
        // ToolRuntime 应用启动时（DemoResourceInitializer）已经把 CalculatorTool.definition() 落库，
        // 这里验证元信息确实可以从数据库查到，且带上了参数 schema，而不是只有一个裸的 id。
        ToolDefinition tool = toolDefinitionService.get(CalculatorTool.ID);

        assertThat(tool).isNotNull();
        assertThat(tool.name()).isEqualTo("Calculator");
        assertThat(tool.description()).isNotBlank();
        assertThat(tool.parametersSchema()).isNotNull();
        assertThat(tool.parametersSchema().get("required").toString()).contains("operator", "a", "b");
    }

    @Test
    void listToolsIncludesCalculator() {
        var tools = toolDefinitionService.list();

        assertThat(tools).anyMatch(tool -> tool.id().equals(CalculatorTool.ID));
    }
}
