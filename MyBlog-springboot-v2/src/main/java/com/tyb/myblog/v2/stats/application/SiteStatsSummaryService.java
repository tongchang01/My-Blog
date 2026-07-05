package com.tyb.myblog.v2.stats.application;

import com.tyb.myblog.v2.stats.domain.StatsDashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

/** 查询公开站点统计摘要。 */
@Service
@RequiredArgsConstructor
public class SiteStatsSummaryService {

    private final StatsDashboardRepository repository;
    private final Clock clock;

    public SiteStatsSummaryResult summary() {
        return new SiteStatsSummaryResult(
                repository.sumUv(LocalDate.now(clock)),
                repository.sumPv());
    }
}
