package com.vib.cms.theme.api;
import com.vib.cms.common.api.ApiResponse;import com.vib.cms.theme.service.ThemeService;import jakarta.validation.Valid;import org.springframework.http.HttpStatus;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("/api/v1/themes") public class ThemeController {
 private final ThemeService service;public ThemeController(ThemeService service){this.service=service;}
 @GetMapping public ApiResponse<List<ThemeResponse>> list(@RequestParam(required=false) UUID productId){return ApiResponse.ok(service.list(productId));}
 @GetMapping("/{id}") public ApiResponse<ThemeResponse> get(@PathVariable UUID id){return ApiResponse.ok(service.get(id));}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApiResponse<ThemeResponse> create(@Valid @RequestBody ThemeRequest r,Authentication a){return ApiResponse.ok(service.create(r,actor(a)));}
 @PutMapping("/{id}") public ApiResponse<ThemeResponse> update(@PathVariable UUID id,@Valid @RequestBody ThemeRequest r,Authentication a){return ApiResponse.ok(service.update(id,r,actor(a)));}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void archive(@PathVariable UUID id){service.archive(id);}
 private String actor(Authentication a){return a==null?"local-dev":a.getName();}
}
