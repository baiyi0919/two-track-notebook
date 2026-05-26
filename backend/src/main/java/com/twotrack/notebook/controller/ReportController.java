package com.twotrack.notebook.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twotrack.notebook.common.Result;
import com.twotrack.notebook.service.ReportService;
import com.twotrack.notebook.vo.AttentionReportVo;
import com.twotrack.notebook.vo.AttentionTrendVo;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
@SaCheckLogin
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** 获取注意力审计报告 */
    @GetMapping("/attention")
    public Result<AttentionReportVo> attentionReport(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return Result.success(reportService.getAttentionReport(date));
    }

    /** 获取注意力趋势报告（7天/30天） */
    @GetMapping("/attention/trend")
    public Result<AttentionTrendVo> attentionTrend(
            @RequestParam(required = false, defaultValue = "7") Integer days) {
        return Result.success(reportService.getAttentionTrend(days));
    }
}
