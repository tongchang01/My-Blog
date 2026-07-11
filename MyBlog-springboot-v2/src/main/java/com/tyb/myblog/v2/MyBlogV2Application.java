package com.tyb.myblog.v2;

import com.tyb.myblog.v2.common.config.BootstrapAdminProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;

@ConfigurationPropertiesScan
@SpringBootApplication
public class MyBlogV2Application {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =
                SpringApplication.run(MyBlogV2Application.class, args);
        if (context.getBean(BootstrapAdminProperties.class).exitAfterRun()) {
            System.exit(SpringApplication.exit(context));
        }
    }
}
