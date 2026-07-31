package com.cs.online.fixtures;

import com.cs.online.resource.AgentDefinition;
import com.cs.online.resource.WorkflowDefinition;
import com.cs.online.runtime.agent.AgentDefinitionService;
import com.cs.online.runtime.workflow.WorkflowDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 把 resources/fixtures 下的 JSON 定义文件读出来，落库到 resource_definition 表。
 * 用于测试/本地调试时批量准备 Agent/Workflow 数据，不在生产启动路径上自动触发。
 */
@Component
public class FixtureLoader {

    private static final String AGENTS_PATTERN = "classpath:fixtures/agents/*.json";
    private static final String WORKFLOWS_PATTERN = "classpath:fixtures/workflows/*.json";

    private final AgentDefinitionService agentDefinitionService;
    private final WorkflowDefinitionService workflowDefinitionService;
    private final ObjectMapper objectMapper;

    public FixtureLoader(AgentDefinitionService agentDefinitionService,
                          WorkflowDefinitionService workflowDefinitionService,
                          ObjectMapper objectMapper) {
        this.agentDefinitionService = agentDefinitionService;
        this.workflowDefinitionService = workflowDefinitionService;
        this.objectMapper = objectMapper;
    }

    /** 读取 fixtures/agents/*.json，逐个 create 或 update 到数据库，返回写入的 id 列表。 */
    public List<String> loadAllAgents() {
        List<String> ids = new ArrayList<>();
        for (Resource resource : readResources(AGENTS_PATTERN)) {
            AgentDefinition definition = readJson(resource, AgentDefinition.class);
            upsertAgent(definition);
            ids.add(definition.id());
        }
        return ids;
    }

    /** 读取 fixtures/workflows/*.json，逐个 create 或 update 到数据库，返回写入的 id 列表。 */
    public List<String> loadAllWorkflows() {
        List<String> ids = new ArrayList<>();
        for (Resource resource : readResources(WORKFLOWS_PATTERN)) {
            WorkflowDefinition definition = readJson(resource, WorkflowDefinition.class);
            upsertWorkflow(definition);
            ids.add(definition.id());
        }
        return ids;
    }

    public AgentDefinition loadAgent(String fileName) {
        Resource resource = singleResource("classpath:fixtures/agents/" + fileName);
        AgentDefinition definition = readJson(resource, AgentDefinition.class);
        upsertAgent(definition);
        return definition;
    }

    public WorkflowDefinition loadWorkflow(String fileName) {
        Resource resource = singleResource("classpath:fixtures/workflows/" + fileName);
        WorkflowDefinition definition = readJson(resource, WorkflowDefinition.class);
        upsertWorkflow(definition);
        return definition;
    }

    private void upsertAgent(AgentDefinition definition) {
        if (agentDefinitionService.get(definition.id()) == null) {
            agentDefinitionService.create(definition);
        } else {
            agentDefinitionService.update(definition.id(), definition);
        }
    }

    private void upsertWorkflow(WorkflowDefinition definition) {
        if (workflowDefinitionService.get(definition.id()) == null) {
            workflowDefinitionService.create(definition);
        } else {
            workflowDefinitionService.update(definition.id(), definition);
        }
    }

    private Resource[] readResources(String pattern) {
        try {
            return new PathMatchingResourcePatternResolver().getResources(pattern);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan fixtures at " + pattern + ": " + e.getMessage(), e);
        }
    }

    private Resource singleResource(String location) {
        return new PathMatchingResourcePatternResolver().getResource(location);
    }

    private <T> T readJson(Resource resource, Class<T> type) {
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readValue(in, type);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read fixture " + resource.getFilename() + ": " + e.getMessage(), e);
        }
    }
}
