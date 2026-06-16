package com.tyb.myblog.v2.content.domain.article;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * 公开文章查询条件，归档月份在进入 XML 前转换成闭开时间区间。
 */
public record PublicArticleCriteria(
        int page,
        int size,
        Long categoryId,
        Long tagId,
        String keyword,
        String archiveMonth,
        LocalDateTime archiveFrom,
        LocalDateTime archiveTo,
        LocalDateTime now) {

    public static PublicArticleCriteria from(
            int page,
            int size,
            Long categoryId,
            Long tagId,
            String keyword,
            String archiveMonth,
            LocalDateTime now) {
        LocalDateTime from = null;
        LocalDateTime to = null;
        if (archiveMonth != null && !archiveMonth.isBlank()) {
            try {
                YearMonth month = YearMonth.parse(archiveMonth.trim());
                LocalDate firstDay = month.atDay(1);
                from = firstDay.atStartOfDay();
                to = firstDay.plusMonths(1).atStartOfDay();
            } catch (DateTimeParseException exception) {
                throw new IllegalArgumentException("归档月份格式必须为 yyyy-MM", exception);
            }
        }
        return new PublicArticleCriteria(
                page,
                size,
                categoryId,
                tagId,
                keyword,
                archiveMonth,
                from,
                to,
                now);
    }
}
