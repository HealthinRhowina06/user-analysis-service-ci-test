package com.enoch.analysis.service;

import com.enoch.analysis.dto.UserSummaryRequest;
import com.enoch.analysis.dto.UserTopicDetailDTO;

import java.util.List;

public interface AnalysisSummaryService {
    List<UserTopicDetailDTO> getTopicDetails(UserSummaryRequest request);
}
