package com.cs.online.runtime.tool;

import com.cs.online.persistence.ResourceRecord;
import com.cs.online.persistence.ResourceRepository;
import com.cs.online.resource.ResourceType;
import com.cs.online.resource.ToolDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Tool 的元信息（name/description/parametersSchema）查询服务，数据来源是 resource_definition 表。
 * 只读：Tool 的 definition 由 Java 代码里的 Tool.definition() 决定，不允许通过 API 改写，
 * 落库这件事只在应用启动时由 DemoResourceInitializer 触发。
 */
@Service
public class ToolDefinitionService {

    private final ResourceRepository resourceRepository;
    private final ObjectMapper objectMapper;

    public ToolDefinitionService(ResourceRepository resourceRepository, ObjectMapper objectMapper) {
        this.resourceRepository = resourceRepository;
        this.objectMapper = objectMapper;
    }

    public ToolDefinition get(String id) {
        ResourceRecord record = resourceRepository.findById(id);
        if (record == null || !ResourceType.TOOL.name().equals(record.type())) {
            return null;
        }
        return deserialize(record.configJson());
    }

    public List<ToolDefinition> list() {
        return resourceRepository.findByType(ResourceType.TOOL.name()).stream()
                .map(record -> deserialize(record.configJson()))
                .toList();
    }

    private ToolDefinition deserialize(String configJson) {
        try {
            return objectMapper.readValue(configJson, ToolDefinition.class);
        } catch (Exception e) {
            throw new IllegalStateException("Corrupted ToolDefinition JSON in database: " + e.getMessage(), e);
        }
    }
}
