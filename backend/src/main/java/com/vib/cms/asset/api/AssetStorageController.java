package com.vib.cms.asset.api;

import com.vib.cms.asset.config.AssetStorageProperties;
import com.vib.cms.asset.storage.AssetStorageProvider;
import com.vib.cms.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/asset-storage")
public class AssetStorageController {
    private final AssetStorageProperties properties;
    private final AssetStorageProvider provider;
    public AssetStorageController(AssetStorageProperties properties, AssetStorageProvider provider) {
        this.properties=properties; this.provider=provider;
    }
    @GetMapping
    public ApiResponse<Map<String,Object>> configuration() {
        Map<String,Object> local=Map.of("rootDirectory",properties.getLocal().getRootDirectory(),
                "publicUrlPrefix",properties.getLocal().getPublicUrlPrefix());
        Map<String,Object> s3=Map.of("bucket",properties.getS3().getBucket(),
                "region",properties.getS3().getRegion(),"prefix",properties.getS3().getPrefix(),
                "publicUrlPrefix",properties.getS3().getPublicUrlPrefix());
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("provider",properties.getProvider()); result.put("activeProvider",provider.providerCode());
        result.put("local",local); result.put("s3",s3);
        result.put("configurationSource","environment");
        return ApiResponse.ok(result);
    }
}
