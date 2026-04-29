package com.enoch.analysis.dto;

import java.util.List;

public record ModuleTopicItemDTO(
        String topicCode,
        long count,
        double highestScore,
        long streak,
        double score,
        long questionsCount,
        long attendedQuestionsCount,
        double topicCompletionPercentage,
        double accuracyRatePercentage,
        double conceptMasteryScore,
        double retentionScore,
        double learningEfficiency,
        double guessRate,
        double consistencyPercentage,
        double performanceIndex,
        double trendPercentage,
        String trendLabel,
        double priorityScore,
        String priorityLabel,
        String priorityReason,
        double failureRiskScore,
        String failureRiskLabel,
        List<Double> scoreTimeline,
        double attemptProgressPercentage,
        String lstSubmittedDate,
        boolean weak,
        double attemptScore
) {
}
