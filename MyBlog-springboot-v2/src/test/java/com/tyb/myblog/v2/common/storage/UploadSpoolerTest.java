package com.tyb.myblog.v2.common.storage;

import com.tyb.myblog.v2.common.storage.image.SpooledUpload;
import com.tyb.myblog.v2.common.storage.image.UploadSpooler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadSpoolerTest {

    @TempDir
    Path tempDir;

    @Test
    void spoolsHashesAndDeletesOnClose() throws Exception {
        byte[] content = "image-bytes".getBytes(UTF_8);
        UploadSpooler spooler = new UploadSpooler(tempDir);
        Path path;

        try (SpooledUpload upload = spooler.spool(
                new ByteArrayInputStream(content), content.length)) {
            path = upload.path();
            assertThat(Files.readAllBytes(path)).isEqualTo(content);
            assertThat(upload.size()).isEqualTo(content.length);
            assertThat(upload.sha256()).matches("[0-9a-f]{64}");
        }

        assertThat(path).doesNotExist();
    }

    @Test
    void rejectsEmptyAndOversizedInputWithoutLeavingTempFile() {
        UploadSpooler spooler = new UploadSpooler(tempDir);

        assertThatThrownBy(() -> spooler.spool(
                new ByteArrayInputStream(new byte[0]), 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spooler.spool(
                new ByteArrayInputStream(new byte[11]), 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(tempDir).isEmptyDirectory();
    }

    @Test
    void cleansTempFileWhenInputFails() {
        UploadSpooler spooler = new UploadSpooler(tempDir);
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("broken");
            }
        };

        assertThatThrownBy(() -> spooler.spool(failing, 10))
                .isInstanceOf(IOException.class);
        assertThat(tempDir).isEmptyDirectory();
    }
}
