package com.iflytek.reservation.integration.aliyun;

import com.aliyun.facebody20191230.Client;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class AliyunClients {

    private static final String FACEBODY_ENDPOINT = "facebody.cn-shanghai.aliyuncs.com";
    private static final String OSS_BUCKET_ENV = "RESERVATION_OSS_BUCKET";
    private static final String OSS_ENDPOINT_ENV = "RESERVATION_OSS_ENDPOINT";
    private static final String DEFAULT_OSS_BUCKET = "reservation-signin-system";
    private static final String DEFAULT_OSS_ENDPOINT = "https://oss-cn-hangzhou.aliyuncs.com";

    private volatile Client faceClient;
    private volatile OSS ossClient;

    public Client faceClient() throws Exception {
        if (faceClient != null) {
            return faceClient;
        }
        synchronized (this) {
            if (faceClient != null) {
                return faceClient;
            }
            String accessKeyId = System.getenv("ALIBABA_CLOUD_ACCESS_KEY_ID");
            String accessKeySecret = System.getenv("ALIBABA_CLOUD_ACCESS_KEY_SECRET");
            if (accessKeyId == null || accessKeyId.isBlank() || accessKeySecret == null || accessKeySecret.isBlank()) {
                throw new IllegalStateException("阿里云 AccessKey 未配置");
            }
            Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret);
            config.endpoint = FACEBODY_ENDPOINT;
            faceClient = new Client(config);
            return faceClient;
        }
    }

    public OSS ossClient() {
        if (ossClient != null) {
            return ossClient;
        }
        synchronized (this) {
            if (ossClient != null) {
                return ossClient;
            }
            String accessKeyId = System.getenv("ALIBABA_CLOUD_ACCESS_KEY_ID");
            String accessKeySecret = System.getenv("ALIBABA_CLOUD_ACCESS_KEY_SECRET");
            if (accessKeyId == null || accessKeyId.isBlank() || accessKeySecret == null || accessKeySecret.isBlank()) {
                throw new IllegalStateException("阿里云 AccessKey 未配置");
            }
            ossClient = new OSSClientBuilder().build(ossEndpoint(), accessKeyId, accessKeySecret);
            return ossClient;
        }
    }

    public String ossBucket() {
        String bucket = System.getenv(OSS_BUCKET_ENV);
        if (bucket == null || bucket.isBlank()) {
            return DEFAULT_OSS_BUCKET;
        }
        return bucket.trim();
    }

    public String ossEndpointHost() {
        return URI.create(ossEndpoint()).getHost();
    }

    public String ossEndpoint() {
        String endpoint = System.getenv(OSS_ENDPOINT_ENV);
        if (endpoint == null || endpoint.isBlank()) {
            return DEFAULT_OSS_ENDPOINT;
        }
        endpoint = endpoint.trim();
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            endpoint = "https://" + endpoint;
        }
        return endpoint;
    }
}

