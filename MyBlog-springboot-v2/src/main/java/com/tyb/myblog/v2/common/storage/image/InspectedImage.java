package com.tyb.myblog.v2.common.storage.image;

/**
 * 服务端识别后的图片元数据。
 */
public record InspectedImage(
        ImageFormat format,
        int width,
        int height
) {
}
