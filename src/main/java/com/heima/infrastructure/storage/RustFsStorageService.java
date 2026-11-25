package com.heima.infrastructure.storage;

import com.heima.exception.ServiceException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

@Service
public class RustFsStorageService implements ObjectStorageService {
    private final RustFsProperties properties;
    private volatile S3Client client;
    private volatile S3Presigner presigner;

    public RustFsStorageService(RustFsProperties properties) {
        this.properties = properties;
    }

    @Override
    public void ensureAvailable() {
        if (!properties.isConfigured()) {
            throw new ServiceException(503, "对象存储未配置");
        }
    }

    @Override
    public void upload(String bucket, String key, byte[] content, String contentType) {
        ensureAvailable();
        client().putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(), RequestBody.fromBytes(content));
    }

    @Override
    public byte[] download(String bucket, String key) {
        ensureAvailable();
        try (var response = client().getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())) {
            return response.readAllBytes();
        } catch (java.io.IOException e) {
            throw new ServiceException(502, "读取对象存储失败", e);
        }
    }

    @Override
    public InputStream openStream(String bucket, String key) {
        ensureAvailable();
        return client().getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @Override
    public void delete(String bucket, String key) {
        ensureAvailable();
        client().deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @Override
    public void copy(String sourceBucket, String sourceKey, String targetBucket, String targetKey) {
        ensureAvailable();
        client().copyObject(CopyObjectRequest.builder().sourceBucket(sourceBucket).sourceKey(sourceKey)
                .destinationBucket(targetBucket).destinationKey(targetKey).build());
    }

    @Override
    public String presignGet(String bucket, String key, Duration expire) {
        ensureAvailable();
        Duration effective = expire == null ? properties.getPresignExpire() : expire;
        if (effective.compareTo(Duration.ofMinutes(10)) > 0) {
            throw new ServiceException(400, "预签名地址有效期不能超过10分钟");
        }
        return presigner().presignGetObject(GetObjectPresignRequest.builder().signatureDuration(effective)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build()).build()).url().toString();
    }

    private S3Client client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = S3Client.builder().endpointOverride(URI.create(properties.getEndpoint()))
                            .region(Region.of(properties.getRegion())).credentialsProvider(credentials())
                            .httpClientBuilder(UrlConnectionHttpClient.builder())
                            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(properties.isPathStyleAccess()).build()).build();
                }
            }
        }
        return client;
    }

    private S3Presigner presigner() {
        if (presigner == null) {
            synchronized (this) {
                if (presigner == null) {
                    presigner = S3Presigner.builder().endpointOverride(URI.create(properties.getEndpoint()))
                            .region(Region.of(properties.getRegion())).credentialsProvider(credentials())
                            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(properties.isPathStyleAccess()).build()).build();
                }
            }
        }
        return presigner;
    }

    private StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
    }
}
