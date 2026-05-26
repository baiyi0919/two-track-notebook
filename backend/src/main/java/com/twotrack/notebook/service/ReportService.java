package com.twotrack.notebook.service;

import com.twotrack.notebook.vo.AttentionReportVo;
import com.twotrack.notebook.vo.AttentionTrendVo;

import java.time.LocalDate;

public interface ReportService {

    /** 获取指定日期的注意力审计报告 */
    AttentionReportVo getAttentionReport(LocalDate date);

    /** 获取多日趋势报告（默认7天） */
    AttentionTrendVo getAttentionTrend(Integer days);
}
