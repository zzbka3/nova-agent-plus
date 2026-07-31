package com.cs.online.api;

import com.cs.online.api.dto.ExecutionResponse;
import com.cs.online.api.dto.RunAgentRequest;
import com.cs.online.context.Context;
import com.cs.online.execution.Execution;
import com.cs.online.persistence.ExecutionPersister;
import com.cs.online.resource.AgentDefinition;
import com.cs.online.runtime.agent.AgentDefinitionService;
import com.cs.online.runtime.agent.AgentRuntime;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentDefinitionService agentDefinitionService;
    private final AgentRuntime agentRuntime;
    private final ExecutionPersister executionPersister;

    public AgentController(AgentDefinitionService agentDefinitionService,
                            AgentRuntime agentRuntime,
                            ExecutionPersister executionPersister) {
        this.agentDefinitionService = agentDefinitionService;
        this.agentRuntime = agentRuntime;
        this.executionPersister = executionPersister;
    }

    @PostMapping
    public AgentDefinition create(@RequestBody AgentDefinition definition) {
        return agentDefinitionService.create(definition);
    }

    @GetMapping
    public List<AgentDefinition> list() {
        return agentDefinitionService.list();
    }

    @GetMapping("/{id}")
    public AgentDefinition get(@PathVariable String id) {
        AgentDefinition agent = agentDefinitionService.get(id);
        if (agent == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found: " + id);
        }
        return agent;
    }

    @PutMapping("/{id}")
    public AgentDefinition update(@PathVariable String id, @RequestBody AgentDefinition definition) {
        return agentDefinitionService.update(id, definition);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        agentDefinitionService.delete(id);
    }

    @PostMapping("/{id}/run")
    public ExecutionResponse run(@PathVariable String id, @RequestBody RunAgentRequest request) {
        AgentDefinition agent = agentDefinitionService.get(id);
        if (agent == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found: " + id);
        }

        Context context = new Context();
        context.conversation().addUserMessage(request.input());

        Execution execution = agentRuntime.execute(agent, context);
        executionPersister.persistExecution(execution, agent.id());
        return ExecutionResponse.from(execution);
    }
}
