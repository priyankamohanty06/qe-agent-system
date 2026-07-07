package com.qeagent.models;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a triaged defect identified from test execution failures.
 * Contains severity, root cause analysis, deduplication info, and ownership.
 */
public class Defect {
    @JsonProperty("defect_id")
    private String defectId;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("severity")
    private Severity severity; // CRITICAL, HIGH, MEDIUM, LOW
    
    @JsonProperty("priority")
    private Priority priority; // P0, P1, P2, P3, P4
    
    @JsonProperty("status")
    private DefectStatus status; // NEW, ASSIGNED, IN_PROGRESS, RESOLVED, DUPLICATE, WONTFIX
    
    @JsonProperty("test_results")
    private List<String> testResultIds = new ArrayList<>(); // Results that triggered this defect
    
    @JsonProperty("root_cause_analysis")
    private RootCauseAnalysis rootCauseAnalysis;
    
    @JsonProperty("affected_component")
    private String affectedComponent;
    
    @JsonProperty("reproduction_steps")
    private List<String> reproductionSteps = new ArrayList<>();
    
    @JsonProperty("expected_behavior")
    private String expectedBehavior;
    
    @JsonProperty("actual_behavior")
    private String actualBehavior;
    
    @JsonProperty("error_logs")
    private List<String> errorLogs = new ArrayList<>();
    
    @JsonProperty("is_flaky")
    private boolean isFlaky;
    
    @JsonProperty("flaky_rate")
    private double flakyRate; // Percentage of flakiness (0-1)
    
    @JsonProperty("is_duplicate")
    private boolean isDuplicate;
    
    @JsonProperty("duplicate_of")
    private String duplicateOf; // Reference to original defect ID
    
    @JsonProperty("similar_defects")
    private List<String> similarDefects = new ArrayList<>(); // Clustered similar issues
    
    @JsonProperty("assigned_to")
    private String assignedTo;
    
    @JsonProperty("created_at")
    private long createdAt;
    
    @JsonProperty("updated_at")
    private long updatedAt;
    
    @JsonProperty("tags")
    private List<String> tags = new ArrayList<>();
    
    @JsonProperty("confidence_score")
    private double confidenceScore; // 0-1, how confident the triage is
    
    @JsonProperty("triage_notes")
    private String triageNotes; // AI reasoning for categorization

    // Constructors
    public Defect() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Defect(String defectId, String title, Severity severity) {
        this();
        this.defectId = defectId;
        this.title = title;
        this.severity = severity;
    }

    // Getters and Setters
    public String getDefectId() { return defectId; }
    public void setDefectId(String defectId) { this.defectId = defectId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public DefectStatus getStatus() { return status; }
    public void setStatus(DefectStatus status) { this.status = status; }

    public List<String> getTestResultIds() { return testResultIds; }
    public void setTestResultIds(List<String> testResultIds) { this.testResultIds = testResultIds; }

    public RootCauseAnalysis getRootCauseAnalysis() { return rootCauseAnalysis; }
    public void setRootCauseAnalysis(RootCauseAnalysis rootCauseAnalysis) { this.rootCauseAnalysis = rootCauseAnalysis; }

    public String getAffectedComponent() { return affectedComponent; }
    public void setAffectedComponent(String affectedComponent) { this.affectedComponent = affectedComponent; }

    public List<String> getReproductionSteps() { return reproductionSteps; }
    public List<String> getErrorLogs() { return errorLogs; }

    public boolean isFlaky() { return isFlaky; }
    public void setFlaky(boolean flaky) { isFlaky = flaky; }

    public double getFlakyRate() { return flakyRate; }
    public void setFlakyRate(double flakyRate) { this.flakyRate = flakyRate; }

    public boolean isDuplicate() { return isDuplicate; }
    public void setDuplicate(boolean duplicate) { isDuplicate = duplicate; }

    public String getDuplicateOf() { return duplicateOf; }
    public void setDuplicateOf(String duplicateOf) { this.duplicateOf = duplicateOf; }

    public List<String> getSimilarDefects() { return similarDefects; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public List<String> getTags() { return tags; }
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getTriageNotes() { return triageNotes; }
    public void setTriageNotes(String triageNotes) { this.triageNotes = triageNotes; }

    public void setExpectedBehavior(String expectedBehavior) { this.expectedBehavior = expectedBehavior; }
    public void setActualBehavior(String actualBehavior) { this.actualBehavior = actualBehavior; }

    public enum Severity {
        CRITICAL, HIGH, MEDIUM, LOW
    }

    public enum Priority {
        P0, P1, P2, P3, P4
    }

    public enum DefectStatus {
        NEW, ASSIGNED, IN_PROGRESS, RESOLVED, DUPLICATE, WONTFIX
    }

    /**
     * Root cause analysis for a defect
     */
    public static class RootCauseAnalysis {
        @JsonProperty("hypothesis")
        private String hypothesis; // Initial guess at root cause
        
        @JsonProperty("likely_components")
        private List<String> likelyComponents = new ArrayList<>();
        
        @JsonProperty("contributing_factors")
        private List<String> contributingFactors = new ArrayList<>();
        
        @JsonProperty("recommended_investigation")
        private String recommendedInvestigation;
        
        @JsonProperty("confidence")
        private double confidence; // 0-1

        public RootCauseAnalysis() {}

        public String getHypothesis() { return hypothesis; }
        public void setHypothesis(String hypothesis) { this.hypothesis = hypothesis; }

        public List<String> getLikelyComponents() { return likelyComponents; }
        public List<String> getContributingFactors() { return contributingFactors; }
        public String getRecommendedInvestigation() { return recommendedInvestigation; }
        public void setRecommendedInvestigation(String recommendedInvestigation) { 
            this.recommendedInvestigation = recommendedInvestigation; 
        }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
}
