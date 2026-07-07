package com.qeagent.models;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a comprehensive test plan generated from product artifacts.
 * Contains risk-based prioritization, coverage areas, and entry/exit criteria.
 */
public class TestPlan {
    @JsonProperty("plan_id")
    private String planId;
    
    @JsonProperty("artifact_name")
    private String artifactName;
    
    @JsonProperty("artifact_type")
    private ArtifactType artifactType; // PRD, API_SPEC, USER_STORY, CODE_REVIEW
    
    @JsonProperty("risk_assessment")
    private RiskAssessment riskAssessment;
    
    @JsonProperty("test_scenarios")
    private List<TestScenario> testScenarios = new ArrayList<>();
    
    @JsonProperty("coverage_areas")
    private List<CoverageArea> coverageAreas = new ArrayList<>();
    
    @JsonProperty("entry_criteria")
    private List<String> entryCriteria = new ArrayList<>();
    
    @JsonProperty("exit_criteria")
    private List<String> exitCriteria = new ArrayList<>();
    
    @JsonProperty("ambiguities")
    private List<String> ambiguities = new ArrayList<>(); // Flag unclear specs
    
    @JsonProperty("test_data_requirements")
    private List<TestDataRequirement> testDataRequirements = new ArrayList<>();
    
    @JsonProperty("created_at")
    private long createdAt;
    
    @JsonProperty("rationale")
    private String rationale; // Why these scenarios were chosen

    // Constructors
    public TestPlan() {
        this.createdAt = System.currentTimeMillis();
    }

    public TestPlan(String planId, String artifactName, ArtifactType artifactType) {
        this();
        this.planId = planId;
        this.artifactName = artifactName;
        this.artifactType = artifactType;
    }

    // Getters and Setters
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getArtifactName() { return artifactName; }
    public void setArtifactName(String artifactName) { this.artifactName = artifactName; }

    public ArtifactType getArtifactType() { return artifactType; }
    public void setArtifactType(ArtifactType artifactType) { this.artifactType = artifactType; }

    public RiskAssessment getRiskAssessment() { return riskAssessment; }
    public void setRiskAssessment(RiskAssessment riskAssessment) { this.riskAssessment = riskAssessment; }

    public List<TestScenario> getTestScenarios() { return testScenarios; }
    public void setTestScenarios(List<TestScenario> testScenarios) { this.testScenarios = testScenarios; }

    public List<CoverageArea> getCoverageAreas() { return coverageAreas; }
    public void setCoverageAreas(List<CoverageArea> coverageAreas) { this.coverageAreas = coverageAreas; }

    public List<String> getEntryCriteria() { return entryCriteria; }
    public void setEntryCriteria(List<String> entryCriteria) { this.entryCriteria = entryCriteria; }

    public List<String> getExitCriteria() { return exitCriteria; }
    public void setExitCriteria(List<String> exitCriteria) { this.exitCriteria = exitCriteria; }

    public List<String> getAmbiguities() { return ambiguities; }
    public void setAmbiguities(List<String> ambiguities) { this.ambiguities = ambiguities; }

    public List<TestDataRequirement> getTestDataRequirements() { return testDataRequirements; }
    public void setTestDataRequirements(List<TestDataRequirement> testDataRequirements) { this.testDataRequirements = testDataRequirements; }

    public long getCreatedAt() { return createdAt; }
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    /**
     * Enumerations for test plan artifacts
     */
    public enum ArtifactType {
        PRD, API_SPEC, USER_STORY, CODE_REVIEW
    }

    /**
     * Risk assessment for prioritizing tests
     */
    public static class RiskAssessment {
        @JsonProperty("overall_risk")
        private String overallRisk; // CRITICAL, HIGH, MEDIUM, LOW
        
        @JsonProperty("risk_areas")
        private List<RiskArea> riskAreas = new ArrayList<>();
        
        @JsonProperty("justification")
        private String justification;

        public RiskAssessment() {}

        public String getOverallRisk() { return overallRisk; }
        public void setOverallRisk(String overallRisk) { this.overallRisk = overallRisk; }

        public List<RiskArea> getRiskAreas() { return riskAreas; }
        public void setRiskAreas(List<RiskArea> riskAreas) { this.riskAreas = riskAreas; }

        public String getJustification() { return justification; }
        public void setJustification(String justification) { this.justification = justification; }
    }

    /**
     * Specific risk area
     */
    public static class RiskArea {
        @JsonProperty("area")
        private String area;
        
        @JsonProperty("severity")
        private String severity; // CRITICAL, HIGH, MEDIUM, LOW
        
        @JsonProperty("likelihood")
        private String likelihood; // HIGH, MEDIUM, LOW
        
        @JsonProperty("description")
        private String description;

        public RiskArea() {}
        public RiskArea(String area, String severity, String likelihood, String description) {
            this.area = area;
            this.severity = severity;
            this.likelihood = likelihood;
            this.description = description;
        }

        public String getArea() { return area; }
        public String getSeverity() { return severity; }
        public String getLikelihood() { return likelihood; }
        public String getDescription() { return description; }
    }

    /**
     * Represents a test scenario in the plan
     */
    public static class TestScenario {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("priority")
        private String priority; // CRITICAL, HIGH, MEDIUM, LOW
        
        @JsonProperty("type")
        private String type; // HAPPY_PATH, BOUNDARY, NEGATIVE, PERFORMANCE, SECURITY
        
        @JsonProperty("preconditions")
        private List<String> preconditions = new ArrayList<>();
        
        @JsonProperty("steps")
        private List<String> steps = new ArrayList<>();
        
        @JsonProperty("expected_result")
        private String expectedResult;
        
        @JsonProperty("risk_areas_covered")
        private List<String> riskAreasCovered = new ArrayList<>();

        public TestScenario() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public List<String> getPreconditions() { return preconditions; }
        public List<String> getSteps() { return steps; }
        public String getExpectedResult() { return expectedResult; }
        public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }
        public List<String> getRiskAreasCovered() { return riskAreasCovered; }
    }

    /**
     * Coverage area for traceability
     */
    public static class CoverageArea {
        @JsonProperty("area")
        private String area;
        
        @JsonProperty("coverage_percent")
        private double coveragePercent;
        
        @JsonProperty("scenarios_count")
        private int scenariosCount;

        public CoverageArea() {}
        public CoverageArea(String area, double coveragePercent, int scenariosCount) {
            this.area = area;
            this.coveragePercent = coveragePercent;
            this.scenariosCount = scenariosCount;
        }

        public String getArea() { return area; }
        public double getCoveragePercent() { return coveragePercent; }
        public int getScenariosCount() { return scenariosCount; }
    }

    /**
     * Test data requirements
     */
    public static class TestDataRequirement {
        @JsonProperty("entity")
        private String entity;
        
        @JsonProperty("quantity")
        private int quantity;
        
        @JsonProperty("attributes")
        private Map<String, String> attributes = new HashMap<>();

        public TestDataRequirement() {}
        public TestDataRequirement(String entity, int quantity) {
            this.entity = entity;
            this.quantity = quantity;
        }

        public String getEntity() { return entity; }
        public int getQuantity() { return quantity; }
        public Map<String, String> getAttributes() { return attributes; }
    }
}
