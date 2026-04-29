package com.enoch.analysis.dto;

import java.util.List;

public record TopicDetailsResponse(
        List<ModuleTopicDetailDTO> moduleDetails
) {
}
