package com.vib.cms.product.api;

import com.vib.cms.common.api.ApiResponse;
import com.vib.cms.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.list(page, size, keyword, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> create(
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {
        return ApiResponse.ok(service.create(request, actor(authentication)));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {
        return ApiResponse.ok(service.update(id, request, actor(authentication)));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<ProductResponse> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ProductStatusRequest request,
            Authentication authentication) {
        return ApiResponse.ok(service.changeStatus(id, request.status(), actor(authentication)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id, Authentication authentication) {
        service.archive(id, actor(authentication));
    }

    private String actor(Authentication authentication) {
        return authentication == null ? "local-dev" : authentication.getName();
    }
}
