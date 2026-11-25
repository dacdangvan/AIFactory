package com.vib.cms.content.api;

import com.vib.cms.common.api.ApiResponse;
import com.vib.cms.content.service.PageManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class PageManagementController {
    private final PageManagementService service;
    public PageManagementController(PageManagementService service) { this.service=service; }

    @GetMapping("/pages")
    public ApiResponse<List<Map<String,Object>>> tree(@RequestParam UUID productId) { return ApiResponse.ok(service.tree(productId)); }

    @GetMapping("/pages/{id}")
    public ApiResponse<Map<String,Object>> get(@PathVariable UUID id) { return ApiResponse.ok(service.get(id)); }

    @PostMapping("/pages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String,Object>> create(@RequestParam UUID productId,@RequestBody Map<String,Object> body,Authentication auth) {
        return ApiResponse.ok(service.create(productId,body,actor(auth)));
    }

    @PutMapping("/pages/{id}")
    public ApiResponse<Map<String,Object>> update(@PathVariable UUID id,@RequestBody Map<String,Object> body,Authentication auth) {
        return ApiResponse.ok(service.update(id,body,actor(auth)));
    }

    @DeleteMapping("/pages/{id}")
    public ApiResponse<Void> archive(@PathVariable UUID id) { service.archive(id); return ApiResponse.ok(null); }

    @GetMapping("/pages/{id}/sections")
    public ApiResponse<List<Map<String,Object>>> sections(@PathVariable UUID id) { return ApiResponse.ok(service.sections(id)); }

    @PutMapping("/pages/{id}/sections")
    public ApiResponse<List<Map<String,Object>>> saveSections(@PathVariable UUID id,@RequestBody Map<String,Object> body) {
        Object raw=body.get("sections");
        if(!(raw instanceof List<?> list)) throw new IllegalArgumentException("sections must be an array");
        List<Map<String,Object>> sections=new ArrayList<>();
        for(Object item:list) if(item instanceof Map<?,?> map) {
            Map<String,Object> value=new LinkedHashMap<>(); map.forEach((k,v)->value.put(String.valueOf(k),v)); sections.add(value);
        }
        return ApiResponse.ok(service.saveSections(id,sections));
    }

    @GetMapping("/themes")
    public ApiResponse<List<Map<String,Object>>> themes(@RequestParam UUID productId) { return ApiResponse.ok(service.themes(productId)); }

    @PostMapping("/themes")
    @PutMapping("/themes")
    public ApiResponse<Map<String,Object>> saveTheme(@RequestParam UUID productId,@RequestBody Map<String,Object> body,Authentication auth) {
        return ApiResponse.ok(service.saveTheme(productId,body,actor(auth)));
    }
    private String actor(Authentication auth) { return auth==null?"local-dev":auth.getName(); }
}
