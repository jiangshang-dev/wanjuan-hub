package com.heima.infrastructure.storage;

import java.io.InputStream;
import java.time.Duration;

public interface ObjectStorageService {
    void upload(String bucket, String key, byte[] content, String contentType);
    byte[] download(String bucket, String key);
    InputStream openStream(String bucket, String key);
    void delete(String bucket, String key);
    void copy(String sourceBucket, String sourceKey, String targetBucket, String targetKey);
    String presignGet(String bucket, String key, Duration expire);
    void ensureAvailable();
}
