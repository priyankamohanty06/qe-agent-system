package com.qeagent.models;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an executable test generated from a test scenario.
 * Contains the actual test code, metadata, and execution parameters.
 */
public class GeneratedTest {
    @JsonProperty("test_id")
    private String testId;
    
    @JsonProperty("scenario_id")
    private String scenarioId;
    
    @JsonProperty("test_name")
    private String testName;
    
    @JsonProperty("test_class_name")
    private String testClassName;
    
    @JsonProperty("framework")
    private String framework; // TestNG, JUnit5, etc.
    
    @JsonProperty("source_code")
    private String sourceCode; // Sanitized, executable test code
    
    @JsonProperty("test_data")
    private Map<String, Object> testData = new HashMap<>();
    
    @JsonProperty("tags")
    private List<String> tags = new ArrayList<>();
    
    @JsonProperty("timeout_seconds")
    private int timeoutSeconds = 30;
    
    @JsonProperty("retry_count")
    private int retryCount = 0;
    
    @JsonProperty("dependencies")
    private List<String> dependencies = new ArrayList<>(); // Other test IDs this depends on
    
    @JsonProperty("generated_at")
    private long generatedAt;
    
    @JsonProperty("sanitization_status")
    private SanitizationStatus sanitizationStatus = SanitizationStatus.PENDING;
    
    @JsonProperty("security_checks")
    private List<SecurityCheck> securityChecks = new ArrayList<>();

    // Constructors
    public GeneratedTest() {
        this.generatedAt = System.currentTimeMillis();
    }

    public GeneratedTest(String testId, String scenarioId, String testName) {
        this();
        this.testId = testId;
        this.scenarioId = scenarioId;
        this.testName = testName;
    }

    // Getters and Setters
    public String getTestId() { return testId; }
    public void setTestId(String testId) { this.testId = testId; }

    public String getScenarioId() { return scenarioId; }
    public void setScenarioId(String scenarioId) { this.scenarioId = scenarioId; }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }

    public String getTestClassName() { return testClassName; }
    public void setTestClassName(String testClassName) { this.testClassName = testClassName; }

    public String getFramework() { return framework; }
    public void setFramework(String framework) { this.framework = framework; }

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public Map<String, Object> getTestData() { return testData; }
    public void setTestData(Map<String, Object> testData) { this.testData = testData; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }

    public long getGeneratedAt() { return generatedAt; }

    public SanitizationStatus getSanitizationStatus() { return sanitizationStatus; }
    public void setSanitizationStatus(SanitizationStatus sanitizationStatus) { this.sanitizationStatus = sanitizationStatus; }

    public List<SecurityCheck> getSecurityChecks() { return securityChecks; }
    public void setSecurityChecks(List<SecurityCheck> securityChecks) { this.securityChecks = securityChecks; }

    public enum SanitizationStatus {
        PENDING, SANITIZED, REJECTED, APPROVED
    }

    /**
     * Security check result for generated code
     */
    public static class SecurityCheck {
        @JsonProperty("check_name")
        private String checkName;
        
        @JsonProperty("status")
        private String status; // PASS, FAIL, WARNING
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("remediation")
        private String remediation;

        public SecurityCheck() {}
        public SecurityCheck(String checkName, String status, String description) {
            this.checkName = checkName;
            this.status = status;
            this.description = description;
        }

        public String getCheckName() { return checkName; }
        public String getStatus() { return status; }
        public String getDescription() { return description; }
        public String getRemediation() { return remediation; }
        public void setRemediation(String remediation) { this.remediation = remediation; }
    }
}
