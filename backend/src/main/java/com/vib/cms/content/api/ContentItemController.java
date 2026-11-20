package com.vib.cms.content.api;

import com.vib.cms.common.api.ApiResponse;
import com.vib.cms.content.service.ContentItemService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/content-items")
public class ContentItemController {
    private final ContentItemService service;
    public ContentItemController(ContentItemService service) { this.service=service; }

    @GetMapping
    public ApiResponse<List<Map<String,Object>>> list(@RequestParam UUID productId,
            @RequestParam(required=false) UUID contentTypeId,
            @RequestParam(required=false) String keyword,
            @RequestParam(defaultValue="100") int limit,Authentication authentication) {
        return ApiResponse.ok(service.list(productId,contentTypeId,keyword,limit,actor(authentication)));
    }

    @GetMapping("/authoring-definition")
    public ApiResponse<Map<String,Object>> definition(@RequestParam UUID contentTypeId) {
        return ApiResponse.ok(service.getAuthoringDefinition(contentTypeId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String,Object>> get(@PathVariable UUID id,Authentication authentication) {
        return ApiResponse.ok(service.getForActor(id,actor(authentication)));
    }

    @GetMapping("/{id}/authoring-definition")
    public ApiResponse<Map<String,Object>> definitionForItem(@PathVariable UUID id,Authentication authentication) {
        return ApiResponse.ok(service.getAuthoringDefinitionForItem(id,actor(authentication)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String,Object>> create(@RequestParam UUID productId,
            @RequestParam UUID contentTypeId, @RequestBody Map<String,Object> body,
            Authentication authentication) {
        return ApiResponse.ok(service.create(productId,contentTypeId,body,actor(authentication)));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String,Object>> update(@PathVariable UUID id,
            @RequestBody Map<String,Object> body, Authentication authentication) {
        return ApiResponse.ok(service.update(id,body,actor(authentication)));
    }

    @GetMapping("/{id}/workflow-actions")
    public ApiResponse<Map<String,Object>> workflowActions(@PathVariable UUID id,Authentication authentication) {
        return ApiResponse.ok(service.workflowActions(id,actor(authentication)));
    }

    @PostMapping("/{id}/workflow-actions")
    public ApiResponse<Map<String,Object>> executeWorkflowAction(@PathVariable UUID id,
            @RequestBody Map<String,Object> body,Authentication authentication) {
        return ApiResponse.ok(service.executeWorkflowAction(id,body,actor(authentication)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> archive(@PathVariable UUID id,Authentication authentication) {
        service.archive(id,actor(authentication));
        return ApiResponse.ok(null);
    }

    private String actor(Authentication authentication) {
        return authentication==null?"local-dev":authentication.getName();
    }
}
