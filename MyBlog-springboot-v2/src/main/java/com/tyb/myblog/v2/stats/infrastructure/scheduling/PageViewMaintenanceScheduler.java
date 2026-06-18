package com.tyb.myblog.v2.stats.infrastructure.scheduling;

import com.tyb.myblog.v2.stats.application.PageViewMaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 单实例访问统计调度入口。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "myblog.stats.scheduling",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PageViewMaintenanceScheduler {

    private final PageViewMaintenanceService maintenanceService;
    private final ReentrantLock lock = new ReentrantLock();

    @Scheduled(
            cron = "${myblog.stats.scheduling.aggregate-cron:0 */5 * * * *}",
            zone = "Asia/Tokyo")
    public void aggregateRecentDays() {
        runExclusively(
                "访问统计近期聚合",
                maintenanceService::rebuildCurrentWindow);
    }

    @Scheduled(
            cron = "${myblog.stats.scheduling.maintenance-cron:0 30 3 * * *}",
            zone = "Asia/Tokyo")
    public void reconcileAndCleanup() {
        runExclusively(
                "访问统计校准清理",
                maintenanceService::reconcileAndCleanup);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpAfterStartup(ApplicationReadyEvent event) {
        runExclusively(
                "访问统计启动补算",
                maintenanceService::reconcileAndCleanup);
    }

    private void runExclusively(String taskName, Runnable action) {
        if (!lock.tryLock()) {
            log.info("{}因已有任务运行而跳过", taskName);
            return;
        }
        try {
            action.run();
        } catch (RuntimeException exception) {
            log.error("{}失败，等待下一次调度重试", taskName, exception);
        } finally {
            lock.unlock();
        }
    }
}
