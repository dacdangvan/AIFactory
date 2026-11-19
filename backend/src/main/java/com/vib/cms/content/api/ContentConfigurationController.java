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
    @PostMapping("/content-types/{contentTypeId}/template-bindings")
    public ApiResponse<Map<String,Object>> bindTemplate(@PathVariable UUID contentTypeId,
            @RequestBody Map<String,Object> body, Authentication auth) {
        return ApiResponse.ok(service.bindTemplate(contentTypeId,
                UUID.fromString(String.valueOf(body.get("templateVersionId"))), actor(auth)));
    }
    private String actor(Authentication auth) { return auth == null ? "local-dev" : auth.getName(); }
}
