package com.vib.cms.asset.storage;

import com.vib.cms.asset.config.AssetStorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;

@Component
@ConditionalOnProperty(prefix = "cms.asset-storage", name = "provider", havingValue = "s3")
public class S3AssetStorageProvider implements AssetStorageProvider {
    private final AssetStorageProperties properties;
    private final S3Client client;
    public S3AssetStorageProvider(AssetStorageProperties properties) {
        this.properties=properties;
        this.client=S3Client.builder().region(Region.of(properties.getS3().getRegion())).build();
    }
    private String key(String value) {
        String prefix=properties.getS3().getPrefix();
        String clean=value.replaceAll("^/+", "");
        return prefix==null||prefix.isBlank()?clean:prefix.replaceAll("/+$","")+"/"+clean;
    }
    @Override public String upload(InputStream input,String objectKey,String contentType,long contentLength) throws IOException {
        String key=key(objectKey);
        PutObjectRequest request=PutObjectRequest.builder().bucket(properties.getS3().getBucket()).key(key)
            .contentType(contentType).serverSideEncryption(ServerSideEncryption.AES256).build();
        client.putObject(request,RequestBody.fromInputStream(input,contentLength)); return objectKey;
    }
    @Override public InputStream download(String objectKey) {
        ResponseInputStream<GetObjectResponse> stream=client.getObject(GetObjectRequest.builder()
            .bucket(properties.getS3().getBucket()).key(key(objectKey)).build());
        return stream;
    }
    @Override public void delete(String objectKey) {
        client.deleteObject(DeleteObjectRequest.builder().bucket(properties.getS3().getBucket()).key(key(objectKey)).build());
    }
    @Override public boolean exists(String objectKey) {
        try { client.headObject(HeadObjectRequest.builder().bucket(properties.getS3().getBucket()).key(key(objectKey)).build()); return true; }
        catch (S3Exception ex) { if (ex.statusCode()==404) return false; throw ex; }
    }
    @Override public String providerCode(){ return "S3"; }
}
