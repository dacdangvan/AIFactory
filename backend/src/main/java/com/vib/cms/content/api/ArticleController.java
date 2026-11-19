package com.vib.cms.content.api;

import com.vib.cms.common.api.ApiResponse;
import com.vib.cms.content.service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/content/articles")
public class ArticleController {
    private final ArticleService service;

    public ArticleController(ArticleService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ArticleResponse>> list(
            @RequestParam UUID productId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(service.list(productId, type, status, limit));
    }

    @GetMapping("/{id}")
    public ApiResponse<ArticleResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ArticleResponse> create(
            @Valid @RequestBody ArticleRequest request,
            @RequestParam UUID productId,
            Authentication authentication) {
        return ApiResponse.ok(service.create(productId, request, actor(authentication)));
    }

    @PutMapping("/{id}")
    public ApiResponse<ArticleResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ArticleRequest request,
            Authentication authentication) {
        return ApiResponse.ok(service.update(id, request, actor(authentication)));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<ArticleResponse> submit(@PathVariable UUID id, Authentication authentication) {
        return ApiResponse.ok(service.transition(id, "IN_REVIEW", actor(authentication)));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<ArticleResponse> approve(@PathVariable UUID id, Authentication authentication) {
        return ApiResponse.ok(service.transition(id, "APPROVED", actor(authentication)));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<ArticleResponse> publish(@PathVariable UUID id, Authentication authentication) {
        return ApiResponse.ok(service.transition(id, "PUBLISHED", actor(authentication)));
    }

    private String actor(Authentication authentication) {
        return authentication == null ? "local-dev" : authentication.getName();
    }
}