package com.tyb.myblog.v2.common.storage;

import com.tyb.myblog.v2.common.storage.image.ImageFormat;
import com.tyb.myblog.v2.common.storage.image.ImageInspector;
import com.tyb.myblog.v2.common.storage.image.InspectedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageInspectorTest {

    @TempDir
    Path tempDir;

    private final ImageInspector inspector = new ImageInspector();

    @Test
    void identifiesJpegPngGifAndWebpByContent() throws Exception {
        assertImage(writeImage("jpeg", "wrong.bin"), ImageFormat.JPEG);
        assertImage(writeImage("png", "wrong.jpg"), ImageFormat.PNG);
        assertImage(writeImage("gif", "wrong.png"), ImageFormat.GIF);

        byte[] webp = Base64.getDecoder().decode(
                "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEALmk0mk0iIiIiIgBoSygABc6zbAAA");
        Path webpFile = tempDir.resolve("wrong.gif");
        Files.write(webpFile, webp);
        assertThat(inspector.inspect(webpFile).format())
                .isEqualTo(ImageFormat.WEBP);
    }

    @Test
    void rejectsTextAndTruncatedImage() throws Exception {
        Path text = tempDir.resolve("fake.png");
        Files.writeString(text, "not-an-image");
        Path truncated = writeImage("png", "truncated.png");
        byte[] bytes = Files.readAllBytes(truncated);
        Files.write(truncated, java.util.Arrays.copyOf(bytes, 20));

        assertThatThrownBy(() -> inspector.inspect(text))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> inspector.inspect(truncated))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Path writeImage(String format, String filename) throws Exception {
        Path file = tempDir.resolve(filename);
        BufferedImage image =
                new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB);
        assertThat(ImageIO.write(image, format, file.toFile())).isTrue();
        return file;
    }

    private void assertImage(Path path, ImageFormat format) {
        InspectedImage result = inspector.inspect(path);
        assertThat(result.format()).isEqualTo(format);
        assertThat(result.width()).isEqualTo(2);
        assertThat(result.height()).isEqualTo(3);
    }
}
