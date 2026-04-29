package com.enoch.analysis.service.impl;

import com.enoch.analysis.config.AnalysisProperties;
import com.enoch.analysis.dto.UserSummaryRequest;
import com.enoch.analysis.dto.UserTopicDetailDTO;
import com.enoch.analysis.repo.AnalysisJdbcRepository;
import com.enoch.analysis.service.FeatureDisabledException;
import com.enoch.analysis.service.AnalysisSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class AnalysisSummaryServiceImpl implements AnalysisSummaryService {

    @Autowired
    AnalysisJdbcRepository repository;
    @Autowired
    AnalysisProperties analysisProperties;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "userTopicSummary",
            key = "#request.userId() + ':' + (#request.institutionId() == null ? 'NA' : #request.institutionId())"
    )
    public List<UserTopicDetailDTO> getTopicDetails(UserSummaryRequest request) {
        ensureFeatureEnabled();

        long userId = request.userId();
        Long institutionId = request.institutionId();
        Collection<String> excludedQtypes = analysisProperties.getSummary().getExcludeQtypes();
        double defaultPassPercentage = analysisProperties.getSummary().getDefaultPassPercentage();

        CompletableFuture<List<AnalysisJdbcRepository.EnrolledTopicRow>> enrolledFuture =
                CompletableFuture.supplyAsync(() -> repository.findEnrolledTopics(
                        userId,
                        institutionId,
                        excludedQtypes,
                        defaultPassPercentage
                ));
        CompletableFuture<List<AnalysisJdbcRepository.AttemptedTopicRow>> attemptedFuture =
                CompletableFuture.supplyAsync(() -> repository.findAttemptedTopicPerformance(
                        userId,
                        institutionId,
                        excludedQtypes
                ));
        CompletableFuture<List<AnalysisJdbcRepository.TopicQuestionProgressRow>> progressFuture =
                CompletableFuture.supplyAsync(() -> repository.findTopicQuestionProgress(
                        userId,
                        institutionId,
                        excludedQtypes
                ));
        CompletableFuture<List<AnalysisJdbcRepository.TopicAttemptScoreRow>> attemptScoreFuture =
                CompletableFuture.supplyAsync(() -> repository.findTopicAttemptScores(
                        userId,
                        institutionId,
                        excludedQtypes
                ));
        CompletableFuture<List<AnalysisJdbcRepository.TopicCognitiveStatsRow>> cognitiveFuture =
                CompletableFuture.supplyAsync(() -> repository.findTopicCognitiveStats(
                        userId,
                        institutionId,
                        excludedQtypes
                ));
        CompletableFuture<String> userNameFuture =
                CompletableFuture.supplyAsync(() -> repository.findUserDisplayName(userId).orElse(null));

        CompletableFuture.allOf(
                enrolledFuture,
                attemptedFuture,
                progressFuture,
                attemptScoreFuture,
                cognitiveFuture,
                userNameFuture
        ).join();

        List<AnalysisJdbcRepository.EnrolledTopicRow> enrolledRows = enrolledFuture.join();
        List<AnalysisJdbcRepository.AttemptedTopicRow> attemptedRows = attemptedFuture.join();
        List<AnalysisJdbcRepository.TopicQuestionProgressRow> progressRows = progressFuture.join();
        List<AnalysisJdbcRepository.TopicAttemptScoreRow> attemptScoreRows = attemptScoreFuture.join();
        List<AnalysisJdbcRepository.TopicCognitiveStatsRow> cognitiveRows = cognitiveFuture.join();
        String userName = userNameFuture.join();
        return toTopicDetails(
                userId,
                userName,
                defaultPassPercentage,
                enrolledRows,
                attemptedRows,
                progressRows,
                attemptScoreRows,
                cognitiveRows
        );
    }

    private List<UserTopicDetailDTO> toTopicDetails(
            long userId,
            String userName,
            double defaultPassPercentage,
            List<AnalysisJdbcRepository.EnrolledTopicRow> enrolledRows,
            List<AnalysisJdbcRepository.AttemptedTopicRow> attemptedRows,
            List<AnalysisJdbcRepository.TopicQuestionProgressRow> progressRows,
            List<AnalysisJdbcRepository.TopicAttemptScoreRow> attemptScoreRows,
            List<AnalysisJdbcRepository.TopicCognitiveStatsRow> cognitiveRows
    ) {
        Map<String, AnalysisJdbcRepository.AttemptedTopicRow> attemptedByKey = new LinkedHashMap<>();
        for (AnalysisJdbcRepository.AttemptedTopicRow row : attemptedRows) {
            attemptedByKey.put(topicKey(row.subjectId(), row.topicCode()), row);
        }

        Map<String, AnalysisJdbcRepository.TopicQuestionProgressRow> progressByKey = new LinkedHashMap<>();
        for (AnalysisJdbcRepository.TopicQuestionProgressRow row : progressRows) {
            progressByKey.put(topicKey(row.subjectId(), row.topicCode()), row);
        }

        Map<String, List<AnalysisJdbcRepository.TopicAttemptScoreRow>> attemptScoresByKey = new LinkedHashMap<>();
        for (AnalysisJdbcRepository.TopicAttemptScoreRow row : attemptScoreRows) {
            String key = topicKey(row.subjectId(), row.topicCode());
            attemptScoresByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
        }

        Map<String, AnalysisJdbcRepository.TopicCognitiveStatsRow> cognitiveByKey = new LinkedHashMap<>();
        for (AnalysisJdbcRepository.TopicCognitiveStatsRow row : cognitiveRows) {
            cognitiveByKey.put(topicKey(row.subjectId(), row.topicCode()), row);
        }

        return enrolledRows.stream().map(enrolled -> {
            String key = topicKey(enrolled.subjectId(), enrolled.topicCode());
            AnalysisJdbcRepository.AttemptedTopicRow attempted = attemptedByKey.get(key);
            AnalysisJdbcRepository.TopicQuestionProgressRow progress = progressByKey.get(key);
            AnalysisJdbcRepository.TopicCognitiveStatsRow cognitive = cognitiveByKey.get(key);
            List<AnalysisJdbcRepository.TopicAttemptScoreRow> attemptsForTopic = attemptScoresByKey.getOrDefault(key, List.of());

            long attemptsCount = attempted == null ? 0L : attempted.attemptsCount();
            double averageScorePercentage = attempted == null ? 0.0 : attempted.averageScorePercentage();
            double highestScorePercentage = attempted == null ? 0.0 : attempted.highestScorePercentage();

            long questionsCount = progress == null ? 0L : progress.questionsCount();
            long attendedQuestionsCount = progress == null ? 0L : progress.attendedQuestionsCount();
            double topicCompletionPercentage = questionsCount == 0
                    ? 0.0
                    : (attendedQuestionsCount * 100.0) / questionsCount;

            List<AnalysisJdbcRepository.TopicAttemptScoreRow> sortedAttempts = sortAttempts(attemptsForTopic);
            long streak = computeStreak(sortedAttempts);
            List<Double> scoreTimeline = buildScoreTimeline(sortedAttempts);
            double attemptProgressPercentage = computeAttemptProgressPercentage(sortedAttempts);
            double accuracyRatePercentage = averageScorePercentage;
            double trendPercentage = computeTrendPercentage(sortedAttempts);
            String trendLabel = computeTrendLabel(trendPercentage);
            double consistencyPercentage = computeConsistencyScore(scoreTimeline);

            double conceptMasteryScore = computeConceptMasteryScore(cognitive, accuracyRatePercentage);
            double retentionScore = computeRetentionScore(cognitive);
            double learningEfficiency = computeLearningEfficiency(cognitive, sortedAttempts);
            double guessRate = computeGuessRate(cognitive);

            double performanceIndex = (0.5 * accuracyRatePercentage)
                    + (0.3 * topicCompletionPercentage)
                    + (0.2 * consistencyPercentage);

            double negativeTrendWeight = trendPercentage < 0 ? Math.min(100.0, Math.abs(trendPercentage)) : 0.0;
            double priorityScore = (100.0 - accuracyRatePercentage)
                    + (100.0 - topicCompletionPercentage)
                    + negativeTrendWeight;
            String priorityLabel = classifyPriority(priorityScore);
            String priorityReason = buildPriorityReason(accuracyRatePercentage, topicCompletionPercentage, trendPercentage, attemptsCount);

            double failureRiskScore = ((100.0 - accuracyRatePercentage) * 0.5)
                    + ((100.0 - topicCompletionPercentage) * 0.3)
                    + (negativeTrendWeight * 0.2);
            String failureRiskLabel = classifyFailureRisk(failureRiskScore);

            boolean weak = (accuracyRatePercentage < 40.0) || (topicCompletionPercentage < 50.0);

            String lastSubmittedDate = Optional.ofNullable(attempted)
                    .map(AnalysisJdbcRepository.AttemptedTopicRow::lastSubmittedDate)
                    .map(ts -> ts.toLocalDateTime().toString())
                    .orElse(null);

            return new UserTopicDetailDTO(
                    userId,
                    userName,
                    firstNonBlank(enrolled.courseCode(), attempted == null ? null : attempted.courseCode()),
                    firstNonBlank(enrolled.sylCode(), attempted == null ? null : attempted.sylCode()),
                    enrolled.subjectName(),
                    firstNonBlank(enrolled.unitCode(), attempted == null ? null : attempted.unitCode()),
                    firstNonBlank(enrolled.modCode(), attempted == null ? null : attempted.modCode()),
                    enrolled.topicCode(),
                    attemptsCount,
                    highestScorePercentage,
                    0L,
                    averageScorePercentage,
                    questionsCount,
                    attendedQuestionsCount,
                    topicCompletionPercentage,
                    accuracyRatePercentage,
                    conceptMasteryScore,
                    retentionScore,
                    learningEfficiency,
                    guessRate,
                    consistencyPercentage,
                    performanceIndex,
                    trendPercentage,
                    trendLabel,
                    priorityScore,
                    priorityLabel,
                    priorityReason,
                    failureRiskScore,
                    failureRiskLabel,
                    scoreTimeline,
                    attemptProgressPercentage,
                    lastSubmittedDate,
                    weak,
                    averageScorePercentage
            );
        }).toList();
    }

    private List<Double> buildScoreTimeline(List<AnalysisJdbcRepository.TopicAttemptScoreRow> sortedAttempts) {
        if (sortedAttempts.isEmpty()) {
            return List.of();
        }
        return sortedAttempts.stream()
                .map(AnalysisJdbcRepository.TopicAttemptScoreRow::attemptPercentage)
                .collect(Collectors.toList());
    }

    private List<AnalysisJdbcRepository.TopicAttemptScoreRow> sortAttempts(
            List<AnalysisJdbcRepository.TopicAttemptScoreRow> attemptsForTopic
    ) {
        if (attemptsForTopic.size() < 2) {
            return attemptsForTopic;
        }
        List<AnalysisJdbcRepository.TopicAttemptScoreRow> sorted = new ArrayList<>(attemptsForTopic);
        sorted.sort(this::compareAttemptOrder);
        return sorted;
    }

    private long computeStreak(List<AnalysisJdbcRepository.TopicAttemptScoreRow> sortedAttempts) {
        if (sortedAttempts.isEmpty()) {
            return 0L;
        }
        if (sortedAttempts.size() == 1) {
            return 1L;
        }
        long streak = 1L;
        for (int i = sortedAttempts.size() - 1; i > 0; i--) {
            double current = sortedAttempts.get(i).attemptPercentage();
            double previous = sortedAttempts.get(i - 1).attemptPercentage();
            if (current >= previous) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private double computeAttemptProgressPercentage(List<AnalysisJdbcRepository.TopicAttemptScoreRow> sortedAttempts) {
        if (sortedAttempts.size() < 2) {
            return 0.0;
        }
        double previousScore = sortedAttempts.get(sortedAttempts.size() - 2).attemptPercentage();
        double currentScore = sortedAttempts.get(sortedAttempts.size() - 1).attemptPercentage();
        if (previousScore <= 0.0) {
            return 0.0;
        }
        double progress = ((currentScore - previousScore) / previousScore) * 100.0;
        return Math.max(0.0, progress);
    }

    private double computeTrendPercentage(List<AnalysisJdbcRepository.TopicAttemptScoreRow> sortedAttempts) {
        if (sortedAttempts.size() < 2) {
            return 0.0;
        }
        double firstScore = sortedAttempts.get(0).attemptPercentage();
        double lastScore = sortedAttempts.get(sortedAttempts.size() - 1).attemptPercentage();
        if (firstScore <= 0.0) {
            return 0.0;
        }
        return ((lastScore - firstScore) / firstScore) * 100.0;
    }

    private String computeTrendLabel(double trendPercentage) {
        if (trendPercentage > 5.0) {
            return "IMPROVING";
        }
        if (trendPercentage < -5.0) {
            return "DECLINING";
        }
        return "STAGNANT";
    }

    private double computeConsistencyScore(List<Double> scoreTimeline) {
        if (scoreTimeline.isEmpty()) {
            return 0.0;
        }
        double mean = scoreTimeline.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = scoreTimeline.stream()
                .mapToDouble(score -> {
                    double diff = score - mean;
                    return diff * diff;
                })
                .average()
                .orElse(0.0);
        double stddev = Math.sqrt(variance);
        return Math.max(0.0, Math.min(100.0, 100.0 - stddev));
    }

    private double computeConceptMasteryScore(
            AnalysisJdbcRepository.TopicCognitiveStatsRow cognitive,
            double fallbackAccuracy
    ) {
        if (cognitive == null || cognitive.weightedAttemptSum() <= 0.0) {
            return fallbackAccuracy;
        }
        return (cognitive.weightedCorrectSum() * 100.0) / cognitive.weightedAttemptSum();
    }

    private double computeRetentionScore(AnalysisJdbcRepository.TopicCognitiveStatsRow cognitive) {
        if (cognitive == null || cognitive.reattemptCount() <= 0) {
            return 0.0;
        }
        return (cognitive.correctReattemptCount() * 100.0) / cognitive.reattemptCount();
    }

    private double computeLearningEfficiency(
            AnalysisJdbcRepository.TopicCognitiveStatsRow cognitive,
            List<AnalysisJdbcRepository.TopicAttemptScoreRow> sortedAttempts
    ) {
        if (cognitive == null || sortedAttempts.size() < 2) {
            return 0.0;
        }
        double firstScore = sortedAttempts.get(0).attemptPercentage();
        double lastScore = sortedAttempts.get(sortedAttempts.size() - 1).attemptPercentage();
        double scoreImprovement = Math.max(0.0, lastScore - firstScore);
        double totalMinutes = cognitive.totalTimeTakenMs() <= 0 ? 0.0 : (cognitive.totalTimeTakenMs() / 60000.0);
        if (totalMinutes <= 0.0) {
            return 0.0;
        }
        return scoreImprovement / totalMinutes;
    }

    private double computeGuessRate(AnalysisJdbcRepository.TopicCognitiveStatsRow cognitive) {
        if (cognitive == null || cognitive.totalAnswerAttempts() <= 0) {
            return 0.0;
        }
        return (cognitive.fastIncorrectCount() * 100.0) / cognitive.totalAnswerAttempts();
    }

    private String classifyPriority(double priorityScore) {
        if (priorityScore <= 50.0) {
            return "STRONG";
        }
        if (priorityScore <= 120.0) {
            return "MODERATE";
        }
        return "CRITICAL";
    }

    private String buildPriorityReason(
            double accuracyRatePercentage,
            double topicCompletionPercentage,
            double trendPercentage,
            long attemptsCount
    ) {
        List<String> reasons = new ArrayList<>();
        if (accuracyRatePercentage < 40.0) {
            reasons.add("low accuracy");
        }
        if (topicCompletionPercentage < 30.0) {
            reasons.add("low completion");
        }
        if (trendPercentage < 0.0) {
            reasons.add("negative trend");
        }
        if (attemptsCount >= 5 && accuracyRatePercentage < 40.0) {
            reasons.add("high attempts + low accuracy");
        }
        if (reasons.isEmpty()) {
            return "stable";
        }
        return String.join(" + ", reasons);
    }

    private String classifyFailureRisk(double failureRiskScore) {
        if (failureRiskScore < 40.0) {
            return "SAFE";
        }
        if (failureRiskScore < 70.0) {
            return "MEDIUM";
        }
        return "HIGH";
    }

    private int compareAttemptOrder(
            AnalysisJdbcRepository.TopicAttemptScoreRow left,
            AnalysisJdbcRepository.TopicAttemptScoreRow right
    ) {
        if (left.createdAt() == null && right.createdAt() != null) {
            return -1;
        }
        if (left.createdAt() != null && right.createdAt() == null) {
            return 1;
        }
        if (left.createdAt() != null && right.createdAt() != null) {
            int dateCompare = left.createdAt().compareTo(right.createdAt());
            if (dateCompare != 0) {
                return dateCompare;
            }
        }
        return Long.compare(left.paperId(), right.paperId());
    }

    private String topicKey(long subjectId, String topicCode) {
        String topic = topicCode == null ? "" : topicCode.trim().toUpperCase();
        return subjectId + "|" + topic;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        if (second != null && !second.trim().isEmpty()) {
            return second.trim();
        }
        return "";
    }

    private void ensureFeatureEnabled() {
        if (!analysisProperties.isFeatureEnabled()) {
            throw new FeatureDisabledException("User analysis feature is disabled");
        }
    }
}
