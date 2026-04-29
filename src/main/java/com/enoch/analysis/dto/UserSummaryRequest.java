package com.enoch.analysis.dto;

public record UserSummaryRequest(
        Long userId,
        Long institutionId
) {
}
