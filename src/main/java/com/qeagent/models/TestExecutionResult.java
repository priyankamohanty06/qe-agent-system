package com.qeagent.models;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the result of a test execution.
 * Contains pass/fail status, error details, execution metrics, and logs.
 */
public class TestExecutionResult {
    @JsonProperty("result_id")
    private String resultId;
    
    @JsonProperty("test_id")
    private String testId;
    
    @JsonProperty("status")
    private ExecutionStatus status; // PASSED, FAILED, SKIPPED, ERROR, TIMEOUT
    
    @JsonProperty("execution_time_ms")
    private long executionTimeMs;
    
    @JsonProperty("start_time")
    private long startTime;
    
    @JsonProperty("end_time")
    private long endTime;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    @JsonProperty("stack_trace")
    private String stackTrace;
    
    @JsonProperty("assertions_passed")
    private int assertionsPassed;
    
    @JsonProperty("assertions_failed")
    private int assertionsFailed;
    
    @JsonProperty("actual_result")
    private String actualResult;
    
    @JsonProperty("expected_result")
    private String expectedResult;
    
    @JsonProperty("logs")
    private List<LogEntry> logs = new ArrayList<>();
    
    @JsonProperty("artifacts")
    private Map<String, String> artifacts = new HashMap<>(); // Screenshots, HAR files, etc.
    
    @JsonProperty("flaky")
    private boolean flaky = false;
    
    @JsonProperty("flaky_count")
    private int flakyCount = 0; // Number of times this test has been flaky
    
    @JsonProperty("retry_attempt")
    private int retryAttempt = 0;
    
    @JsonProperty("environment")
    private String environment; // Browser version, API endpoint, etc.
    
    @JsonProperty("metrics")
    private Map<String, Double> metrics = new HashMap<>(); // Performance metrics

    // Constructors
    public TestExecutionResult() {}

    public TestExecutionResult(String resultId, String testId, ExecutionStatus status) {
        this.resultId = resultId;
        this.testId = testId;
        this.status = status;
    }

    // Getters and Setters
    public String getResultId() { return resultId; }
    public void setResultId(String resultId) { this.resultId = resultId; }

    public String getTestId() { return testId; }
    public void setTestId(String testId) { this.testId = testId; }

    public ExecutionStatus getStatus() { return status; }
    public void setStatus(ExecutionStatus status) { this.status = status; }

    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }

    public int getAssertionsPassed() { return assertionsPassed; }
    public void setAssertionsPassed(int assertionsPassed) { this.assertionsPassed = assertionsPassed; }

    public int getAssertionsFailed() { return assertionsFailed; }
    public void setAssertionsFailed(int assertionsFailed) { this.assertionsFailed = assertionsFailed; }

    public String getActualResult() { return actualResult; }
    public void setActualResult(String actualResult) { this.actualResult = actualResult; }

    public String getExpectedResult() { return expectedResult; }
    public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }

    public List<LogEntry> getLogs() { return logs; }
    public void setLogs(List<LogEntry> logs) { this.logs = logs; }

    public Map<String, String> getArtifacts() { return artifacts; }
    public void setArtifacts(Map<String, String> artifacts) { this.artifacts = artifacts; }

    public boolean isFlaky() { return flaky; }
    public void setFlaky(boolean flaky) { this.flaky = flaky; }

    public int getFlakyCount() { return flakyCount; }
    public void setFlakyCount(int flakyCount) { this.flakyCount = flakyCount; }

    public int getRetryAttempt() { return retryAttempt; }
    public void setRetryAttempt(int retryAttempt) { this.retryAttempt = retryAttempt; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public Map<String, Double> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Double> metrics) { this.metrics = metrics; }

    public enum ExecutionStatus {
        PASSED, FAILED, SKIPPED, ERROR, TIMEOUT
    }

    /**
     * Log entry from test execution
     */
    public static class LogEntry {
        @JsonProperty("timestamp")
        private long timestamp;
        
        @JsonProperty("level")
        private String level; // INFO, DEBUG, WARN, ERROR
        
        @JsonProperty("message")
        private String message;

        public LogEntry() {}
        public LogEntry(long timestamp, String level, String message) {
            this.timestamp = timestamp;
            this.level = level;
            this.message = message;
        }

        public long getTimestamp() { return timestamp; }
        public String getLevel() { return level; }
        public String getMessage() { return message; }
    }
}
