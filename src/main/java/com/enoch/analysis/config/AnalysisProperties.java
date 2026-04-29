package com.enoch.analysis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "analysis")
public class AnalysisProperties {

    private boolean featureEnabled = true;
    private SummaryProperties summary = new SummaryProperties();

    public boolean isFeatureEnabled() {
        return featureEnabled;
    }

    public void setFeatureEnabled(boolean featureEnabled) {
        this.featureEnabled = featureEnabled;
    }

    public SummaryProperties getSummary() {
        return summary;
    }

    public void setSummary(SummaryProperties summary) {
        this.summary = summary;
    }

    public static class SummaryProperties {
        private double defaultPassPercentage = 50.0;
        private List<String> excludeQtypes = new ArrayList<>(List.of("MOCKTEST", "COMPETITION"));

        public double getDefaultPassPercentage() {
            return defaultPassPercentage;
        }

        public void setDefaultPassPercentage(double defaultPassPercentage) {
            this.defaultPassPercentage = defaultPassPercentage;
        }

        public List<String> getExcludeQtypes() {
            return excludeQtypes;
        }

        public void setExcludeQtypes(List<String> excludeQtypes) {
            this.excludeQtypes = excludeQtypes;
        }
    }
}
