package com.cs.online.persistence;

import com.cs.online.context.ContextSnapshot;
import com.cs.online.execution.DefaultExecution;
import com.cs.online.execution.Execution;
import com.cs.online.resource.Resource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * 统一负责把 Resource 定义和 Execution 结果落库，供各 Controller 复用，
 * 避免每个 Controller 重复处理序列化细节。
 */
@Component
public class ExecutionPersister {

    private final ResourceRepository resourceRepository;
    private final ExecutionRepository executionRepository;
    private final ObjectMapper objectMapper;

    public ExecutionPersister(ResourceRepository resourceRepository,
                               ExecutionRepository executionRepository,
                               ObjectMapper objectMapper) {
        this.resourceRepository = resourceRepository;
        this.executionRepository = executionRepository;
        this.objectMapper = objectMapper;
    }

    public void persistResource(Resource resource, String name, Object config) {
        resourceRepository.save(resource, name, toJson(config));
    }

    public void persistExecution(Execution execution, String resourceId) {
        String contextJson = toJson(ContextSnapshot.of(execution.context()));
        String errorMessage = execution instanceof DefaultExecution de ? de.getErrorMessage() : null;
        executionRepository.save(execution, resourceId, contextJson, errorMessage);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{\"_serializationError\":\"" + e.getMessage() + "\"}";
        }
    }
}
