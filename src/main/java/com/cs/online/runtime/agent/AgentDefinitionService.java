package com.cs.online.runtime.agent;

import com.cs.online.persistence.ResourceRecord;
import com.cs.online.persistence.ResourceRepository;
import com.cs.online.resource.AgentDefinition;
import com.cs.online.resource.DefinitionValidationException;
import com.cs.online.resource.DefinitionValidator;
import com.cs.online.resource.ResourceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AgentDefinition 完全由数据库驱动，不做内存缓存：
 * 前端改了配置立刻生效，代价是每次 run 多一次数据库查询，V1 规模下可接受。
 */
@Service
public class AgentDefinitionService {

    private final ResourceRepository resourceRepository;
    private final DefinitionValidator validator;
    private final ObjectMapper objectMapper;

    public AgentDefinitionService(ResourceRepository resourceRepository,
                                   DefinitionValidator validator,
                                   ObjectMapper objectMapper) {
        this.resourceRepository = resourceRepository;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    public AgentDefinition create(AgentDefinition definition) {
        validator.validate(definition);
        save(definition);
        return definition;
    }

    public AgentDefinition update(String id, AgentDefinition definition) {
        if (!id.equals(definition.id())) {
            throw new DefinitionValidationException("Path id and body id must match: " + id + " != " + definition.id());
        }
        if (get(id) == null) {
            throw new DefinitionValidationException("Agent not found: " + id);
        }
        validator.validate(definition);
        save(definition);
        return definition;
    }

    public AgentDefinition get(String id) {
        ResourceRecord record = resourceRepository.findById(id);
        if (record == null || !ResourceType.AGENT.name().equals(record.type())) {
            return null;
        }
        return deserialize(record.configJson());
    }

    public List<AgentDefinition> list() {
        return resourceRepository.findByType(ResourceType.AGENT.name()).stream()
                .map(record -> deserialize(record.configJson()))
                .toList();
    }

    public void delete(String id) {
        resourceRepository.delete(id);
    }

    private void save(AgentDefinition definition) {
        try {
            resourceRepository.save(definition, definition.name(), objectMapper.writeValueAsString(definition));
        } catch (Exception e) {
            throw new DefinitionValidationException("Failed to serialize AgentDefinition: " + e.getMessage());
        }
    }

    private AgentDefinition deserialize(String configJson) {
        try {
            return objectMapper.readValue(configJson, AgentDefinition.class);
        } catch (Exception e) {
            throw new IllegalStateException("Corrupted AgentDefinition JSON in database: " + e.getMessage(), e);
        }
    }
}
