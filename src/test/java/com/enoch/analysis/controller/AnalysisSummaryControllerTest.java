package com.enoch.analysis.controller;

import com.enoch.analysis.service.AnalysisSummaryService;
import com.enoch.analysis.service.FeatureDisabledException;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AnalysisSummaryController.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.import-check.enabled=false",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false"
        }
)
class AnalysisSummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalysisSummaryService analysisSummaryService;


    // =========================================================
    // TEST 1
    // Valid user ID should return HTTP 200
    // =========================================================

    @Test
    void shouldReturn200ForValidUser() throws Exception {

        when(analysisSummaryService.getTopicDetails(any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(
                        get("/as/users/1/summary")
                )
                .andExpect(status().isOk());
    }


    // =========================================================
    // TEST 2
    // Invalid user ID (String instead of Long)
    // Current application behaviour = HTTP 500
    // =========================================================

    @Test
    void shouldReturn500ForInvalidUserId() throws Exception {

        mockMvc.perform(
                        get("/as/users/abc/summary")
                )
                .andExpect(status().isInternalServerError());
    }


    // =========================================================
    // TEST 3
    // Another valid numeric user ID
    // =========================================================

    @Test
    void shouldReturn200ForAnotherValidUser() throws Exception {

        when(analysisSummaryService.getTopicDetails(any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(
                        get("/as/users/10/summary")
                )
                .andExpect(status().isOk());
    }


    // =========================================================
    // TEST 4
    // Large numeric user ID should also be accepted
    // =========================================================

    @Test
    void shouldAcceptLargeUserId() throws Exception {

        when(analysisSummaryService.getTopicDetails(any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(
                        get("/as/users/999999/summary")
                )
                .andExpect(status().isOk());
    }


    // =========================================================
    // TEST 5
    // Empty service result should still return successful response
    // =========================================================

    @Test
    void shouldReturnSuccessWhenServiceReturnsEmptyList() throws Exception {

        when(analysisSummaryService.getTopicDetails(any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(
                        get("/as/users/5/summary")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ));
    }


    // =========================================================
    // TEST 6
    // Service RuntimeException should become HTTP 500
    // =========================================================

    @Test
    void shouldReturn500WhenServiceThrowsException() throws Exception {

        when(analysisSummaryService.getTopicDetails(any()))
                .thenThrow(new RuntimeException("Test service failure"));

        mockMvc.perform(
                        get("/as/users/1/summary")
                )
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ));
    }


    // =========================================================
    // TEST 7
    // Error response should contain status 500
    // =========================================================

    @Test
    void shouldContain500StatusInErrorResponse() throws Exception {

        when(analysisSummaryService.getTopicDetails(any()))
                .thenThrow(new RuntimeException("Test failure"));

        mockMvc.perform(
                        get("/as/users/1/summary")
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }


    // =========================================================
    // TEST 8
    // Error response should contain expected message
    // =========================================================

    @Test
    void shouldContainErrorMessageWhenServiceFails() throws Exception {

        when(analysisSummaryService.getTopicDetails(any()))
                .thenThrow(new RuntimeException("Test failure"));

        mockMvc.perform(
                        get("/as/users/1/summary")
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("Internal server error"));
    }


    // =========================================================
    // TEST 9
    // Verify service is actually called
    // =========================================================

    @Test
    void shouldCallAnalysisService() throws Exception {

        when(analysisSummaryService.getTopicDetails(any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(
                        get("/as/users/1/summary")
                )
                .andExpect(status().isOk());

        verify(analysisSummaryService)
                .getTopicDetails(any());
    }


    // =========================================================
    // TEST 10
    // Feature disabled should return service unavailable
    // =========================================================

    @Test
    void shouldReturn503WhenFeatureDisabled() throws Exception {

        when(analysisSummaryService.getTopicDetails(any()))
                .thenThrow(
                        new FeatureDisabledException(
                                "User analysis feature is disabled"
                        )
                );

        mockMvc.perform(
                        get("/as/users/1/summary")
                )
                .andExpect(status().isServiceUnavailable());
    }


    // =========================================================
    // TEST 11
    // Feature disabled response should contain 503
    // =========================================================

    @Test
    void shouldContain503StatusWhenFeatureDisabled() throws Exception {

        when(analysisSummaryService.getTopicDetails(any()))
                .thenThrow(
                        new FeatureDisabledException(
                                "User analysis feature is disabled"
                        )
                );

        mockMvc.perform(
                        get("/as/users/1/summary")
                )
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }


    // =========================================================
    // TEST 12
    // Feature disabled response should contain message
    // =========================================================

    @Test
    void shouldContainMessageWhenFeatureDisabled() throws Exception {

        when(analysisSummaryService.getTopicDetails(any()))
                .thenThrow(
                        new FeatureDisabledException(
                                "User analysis feature is disabled"
                        )
                );

        mockMvc.perform(
                        get("/as/users/1/summary")
                )
                .andExpect(status().isServiceUnavailable())
                .andExpect(
                        jsonPath("$.message")
                                .value("User analysis feature is disabled")
                );
    }
}