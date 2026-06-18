package com.tyb.myblog.v2.stats.infrastructure.scheduling;

import com.tyb.myblog.v2.stats.application.PageViewMaintenanceService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PageViewMaintenanceSchedulerTest {

    @Test
    void delegatesAllThreeSchedulingEntrypoints() {
        PageViewMaintenanceService service =
                mock(PageViewMaintenanceService.class);
        PageViewMaintenanceScheduler scheduler =
                new PageViewMaintenanceScheduler(service);

        scheduler.aggregateRecentDays();
        scheduler.reconcileAndCleanup();
        scheduler.catchUpAfterStartup(null);

        verify(service).rebuildCurrentWindow();
        verify(service, times(2)).reconcileAndCleanup();
    }
}
