package com.vib.cms.asset.api;

import com.vib.cms.asset.service.MediaService;
import com.vib.cms.asset.storage.AssetStorageProvider;
import com.vib.cms.common.api.ApiResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {
    private final MediaService service;
    private final AssetStorageProvider storage;

    public MediaController(MediaService service, AssetStorageProvider storage) {
        this.service = service;
        this.storage = storage;
    }

    @GetMapping
    public ApiResponse<List<Map<String,Object>>> list(@RequestParam(required=false) UUID productId,
            @RequestParam(required=false) String keyword, @RequestParam(defaultValue="100") int limit) {
        return ApiResponse.ok(service.list(productId, keyword, limit));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String,Object>> upload(@RequestPart("file") MultipartFile file,
            @RequestParam(required=false) UUID productId, Authentication authentication) throws IOException {
        return ApiResponse.ok(service.upload(file, productId, authentication==null?"local-dev":authentication.getName()));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID id) throws IOException {
        MediaService.StoredAsset asset = service.getForDownload(id);
        var stream = storage.download(asset.objectKey());
        String disposition = asset.contentType().startsWith("image/") ? "inline" : "attachment";
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(asset.contentType()))
            .contentLength(asset.size())
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.builder(disposition)
                .filename(asset.filename()).build().toString())
            .cacheControl(CacheControl.noCache())
            .body(new InputStreamResource(stream));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
