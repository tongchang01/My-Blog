package com.tyb.myblog.v2.common.storage;

import com.tyb.myblog.v2.common.storage.config.StorageProperties;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 按上传配置或附件历史记录选择物理存储实现。
 */
public class StorageServiceRegistry {

    private final StorageProperties properties;
    private final Map<StorageType, StorageService> services =
            new EnumMap<>(StorageType.class);

    public StorageServiceRegistry(
            StorageProperties properties,
            List<StorageService> services) {
        this.properties = Objects.requireNonNull(properties);
        for (StorageService service : services) {
            StorageService previous =
                    this.services.put(service.type(), service);
            if (previous != null) {
                throw new IllegalStateException(
                        "附件存储类型重复注册：" + service.type());
            }
        }
    }

    public StorageService current() {
        return required(properties.getType());
    }

    public StorageService required(StorageType type) {
        StorageService service = services.get(type);
        if (service == null) {
            throw new StorageOperationException(
                    "附件存储后端不可用：" + type);
        }
        return service;
    }
}
