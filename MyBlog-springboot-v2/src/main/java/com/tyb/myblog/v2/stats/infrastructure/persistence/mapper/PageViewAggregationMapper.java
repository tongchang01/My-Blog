package com.tyb.myblog.v2.stats.infrastructure.persistence.mapper;

import com.tyb.myblog.v2.stats.infrastructure.persistence.entity.PageViewDailyEntity;
import com.tyb.myblog.v2.stats.infrastructure.persistence.projection.PageViewAggregateRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 访问日聚合 Mapper，所有 SQL 位于 XML。
 */
@Mapper
public interface PageViewAggregationMapper {

    List<PageViewAggregateRow> summarize(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    int deleteDay(@Param("statDate") LocalDate statDate);

    int insertAll(@Param("rows") List<PageViewDailyEntity> rows);

    int deleteRawBefore(@Param("cutoff") LocalDateTime cutoff);
}
