package com.jwellkeeper.analytics.dto;

public record DecisionInsight(
        String severity,
        String title,
        String message,
        String recommendedAction
) {
}
