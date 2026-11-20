package com.vib.cms.asset.storage;

import java.io.IOException;
import java.io.InputStream;

public interface AssetStorageProvider {
    String upload(InputStream input, String objectKey, String contentType, long contentLength) throws IOException;
    InputStream download(String objectKey) throws IOException;
    void delete(String objectKey) throws IOException;
    boolean exists(String objectKey) throws IOException;
    String providerCode();
}
