package com.enoch.analysis.dto;

import java.util.List;

public record ModuleTopicDetailDTO(
        Long userId,
        String userName,
        String courseCode,
        String sylCode,
        String subCode,
        String unitCode,
        String modCode,
        long attendedTopics,
        long totalTopics,
        double attendancePercentage,
        List<ModuleTopicItemDTO> topics
) {
}
