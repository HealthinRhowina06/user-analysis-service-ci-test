package com.enoch.analysis.service;

public class FeatureDisabledException extends RuntimeException {
    public FeatureDisabledException(String message) {
        super(message);
    }
}
