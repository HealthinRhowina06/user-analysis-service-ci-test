package com.enoch.analysis.controller;

import com.enoch.analysis.dto.ModuleTopicDetailDTO;
import com.enoch.analysis.dto.ModuleTopicItemDTO;
import com.enoch.analysis.dto.TopicDetailsResponse;
import com.enoch.analysis.dto.UserTopicDetailDTO;
import com.enoch.analysis.dto.UserSummaryRequest;
import com.enoch.analysis.service.AnalysisSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
public class AnalysisSummaryController {

    @Autowired
    AnalysisSummaryService analysisSummaryService;


    @GetMapping("/as/users/{userId}/summary")
    public ResponseEntity<TopicDetailsResponse> getUserSummary(
            @PathVariable("userId") Long userId,
            @RequestHeader(value = "X-Institution-Id", required = false) Long institutionId
    ) {
        UserSummaryRequest request = new UserSummaryRequest(userId, institutionId);
        List<UserTopicDetailDTO> topicDetails = analysisSummaryService.getTopicDetails(request);
        return ResponseEntity.ok(new TopicDetailsResponse(toModuleDetails(topicDetails)));
    }

    private List<ModuleTopicDetailDTO> toModuleDetails(List<UserTopicDetailDTO> topicDetails) {
        Map<String, List<UserTopicDetailDTO>> grouped = new LinkedHashMap<>();
        for (UserTopicDetailDTO topic : topicDetails) {
            String key = groupKey(topic);
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(topic);
        }

        List<ModuleTopicDetailDTO> moduleDetails = new ArrayList<>();
        for (List<UserTopicDetailDTO> topicsInModule : grouped.values()) {
            if (topicsInModule.isEmpty()) {
                continue;
            }
            UserTopicDetailDTO sample = topicsInModule.get(0);
            long totalTopics = topicsInModule.size();
            long attendedTopics = topicsInModule.stream()
                    .filter(t -> t.count() > 0)
                    .count();
            double attendancePercentage = totalTopics == 0
                    ? 0.0
                    : (attendedTopics * 100.0) / totalTopics;

            List<ModuleTopicItemDTO> topics = topicsInModule.stream()
                    .map(t -> new ModuleTopicItemDTO(
                            t.topicCode(),
                            t.count(),
                            t.highestScore(),
                            t.streak(),
                            t.score(),
                            t.questionsCount(),
                            t.attendedQuestionsCount(),
                            t.topicCompletionPercentage(),
                            t.accuracyRatePercentage(),
                            t.conceptMasteryScore(),
                            t.retentionScore(),
                            t.learningEfficiency(),
                            t.guessRate(),
                            t.consistencyPercentage(),
                            t.performanceIndex(),
                            t.trendPercentage(),
                            t.trendLabel(),
                            t.priorityScore(),
                            t.priorityLabel(),
                            t.priorityReason(),
                            t.failureRiskScore(),
                            t.failureRiskLabel(),
                            t.scoreTimeline(),
                            t.attemptProgressPercentage(),
                            t.lstSubmittedDate(),
                            t.weak(),
                            t.attemptScore()
                    ))
                    .toList();

            moduleDetails.add(new ModuleTopicDetailDTO(
                    sample.userId(),
                    sample.userName(),
                    sample.courseCode(),
                    sample.sylCode(),
                    sample.subCode(),
                    sample.unitCode(),
                    sample.modCode(),
                    attendedTopics,
                    totalTopics,
                    attendancePercentage,
                    topics
            ));
        }
        return moduleDetails;
    }

    private String groupKey(UserTopicDetailDTO topic) {
        return safe(topic.courseCode()) + "|"
                + safe(topic.sylCode()) + "|"
                + safe(topic.subCode()) + "|"
                + safe(topic.unitCode()) + "|"
                + safe(topic.modCode());
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
