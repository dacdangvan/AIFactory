package com.vib.cms.product.api;
import com.vib.cms.common.api.ApiResponse;import com.vib.cms.product.service.ProductService;import jakarta.validation.Valid;import org.springframework.http.HttpStatus;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("/api/v1/products") public class ProductController {
 private final ProductService service;public ProductController(ProductService service){this.service=service;}
 @GetMapping public ApiResponse<List<ProductResponse>> list(){return ApiResponse.ok(service.list());}
 @GetMapping("/{id}") public ApiResponse<ProductResponse> get(@PathVariable UUID id){return ApiResponse.ok(service.get(id));}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductRequest r,Authentication a){return ApiResponse.ok(service.create(r,actor(a)));}
 @PutMapping("/{id}") public ApiResponse<ProductResponse> update(@PathVariable UUID id,@Valid @RequestBody ProductRequest r,Authentication a){return ApiResponse.ok(service.update(id,r,actor(a)));}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void archive(@PathVariable UUID id){service.archive(id);}
 private String actor(Authentication a){return a==null?"local-dev":a.getName();}
}
