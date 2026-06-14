package com.tyb.myblog.v2.system.application.attachment;

import com.tyb.myblog.v2.common.storage.image.ImageFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 生成不包含用户输入的附件对象键。
 */
@Component
@RequiredArgsConstructor
public class ObjectKeyGenerator {

    private final Clock clock;

    public String generate(ImageFormat format) {
        LocalDate today = LocalDate.now(clock);
        return "attachments/%04d/%02d/%s.%s".formatted(
                today.getYear(),
                today.getMonthValue(),
                UUID.randomUUID(),
                format.extension());
    }
}
