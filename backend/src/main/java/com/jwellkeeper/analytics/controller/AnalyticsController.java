package com.jwellkeeper.analytics.controller;

import com.jwellkeeper.analytics.dto.AnalyticsRange;
import com.jwellkeeper.analytics.dto.AnalyticsSummaryResponse;
import com.jwellkeeper.analytics.service.AnalyticsService;
import com.jwellkeeper.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService service;

    @GetMapping("/summary")
    public ApiResponse<AnalyticsSummaryResponse> summary(
            @RequestParam(defaultValue = "THIS_MONTH") AnalyticsRange range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success("Analytics summary fetched", service.summary(range, from, to));
    }
}
