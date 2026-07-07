package com.qeagent.models;

import java.util.*;

/**
 * Execution context passed between agents and through the workflow.
 * Maintains state across test planning, generation, execution, and triaging stages.
 */
public class ExecutionContext {
    private String contextId;
    private String productArtifact; // Raw PRD/spec content
    private TestPlan testPlan;
    private List<GeneratedTest> generatedTests = new ArrayList<>();
    private List<TestExecutionResult> executionResults = new ArrayList<>();
    private List<Defect> triageDefects = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();
    private long createdAt;
    private long completedAt;
    private WorkflowStatus status = WorkflowStatus.INITIALIZING;
    private List<String> errorLog = new ArrayList<>();

    public ExecutionContext() {
        this.contextId = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
    }

    public ExecutionContext(String contextId) {
        this.contextId = contextId;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getContextId() { return contextId; }

    public String getProductArtifact() { return productArtifact; }
    public void setProductArtifact(String productArtifact) { this.productArtifact = productArtifact; }

    public TestPlan getTestPlan() { return testPlan; }
    public void setTestPlan(TestPlan testPlan) { this.testPlan = testPlan; }

    public List<GeneratedTest> getGeneratedTests() { return generatedTests; }
    public void setGeneratedTests(List<GeneratedTest> generatedTests) { this.generatedTests = generatedTests; }

    public List<TestExecutionResult> getExecutionResults() { return executionResults; }
    public void setExecutionResults(List<TestExecutionResult> executionResults) { this.executionResults = executionResults; }

    public List<Defect> getTriageDefects() { return triageDefects; }
    public void setTriageDefects(List<Defect> triageDefects) { this.triageDefects = triageDefects; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public long getCreatedAt() { return createdAt; }
    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    public WorkflowStatus getStatus() { return status; }
    public void setStatus(WorkflowStatus status) { this.status = status; }

    public List<String> getErrorLog() { return errorLog; }
    public void addError(String error) {
        this.errorLog.add("[" + System.currentTimeMillis() + "] " + error);
    }

    public enum WorkflowStatus {
        INITIALIZING, PLANNING, GENERATING, EXECUTING, TRIAGING, COMPLETED, FAILED
    }
}
