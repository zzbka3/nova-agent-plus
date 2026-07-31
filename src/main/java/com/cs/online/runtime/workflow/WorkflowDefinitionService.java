package com.cs.online.runtime.workflow;

import com.cs.online.persistence.ResourceRecord;
import com.cs.online.persistence.ResourceRepository;
import com.cs.online.resource.DefinitionValidationException;
import com.cs.online.resource.DefinitionValidator;
import com.cs.online.resource.ResourceType;
import com.cs.online.resource.WorkflowDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * WorkflowDefinition 完全由数据库驱动，不做内存缓存，与 AgentDefinitionService 对称。
 */
@Service
public class WorkflowDefinitionService {

    private final ResourceRepository resourceRepository;
    private final DefinitionValidator validator;
    private final ObjectMapper objectMapper;

    public WorkflowDefinitionService(ResourceRepository resourceRepository,
                                      DefinitionValidator validator,
                                      ObjectMapper objectMapper) {
        this.resourceRepository = resourceRepository;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    public WorkflowDefinition create(WorkflowDefinition definition) {
        validator.validate(definition);
        save(definition);
        return definition;
    }

    public WorkflowDefinition update(String id, WorkflowDefinition definition) {
        if (!id.equals(definition.id())) {
            throw new DefinitionValidationException("Path id and body id must match: " + id + " != " + definition.id());
        }
        if (get(id) == null) {
            throw new DefinitionValidationException("Workflow not found: " + id);
        }
        validator.validate(definition);
        save(definition);
        return definition;
    }

    public WorkflowDefinition get(String id) {
        ResourceRecord record = resourceRepository.findById(id);
        if (record == null || !ResourceType.WORKFLOW.name().equals(record.type())) {
            return null;
        }
        return deserialize(record.configJson());
    }

    public List<WorkflowDefinition> list() {
        return resourceRepository.findByType(ResourceType.WORKFLOW.name()).stream()
                .map(record -> deserialize(record.configJson()))
                .toList();
    }

    public void delete(String id) {
        resourceRepository.delete(id);
    }

    private void save(WorkflowDefinition definition) {
        try {
            resourceRepository.save(definition, definition.name(), objectMapper.writeValueAsString(definition));
        } catch (Exception e) {
            throw new DefinitionValidationException("Failed to serialize WorkflowDefinition: " + e.getMessage());
        }
    }

    private WorkflowDefinition deserialize(String configJson) {
        try {
            return objectMapper.readValue(configJson, WorkflowDefinition.class);
        } catch (Exception e) {
            throw new IllegalStateException("Corrupted WorkflowDefinition JSON in database: " + e.getMessage(), e);
        }
    }
}
