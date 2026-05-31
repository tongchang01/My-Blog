package com.tyb.myblog.v2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class MyBlogV2Application {

    public static void main(String[] args) {
        SpringApplication.run(MyBlogV2Application.class, args);
    }
}
