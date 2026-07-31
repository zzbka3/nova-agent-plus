package com.cs.online.api;

import com.cs.online.resource.ToolDefinition;
import com.cs.online.runtime.tool.ToolDefinitionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final ToolDefinitionService toolDefinitionService;

    public ToolController(ToolDefinitionService toolDefinitionService) {
        this.toolDefinitionService = toolDefinitionService;
    }

    @GetMapping
    public List<ToolDefinition> list() {
        return toolDefinitionService.list();
    }

    @GetMapping("/{id}")
    public ToolDefinition get(@PathVariable String id) {
        ToolDefinition tool = toolDefinitionService.get(id);
        if (tool == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tool not found: " + id);
        }
        return tool;
    }
}
