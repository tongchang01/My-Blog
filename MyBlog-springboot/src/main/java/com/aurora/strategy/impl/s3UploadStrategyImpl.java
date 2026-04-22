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

@Service("s3UploadStrategyImpl")
public class s3UploadStrategyImpl extends AbstractUploadStrategyImpl {

    @Autowired
    private s3ConfigProperties s3ConfigProperties;

    //获取s3Client
    private AmazonS3 gets3Client() {
        return AmazonS3ClientBuilder.standard()
                .withRegion(s3ConfigProperties.getRegion())
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(s3ConfigProperties.getAccessKeyId(), s3ConfigProperties.getAccessKeySecret())))
                .build();

    }

    //判断文件是否存在
    @Override
    public Boolean exists(String filePath) {
        return gets3Client().doesObjectExist(s3ConfigProperties.getBucketName(), filePath);
    }

    //上传文件
    @Override
    public void upload(String path, String fileName, InputStream inputStream) {
            ObjectMetadata metadata = new ObjectMetadata();
            // 关键：直接传入 inputStream，并带上 metadata
            gets3Client().putObject(s3ConfigProperties.getBucketName(), path + fileName, inputStream, metadata);
    }

    //获取文件访问路径
    @Override
    public String getFileAccessUrl(String filePath) {
        return s3ConfigProperties.getUrl() + filePath;
    }

}
