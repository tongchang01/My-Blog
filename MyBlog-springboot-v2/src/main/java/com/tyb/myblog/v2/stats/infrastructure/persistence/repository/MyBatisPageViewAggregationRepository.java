package com.tyb.myblog.v2.stats.infrastructure.persistence.repository;

import com.tyb.myblog.v2.stats.domain.DailyPageView;
import com.tyb.myblog.v2.stats.domain.PageViewAggregationRepository;
import com.tyb.myblog.v2.stats.domain.StatsLanguage;
import com.tyb.myblog.v2.stats.infrastructure.persistence.entity.PageViewDailyEntity;
import com.tyb.myblog.v2.stats.infrastructure.persistence.mapper.PageViewAggregationMapper;
import com.tyb.myblog.v2.stats.infrastructure.persistence.projection.PageViewAggregateRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MyBatis 页面访问聚合仓储。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisPageViewAggregationRepository
        implements PageViewAggregationRepository {

    private final PageViewAggregationMapper mapper;

    @Override
    public List<DailyPageView> summarize(
            LocalDateTime start,
            LocalDateTime end,
            LocalDate statDate) {
        return mapper.summarize(start, end).stream()
                .map(row -> toDomain(row, statDate))
                .toList();
    }

    @Override
    public void deleteDay(LocalDate statDate) {
        mapper.deleteDay(statDate);
    }

    @Override
    public void insertAll(List<DailyPageView> rows) {
        if (rows.isEmpty()) {
            return;
        }
        int inserted = mapper.insertAll(rows.stream()
                .map(this::toEntity)
                .toList());
        if (inserted != rows.size()) {
            throw new IllegalStateException("访问日聚合写入不完整");
        }
    }

    @Override
    public int deleteRawBefore(LocalDateTime cutoff) {
        return mapper.deleteRawBefore(cutoff);
    }

    private DailyPageView toDomain(
            PageViewAggregateRow row,
            LocalDate statDate) {
        return new DailyPageView(
                row.getArticleId(),
                StatsLanguage.fromCode(row.getLang()),
                statDate,
                row.getPv(),
                row.getUv());
    }

    private PageViewDailyEntity toEntity(DailyPageView row) {
        PageViewDailyEntity entity = new PageViewDailyEntity();
        entity.setArticleId(row.articleId());
        entity.setLang(row.language().code());
        entity.setStatDate(row.statDate());
        entity.setPv(row.pv());
        entity.setUv(row.uv());
        return entity;
    }
}
