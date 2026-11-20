package com.vib.cms.content.api;

import com.vib.cms.common.api.ApiResponse;
import com.vib.cms.content.service.ContentConfigurationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ContentConfigurationController {
    private final ContentConfigurationService service;
    public ContentConfigurationController(ContentConfigurationService service) { this.service = service; }

    @GetMapping("/content-types")
    public ApiResponse<List<Map<String,Object>>> contentTypes(@RequestParam UUID productId) {
        return ApiResponse.ok(service.listContentTypes(productId));
    }
    @PutMapping("/content-types/{id}")
    public ApiResponse<Map<String,Object>> updateContentType(@PathVariable UUID id, @RequestBody Map<String,Object> body, Authentication auth) {
        return ApiResponse.ok(service.updateContentType(id, body, actor(auth)));
    }
    @PatchMapping("/content-types/{id}/status")
    public ApiResponse<Map<String,Object>> contentTypeStatus(@PathVariable UUID id, @RequestBody Map<String,Object> body) {
        return ApiResponse.ok(service.setContentTypeStatus(id, String.valueOf(body.get("status"))));
    }
    @DeleteMapping("/content-types/{id}")
    public ApiResponse<Map<String,Object>> archiveContentType(@PathVariable UUID id) {
        return ApiResponse.ok(service.setContentTypeStatus(id, "ARCHIVED"));
    }
    @PostMapping("/content-types")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String,Object>> createContentType(@RequestParam UUID productId,
            @RequestBody Map<String,Object> body, Authentication auth) {
        return ApiResponse.ok(service.createContentType(productId, body, actor(auth)));
    }
    @GetMapping("/authoring-templates")
    public ApiResponse<List<Map<String,Object>>> templates(@RequestParam UUID productId) {
        return ApiResponse.ok(service.listTemplates(productId));
    }
    @PutMapping("/authoring-templates/{id}")
    public ApiResponse<Map<String,Object>> updateTemplate(@PathVariable UUID id, @RequestBody Map<String,Object> body, Authentication auth) {
        return ApiResponse.ok(service.updateTemplate(id, body, actor(auth)));
    }
    @PatchMapping("/authoring-templates/{id}/status")
    public ApiResponse<Map<String,Object>> templateStatus(@PathVariable UUID id, @RequestBody Map<String,Object> body) {
        return ApiResponse.ok(service.setTemplateStatus(id, String.valueOf(body.get("status"))));
    }
    @DeleteMapping("/authoring-templates/{id}")
    public ApiResponse<Map<String,Object>> archiveTemplate(@PathVariable UUID id) {
        return ApiResponse.ok(service.setTemplateStatus(id, "ARCHIVED"));
    }
    @PostMapping("/authoring-templates")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String,Object>> createTemplate(@RequestParam UUID productId,
            @RequestBody Map<String,Object> body, Authentication auth) {
        return ApiResponse.ok(service.createTemplate(productId, body, actor(auth)));
    }
    @PostMapping("/authoring-templates/{templateId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String,Object>> createTemplateVersion(@PathVariable UUID templateId,
            @RequestBody Map<String,Object> body, Authentication auth) {
        return ApiResponse.ok(service.createTemplateVersion(templateId, body, actor(auth)));
    }
    @GetMapping("/authoring-templates/{templateId}/versions")
    public ApiResponse<List<Map<String,Object>>> templateVersions(@PathVariable UUID templateId) {
        return ApiResponse.ok(service.listTemplateVersions(templateId));
    }
    @PostMapping("/products/{productId}/workflow-bindings")
    public ApiResponse<Map<String,Object>> bindWorkflow(@PathVariable UUID productId,
            @RequestBody Map<String,Object> body, Authentication auth) {
        UUID contentTypeId = body.get("contentTypeId") == null || String.valueOf(body.get("contentTypeId")).isBlank()
                ? null : UUID.fromString(String.valueOf(body.get("contentTypeId")));
        return ApiResponse.ok(service.bindWorkflow(productId, contentTypeId,
                UUID.fromString(String.valueOf(body.get("workflowVersionId"))),
                String.valueOf(body.get("bindingType")), actor(auth)));
    }
    @PostMapping("/content-types/{contentTypeId}/template-bindings")
    public ApiResponse<Map<String,Object>> bindTemplate(@PathVariable UUID contentTypeId,
            @RequestBody Map<String,Object> body, Authentication auth) {
        return ApiResponse.ok(service.bindTemplate(contentTypeId,
                UUID.fromString(String.valueOf(body.get("templateVersionId"))), actor(auth)));
    }
    private String actor(Authentication auth) { return auth == null ? "local-dev" : auth.getName(); }
}
