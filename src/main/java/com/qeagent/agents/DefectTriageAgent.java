package com.qeagent.agents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qeagent.models.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DefectTriageAgent: Analyzes test failures, clusters similar issues, detects flakiness,
 * and creates actionable defects with root cause analysis.
 * 
 * Stage 4 of the QE workflow.
 */
public class DefectTriageAgent {
    private static final Logger logger = LoggerFactory.getLogger(DefectTriageAgent.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Defect> defectRegistry = new HashMap<>();

    /**
     * Triages test execution results into defects.
     * Deduplicates, detects flakiness, and performs root cause analysis.
     */
    public List<Defect> triageResults(List<TestExecutionResult> results, TestPlan testPlan) {
        logger.info("DefectTriageAgent: Triaging {} test results", results.size());

        List<Defect> defects = new ArrayList<>();

        // Step 1: Separate failures from passes
        List<TestExecutionResult> failures = results.stream()
            .filter(r -> TestExecutionResult.ExecutionStatus.FAILED == r.getStatus() ||
                        TestExecutionResult.ExecutionStatus.ERROR == r.getStatus())
            .collect(Collectors.toList());

        logger.info("Found {} failing tests", failures.size());

        // Step 2: Detect flaky tests
        List<TestExecutionResult> flakyTests = failures.stream()
            .filter(TestExecutionResult::isFlaky)
            .collect(Collectors.toList());
        
        if (!flakyTests.isEmpty()) {
            logger.warn("Detected {} flaky tests", flakyTests.size());
        }

        // Step 3: Cluster similar failures
        Map<String, List<TestExecutionResult>> clusteredFailures = clusterFailures(failures);

        // Step 4: Create defects from clusters
        for (Map.Entry<String, List<TestExecutionResult>> cluster : clusteredFailures.entrySet()) {
            Defect defect = createDefectFromCluster(cluster.getKey(), cluster.getValue(), testPlan);
            if (defect != null) {
                defects.add(defect);
            }
        }

        // Step 5: Deduplicate defects
        List<Defect> deduplicatedDefects = deduplicateDefects(defects);

        logger.info("DefectTriageAgent: Created {} defects from {} failures", deduplicatedDefects.size(), failures.size());
        return deduplicatedDefects;
    }

    /**
     * Clusters similar failures by error pattern and affected component.
     */
    private Map<String, List<TestExecutionResult>> clusterFailures(List<TestExecutionResult> failures) {
        Map<String, List<TestExecutionResult>> clusters = new HashMap<>();

        for (TestExecutionResult failure : failures) {
            String clusterKey = extractClusterKey(failure);
            clusters.computeIfAbsent(clusterKey, k -> new ArrayList<>()).add(failure);
        }

        logger.debug("Clustered {} failures into {} groups", failures.size(), clusters.size());
        return clusters;
    }

    /**
     * Extracts a cluster key from a failure (similar errors should have same key).
     */
    private String extractClusterKey(TestExecutionResult failure) {
        // Normalize error messages to group similar failures
        String errorMsg = failure.getErrorMessage() != null ?
            failure.getErrorMessage().replaceAll("\\d+", "X") : "UNKNOWN";
        
        // Take first line of error
        String clusterKey = errorMsg.split("\\n")[0];
        
        // Limit length
        return clusterKey.substring(0, Math.min(100, clusterKey.length()));
    }

    /**
     * Creates a defect from a cluster of similar failures.
     */
    private Defect createDefectFromCluster(String clusterKey, List<TestExecutionResult> failureCluster,
                                          TestPlan testPlan) {
        if (failureCluster.isEmpty()) {
            return null;
        }

        String defectId = "DEF-" + UUID.randomUUID().toString().substring(0, 8);
        TestExecutionResult firstFailure = failureCluster.get(0);

        Defect defect = new Defect(defectId, clusterKey, determineSeverity(failureCluster));
        defect.setStatus(Defect.DefectStatus.NEW);

        // Set description
        StringBuilder description = new StringBuilder();
        description.append("Test failure detected: ").append(clusterKey).append("\n");
        description.append("Affected tests: ").append(failureCluster.size()).append("\n");
        description.append("Error pattern: ").append(firstFailure.getErrorMessage());
        defect.setDescription(description.toString());

        // Set test result references
        failureCluster.forEach(f -> defect.getTestResultIds().add(f.getResultId()));

        // Detect flakiness
        double flakyRate = failureCluster.stream()
            .filter(TestExecutionResult::isFlaky).count() / (double) failureCluster.size();
        if (flakyRate > 0.0) {
            defect.setFlaky(true);
            defect.setFlakyRate(flakyRate);
        }

        // Perform root cause analysis
        RootCauseAnalysis rca = analyzeRootCause(failureCluster, testPlan);
        defect.setRootCauseAnalysis(rca);

        // Set priority based on severity and frequency
        defect.setPriority(determinePriority(failureCluster, rca));

        // Set confidence score
        double confidence = calculateConfidence(failureCluster, rca);
        defect.setConfidenceScore(confidence);

        // Add triaging notes
        defect.setTriageNotes(generateTriageNotes(failureCluster, rca));

        return defect;
    }

    /**
     * Determines severity based on error type and test priority.
     */
    private Defect.Severity determineSeverity(List<TestExecutionResult> failures) {
        boolean hasStackTrace = failures.stream()
            .anyMatch(f -> f.getStackTrace() != null && !f.getStackTrace().isEmpty());
        
        boolean isTimeout = failures.stream()
            .anyMatch(f -> f.getStatus() == TestExecutionResult.ExecutionStatus.TIMEOUT);

        if (isTimeout) {
            return Defect.Severity.HIGH;
        }
        
        if (hasStackTrace && failures.stream()
            .anyMatch(f -> f.getStackTrace().contains("NullPointerException"))) {
            return Defect.Severity.CRITICAL;
        }

        // Check if assertions failed
        boolean hasAssertionFailures = failures.stream()
            .anyMatch(f -> f.getAssertionsFailed() > 0);
        
        return hasAssertionFailures ? Defect.Severity.HIGH : Defect.Severity.MEDIUM;
    }

    /**
     * Determines priority based on failure cluster size and severity.
     */
    private Defect.Priority determinePriority(List<TestExecutionResult> failures,
                                             RootCauseAnalysis rca) {
        if (failures.size() >= 5) {
            return Defect.Priority.P0; // Multiple related failures
        }
        
        if (failures.size() >= 3) {
            return Defect.Priority.P1;
        }
        
        if (failures.size() > 1) {
            return Defect.Priority.P2;
        }
        
        return Defect.Priority.P3;
    }

    /**
     * Performs root cause analysis on a failure cluster.
     */
    private RootCauseAnalysis analyzeRootCause(List<TestExecutionResult> failures,
                                              TestPlan testPlan) {
        Defect.RootCauseAnalysis rca = new Defect.RootCauseAnalysis();

        // Analyze stack traces
        String commonStackTrace = extractCommonStackTrace(failures);
        if (commonStackTrace.contains("NullPointerException")) {
            rca.setHypothesis("Null pointer dereference in API response handling");
            rca.getLikelyComponents().add("API Response Parser");
            rca.getContributingFactors().add("Missing null checks");
        } else if (commonStackTrace.contains("AssertionError")) {
            rca.setHypothesis("Unexpected API response or data format");
            rca.getLikelyComponents().add("API Contract");
            rca.getContributingFactors().add("Response schema mismatch");
        } else {
            rca.setHypothesis("Test infrastructure or environment issue");
            rca.getLikelyComponents().add("Test Framework");
            rca.getContributingFactors().add("Environmental dependency");
        }

        rca.setRecommendedInvestigation("Review API contract against spec. " +
            "Verify null handling in response parsing. Check for recent API changes.");
        rca.setConfidence(0.65); // Moderate confidence without full stack trace analysis

        return rca;
    }

    /**
     * Extracts common elements from stack traces.
     */
    private String extractCommonStackTrace(List<TestExecutionResult> failures) {
        if (failures.isEmpty()) {
            return "";
        }
        
        String first = failures.get(0).getStackTrace() != null ? failures.get(0).getStackTrace() : "";
        return first;
    }

    /**
     * Calculates overall confidence of the triage.
     */
    private double calculateConfidence(List<TestExecutionResult> failures,
                                      Defect.RootCauseAnalysis rca) {
        double confidence = 0.5;
        
        // More failures = higher confidence in pattern
        if (failures.size() > 3) {
            confidence += 0.2;
        }
        
        // Clear error messages improve confidence
        if (failures.stream().allMatch(f -> f.getErrorMessage() != null &&
            !f.getErrorMessage().isEmpty())) {
            confidence += 0.15;
        }
        
        // Stack traces improve confidence
        if (failures.stream().allMatch(f -> f.getStackTrace() != null &&
            !f.getStackTrace().isEmpty())) {
            confidence += 0.15;
        }
        
        // RCA confidence impacts overall
        confidence += rca.getConfidence() * 0.1;
        
        return Math.min(0.95, confidence);
    }

    /**
     * Generates human-readable triage notes.
     */
    private String generateTriageNotes(List<TestExecutionResult> failures,
                                      Defect.RootCauseAnalysis rca) {
        StringBuilder notes = new StringBuilder();
        notes.append("Triage Analysis:\n");
        notes.append("- Failure Pattern: ").append(failures.size()).append(" tests affected\n");
        notes.append("- Root Cause Hypothesis: ").append(rca.getHypothesis()).append("\n");
        notes.append("- Likely Components: ").append(String.join(", ", rca.getLikelyComponents())).append("\n");
        notes.append("- Investigation Needed: ").append(rca.getRecommendedInvestigation()).append("\n");
        notes.append("- Confidence: ").append(String.format("%.1f%%", rca.getConfidence() * 100)).append("\n");
        return notes.toString();
    }

    /**
     * Deduplicates defects by clustering similar issues.
     */
    private List<Defect> deduplicateDefects(List<Defect> defects) {
        List<Defect> deduplicated = new ArrayList<>();
        Set<String> processedTitles = new HashSet<>();

        for (Defect defect : defects) {
            String normalizedTitle = normalizeTitle(defect.getTitle());

            if (!processedTitles.contains(normalizedTitle)) {
                // Find similar defects
                List<Defect> similarDefects = defects.stream()
                    .filter(d -> normalizeTitle(d.getTitle()).equals(normalizedTitle))
                    .collect(Collectors.toList());

                if (similarDefects.size() > 1) {
                    // Mark as duplicate
                    Defect primary = similarDefects.get(0);
                    primary.setSimilarDefects(similarDefects.stream()
                        .skip(1)
                        .map(Defect::getDefectId)
                        .collect(Collectors.toList()));
                    
                    logger.info("Found {} similar defects, marked as duplicates", similarDefects.size() - 1);
                }

                deduplicated.add(similarDefects.get(0));
                processedTitles.add(normalizedTitle);
            }
        }

        logger.info("Deduplicated {} defects to {} unique issues", defects.size(), deduplicated.size());
        return deduplicated;
    }

    /**
     * Normalizes defect titles for deduplication.
     */
    private String normalizeTitle(String title) {
        return title.toLowerCase()
            .replaceAll("\\d+", "X") // Replace numbers
            .replaceAll("\\s+", " ") // Normalize spaces
            .trim();
    }
}
