package com.heima.task;

import com.heima.modules.search.service.SearchOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchOutboxCompensationTask {
    private final SearchOutboxService searchOutboxService;

    @Scheduled(fixedDelayString = "${wanjuan.search.outbox-scan-ms:5000}")
    public void compensate() {
        searchOutboxService.compensate();
    }
}
