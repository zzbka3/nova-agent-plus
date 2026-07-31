package com.cs.online.api;

import com.cs.online.api.dto.ExecutionResponse;
import com.cs.online.context.Context;
import com.cs.online.execution.Execution;
import com.cs.online.persistence.ExecutionPersister;
import com.cs.online.resource.WorkflowDefinition;
import com.cs.online.runtime.workflow.WorkflowDefinitionService;
import com.cs.online.runtime.workflow.WorkflowRuntime;
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
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowDefinitionService workflowDefinitionService;
    private final WorkflowRuntime workflowRuntime;
    private final ExecutionPersister executionPersister;

    public WorkflowController(WorkflowDefinitionService workflowDefinitionService,
                               WorkflowRuntime workflowRuntime,
                               ExecutionPersister executionPersister) {
        this.workflowDefinitionService = workflowDefinitionService;
        this.workflowRuntime = workflowRuntime;
        this.executionPersister = executionPersister;
    }

    @PostMapping
    public WorkflowDefinition create(@RequestBody WorkflowDefinition definition) {
        return workflowDefinitionService.create(definition);
    }

    @GetMapping
    public List<WorkflowDefinition> list() {
        return workflowDefinitionService.list();
    }

    @GetMapping("/{id}")
    public WorkflowDefinition get(@PathVariable String id) {
        WorkflowDefinition workflow = workflowDefinitionService.get(id);
        if (workflow == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found: " + id);
        }
        return workflow;
    }

    @PutMapping("/{id}")
    public WorkflowDefinition update(@PathVariable String id, @RequestBody WorkflowDefinition definition) {
        return workflowDefinitionService.update(id, definition);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        workflowDefinitionService.delete(id);
    }

    @PostMapping("/{id}/run")
    public ExecutionResponse run(@PathVariable String id) {
        WorkflowDefinition workflow = workflowDefinitionService.get(id);
        if (workflow == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found: " + id);
        }

        Context context = new Context();
        Execution execution = workflowRuntime.execute(workflow, context);
        executionPersister.persistExecution(execution, workflow.id());
        return ExecutionResponse.from(execution);
    }
}
