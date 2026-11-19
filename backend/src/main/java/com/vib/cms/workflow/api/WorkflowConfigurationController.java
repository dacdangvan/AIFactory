package com.vib.cms.workflow.api;

import com.vib.cms.common.api.ApiResponse;
import com.vib.cms.workflow.service.WorkflowConfigurationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowConfigurationController {
    private final WorkflowConfigurationService service;
    public WorkflowConfigurationController(WorkflowConfigurationService service) { this.service=service; }

    @GetMapping
    public ApiResponse<List<Map<String,Object>>> list(@RequestParam(required=false) UUID productId) {
        return ApiResponse.ok(service.list(productId));
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String,Object>> create(@RequestBody Map<String,Object> definition, Authentication auth) {
        return ApiResponse.ok(service.create(definition, auth==null?"local-dev":auth.getName()));
    }
}
