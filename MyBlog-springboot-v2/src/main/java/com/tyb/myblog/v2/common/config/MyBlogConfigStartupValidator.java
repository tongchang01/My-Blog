package com.tyb.myblog.v2.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/**
 * MyBlog 全局运行环境启动校验器。
 */
@Component
public class MyBlogConfigStartupValidator implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(MyBlogConfigStartupValidator.class);
    private static final ZoneId REQUIRED_ZONE = ZoneId.of("Asia/Tokyo");

    /**
     * 校验 JVM 默认时区，避免数据库、应用和序列化时间语义不一致。
     */
    @Override
    public void afterPropertiesSet() {
        ZoneId currentZone = ZoneId.systemDefault();
        LOGGER.info("当前 JVM 默认时区：{}", currentZone);

        if (!REQUIRED_ZONE.equals(currentZone)) {
            throw new IllegalStateException(
                    "JVM 默认时区必须是 Asia/Tokyo，请添加启动参数 -Duser.timezone=Asia/Tokyo");
        }
    }
}
