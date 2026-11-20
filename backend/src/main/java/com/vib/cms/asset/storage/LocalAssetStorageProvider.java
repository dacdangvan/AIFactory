package com.vib.cms.asset.storage;

import com.vib.cms.asset.config.AssetStorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
@ConditionalOnProperty(prefix = "cms.asset-storage", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalAssetStorageProvider implements AssetStorageProvider {
    private final Path root;
    public LocalAssetStorageProvider(AssetStorageProperties properties) {
        this.root = Path.of(properties.getLocal().getRootDirectory()).toAbsolutePath().normalize();
    }
    private Path resolve(String key) throws IOException {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) throw new IOException("Invalid asset object key");
        return target;
    }
    @Override public String upload(InputStream input, String key, String contentType, long length) throws IOException {
        Path target=resolve(key); Files.createDirectories(target.getParent());
        Files.copy(input,target,StandardCopyOption.REPLACE_EXISTING); return key;
    }
    @Override public InputStream download(String key) throws IOException { return Files.newInputStream(resolve(key)); }
    @Override public void delete(String key) throws IOException { Files.deleteIfExists(resolve(key)); }
    @Override public boolean exists(String key) throws IOException { return Files.exists(resolve(key)); }
    @Override public String providerCode(){ return "LOCAL"; }
}
