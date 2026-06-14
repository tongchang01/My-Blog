package com.tyb.myblog.v2.common.storage.image;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;

/**
 * 根据文件内容识别并完整解码允许的图片格式。
 */
public class ImageInspector {

    private static final int MAX_SIDE = 20_000;
    private static final long MAX_PIXELS = 40_000_000L;
    private static final int MAX_GIF_FRAMES = 500;

    public InspectedImage inspect(Path path) {
        try (ImageInputStream stream =
                     ImageIO.createImageInputStream(path.toFile())) {
            if (stream == null) {
                throw invalidImage();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw invalidImage();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, false, true);
                ImageFormat format = format(reader.getFormatName());
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                validateDimensions(width, height);
                BufferedImage firstFrame = reader.read(0);
                if (firstFrame == null) {
                    throw invalidImage();
                }
                if (format == ImageFormat.GIF) {
                    validateGif(reader);
                }
                return new InspectedImage(format, width, height);
            } finally {
                reader.dispose();
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("图片文件已损坏", exception);
        }
    }

    private void validateGif(ImageReader reader) throws IOException {
        int frames = reader.getNumImages(true);
        if (frames < 1 || frames > MAX_GIF_FRAMES) {
            throw new IllegalArgumentException("GIF 帧数不能超过500");
        }
        for (int index = 0; index < frames; index++) {
            validateDimensions(
                    reader.getWidth(index),
                    reader.getHeight(index));
            if (reader.read(index) == null) {
                throw invalidImage();
            }
        }
    }

    private ImageFormat format(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "jpeg", "jpg" -> ImageFormat.JPEG;
            case "png" -> ImageFormat.PNG;
            case "gif" -> ImageFormat.GIF;
            case "webp" -> ImageFormat.WEBP;
            default -> throw new IllegalArgumentException("不支持的图片格式");
        };
    }

    private void validateDimensions(int width, int height) {
        if (width < 1 || height < 1
                || width > MAX_SIDE || height > MAX_SIDE
                || Math.multiplyExact((long) width, height) > MAX_PIXELS) {
            throw new IllegalArgumentException("图片尺寸超过安全限制");
        }
    }

    private IllegalArgumentException invalidImage() {
        return new IllegalArgumentException("文件不是有效图片");
    }
}
