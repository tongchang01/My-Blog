package com.tyb.myblog.v2.common.infrastructure.storage.s3;

import com.tyb.myblog.v2.common.storage.StoreObjectCommand;
import com.tyb.myblog.v2.common.storage.StorageOperationException;
import com.tyb.myblog.v2.common.storage.StorageService;
import com.tyb.myblog.v2.common.storage.StorageType;
import com.tyb.myblog.v2.common.storage.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;

/**
 * AWS S3 附件存储适配器。
 */
public class S3StorageService implements StorageService {

    private static final Logger log =
            LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3StorageService(
            S3Client client,
            String bucket,
            URI publicBaseUrl) {
        this.client = client;
        this.bucket = bucket;
        this.publicBaseUrl = trimTrailingSlash(publicBaseUrl.toString());
    }

    @Override
    public StorageType type() {
        return StorageType.S3;
    }

    @Override
    public StoredObject store(StoreObjectCommand command) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(command.objectKey())
                .contentType(command.contentType())
                .contentLength(command.contentLength())
                .build();
        try {
            client.putObject(
                    request,
                    RequestBody.fromFile(command.source()));
            return new StoredObject(
                    type(),
                    bucket,
                    command.objectKey(),
                    publicBaseUrl + "/" + command.objectKey());
        } catch (S3Exception exception) {
            throw operationFailed(
                    "put", bucket, command.objectKey(), exception);
        }
    }

    @Override
    public boolean exists(String bucket, String objectKey) {
        requireBucket(bucket);
        try {
            client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
            return true;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return false;
            }
            throw operationFailed(
                    "head", bucket, objectKey, exception);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        requireBucket(bucket);
        try {
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
        } catch (S3Exception exception) {
            throw operationFailed(
                    "delete", bucket, objectKey, exception);
        }
    }

    private StorageOperationException operationFailed(
            String operation,
            String bucket,
            String objectKey,
            S3Exception exception) {
        log.error(
                "S3附件操作失败，operation={}, bucket={}, objectKey={}, requestId={}",
                operation,
                bucket,
                objectKey,
                exception.requestId());
        return new StorageOperationException(
                "附件存储操作失败", exception);
    }

    private void requireBucket(String candidate) {
        if (!bucket.equals(candidate)) {
            throw new StorageOperationException("S3 存储桶不匹配");
        }
    }

    private String trimTrailingSlash(String value) {
        String normalized = value;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
