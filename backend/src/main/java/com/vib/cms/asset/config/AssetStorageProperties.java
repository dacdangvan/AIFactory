package com.vib.cms.asset.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cms.asset-storage")
public class AssetStorageProperties {
    private String provider = "local";
    private final Local local = new Local();
    private final S3 s3 = new S3();
    public String getProvider(){ return provider; }
    public void setProvider(String provider){ this.provider = provider; }
    public Local getLocal(){ return local; }
    public S3 getS3(){ return s3; }
    public static class Local {
        private String rootDirectory = "./storage/assets";
        private String publicUrlPrefix = "/api/v1/media";
        public String getRootDirectory(){ return rootDirectory; }
        public void setRootDirectory(String value){ rootDirectory=value; }
        public String getPublicUrlPrefix(){ return publicUrlPrefix; }
        public void setPublicUrlPrefix(String value){ publicUrlPrefix=value; }
    }
    public static class S3 {
        private String bucket = "";
        private String region = "ap-southeast-1";
        private String prefix = "cms/assets";
        private String publicUrlPrefix = "";
        public String getBucket(){ return bucket; }
        public void setBucket(String value){ bucket=value; }
        public String getRegion(){ return region; }
        public void setRegion(String value){ region=value; }
        public String getPrefix(){ return prefix; }
        public void setPrefix(String value){ prefix=value; }
        public String getPublicUrlPrefix(){ return publicUrlPrefix; }
        public void setPublicUrlPrefix(String value){ publicUrlPrefix=value; }
    }
}
