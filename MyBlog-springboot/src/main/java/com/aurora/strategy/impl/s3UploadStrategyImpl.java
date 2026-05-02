package com.aurora.strategy.impl;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.aurora.config.properties.s3ConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service("s3UploadStrategyImpl")
public class s3UploadStrategyImpl extends AbstractUploadStrategyImpl {

    private static final Map<String, String> CONTENT_TYPE_MAP = new HashMap<>();

    static {
        CONTENT_TYPE_MAP.put(".jpg", "image/jpeg");
        CONTENT_TYPE_MAP.put(".jpeg", "image/jpeg");
        CONTENT_TYPE_MAP.put(".png", "image/png");
        CONTENT_TYPE_MAP.put(".gif", "image/gif");
        CONTENT_TYPE_MAP.put(".webp", "image/webp");
        CONTENT_TYPE_MAP.put(".svg", "image/svg+xml");
        CONTENT_TYPE_MAP.put(".mp3", "audio/mpeg");
        CONTENT_TYPE_MAP.put(".wav", "audio/wav");
        CONTENT_TYPE_MAP.put(".flac", "audio/flac");
        CONTENT_TYPE_MAP.put(".aac", "audio/aac");
        CONTENT_TYPE_MAP.put(".ogg", "audio/ogg");
        CONTENT_TYPE_MAP.put(".oga", "audio/ogg");
        CONTENT_TYPE_MAP.put(".m4a", "audio/mp4");
        CONTENT_TYPE_MAP.put(".mp4", "audio/mp4");
        CONTENT_TYPE_MAP.put(".weba", "audio/webm");
        CONTENT_TYPE_MAP.put(".webm", "audio/webm");
        CONTENT_TYPE_MAP.put(".opus", "audio/ogg");
        CONTENT_TYPE_MAP.put(".lrc", "text/plain; charset=utf-8");
        CONTENT_TYPE_MAP.put(".txt", "text/plain; charset=utf-8");
    }

    @Autowired
    private s3ConfigProperties s3ConfigProperties;

    private AmazonS3 gets3Client() {
        return AmazonS3ClientBuilder.standard()
                .withRegion(s3ConfigProperties.getRegion())
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(
                                s3ConfigProperties.getAccessKeyId(),
                                s3ConfigProperties.getAccessKeySecret())))
                .build();
    }

    @Override
    public Boolean exists(String filePath) {
        return gets3Client().doesObjectExist(s3ConfigProperties.getBucketName(), filePath);
    }

    @Override
    public void upload(String path, String fileName, InputStream inputStream) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(resolveContentType(fileName));
        gets3Client().putObject(s3ConfigProperties.getBucketName(), path + fileName, inputStream, metadata);
    }

    @Override
    public String getFileAccessUrl(String filePath) {
        return s3ConfigProperties.getUrl() + filePath;
    }

    private String resolveContentType(String fileName) {
        String normalized = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        int extIndex = normalized.lastIndexOf(".");
        if (extIndex == -1) {
            return "application/octet-stream";
        }
        return CONTENT_TYPE_MAP.getOrDefault(normalized.substring(extIndex), "application/octet-stream");
    }
}
