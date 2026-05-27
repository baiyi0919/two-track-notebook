package com.twotrack.notebook.scheduler;

import com.twotrack.notebook.service.AnalysisFrameworkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FrameworkUpdateScheduler {

    private final AnalysisFrameworkService analysisFrameworkService;

    /**
     * 每天凌晨2:00 检查并更新所有到期的分析框架
     * cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledUpdate() {
        log.info("开始执行分析框架定时更新...");
        try {
            int count = analysisFrameworkService.updateAllPendingFrameworksForScheduler();
            log.info("分析框架定时更新完成，共更新 {} 个", count);
        } catch (Exception e) {
            log.error("分析框架定时更新失败", e);
        }
    }
}
