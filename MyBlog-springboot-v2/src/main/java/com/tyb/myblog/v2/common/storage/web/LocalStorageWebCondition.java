package com.tyb.myblog.v2.common.storage.web;

import com.tyb.myblog.v2.common.storage.StorageType;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 根据当前上传后端和显式兼容开关决定是否提供本地媒体映射。
 */
final class LocalStorageWebCondition implements Condition {

    private static final String ENABLED_PROPERTY =
            "myblog.storage.local.web-enabled";
    private static final String TYPE_PROPERTY = "myblog.storage.type";

    @Override
    public boolean matches(
            ConditionContext context,
            AnnotatedTypeMetadata metadata) {
        Binder binder = Binder.get(context.getEnvironment());
        BindResult<Boolean> explicit = binder.bind(
                ENABLED_PROPERTY, Boolean.class);
        if (explicit.isBound()) {
            return explicit.get();
        }
        StorageType type = binder.bind(TYPE_PROPERTY, StorageType.class)
                .orElse(StorageType.LOCAL);
        return type == StorageType.LOCAL;
    }
}
