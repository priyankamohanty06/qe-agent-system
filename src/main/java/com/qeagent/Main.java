package com.qeagent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.qeagent.models.*;
import com.qeagent.orchestration.QEWorkflowOrchestrator;
import com.qeagent.server.QEBackendServer;
import java.util.*;
import java.io.*;

/**
 * Main CLI Entry Point for QE Agent System
 * 
 * Demonstrates end-to-end workflow:
 * 1. Load/create PRD artifact
 * 2. Generate test plan
 * 3. Generate executable tests
 * 4. Execute tests
 * 5. Triage failures into actionable defects
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final ObjectMapper mapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    public static void main(String[] args) throws Exception {
        if (args != null && args.length > 0 && ("--server".equalsIgnoreCase(args[0]) || "server".equalsIgnoreCase(args[0]))) {
            int port = 8081;
            if (args.length > 1) {
                try {
                    port = Integer.parseInt(args[1]);
                } catch (NumberFormatException ignore) {
                    logger.warn("Invalid port '{}', using default 8081", args[1]);
                }
            }

            QEBackendServer backendServer = new QEBackendServer(port);
            backendServer.start();
            System.out.printf("QE backend server is running on http://127.0.0.1:%d\n", port);
            Thread.currentThread().join();
            return;
        }

        System.out.println("\n" +
            "╔════════════════════════════════════════════════════════════════╗\n" +
            "║         AI-Powered QE Agent System - End-to-End Demo          ║\n" +
            "║    Microsoft Semantic Kernel + Quality Engineering Agents      ║\n" +
            "╚════════════════════════════════════════════════════════════════╝\n");

        try {
            // Initialize orchestrator
            QEWorkflowOrchestrator orchestrator = new QEWorkflowOrchestrator();

            // Load sample PRD artifact
            String samplePRD = loadSamplePRD();
            System.out.println("\n📄 Product Artifact (PRD):");
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println(samplePRD);
            System.out.println("═══════════════════════════════════════════════════════════════\n");

            // Execute the complete workflow
            System.out.println("\n🚀 Starting QE Workflow...");
            System.out.println("═══════════════════════════════════════════════════════════════\n");

            long workflowStartTime = System.currentTimeMillis();
            ExecutionContext context = orchestrator.executeWorkflow(samplePRD, TestPlan.ArtifactType.PRD);
            long workflowTime = System.currentTimeMillis() - workflowStartTime;

            // Display workflow results
            displayWorkflowResults(context, workflowTime);

            // Save results to file
            saveResultsToFile(context);

        } catch (Exception e) {
            logger.error("Failed to execute workflow", e);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Loads a sample PRD artifact for demonstration
     */
    private static String loadSamplePRD() {
        return """
    PRODUCT REQUIREMENT DOCUMENT (PRD)
==========================================
Product: User Authentication API v2.0
Version: 1.0
Author: Product Team
Date: 2024-07-07

OVERVIEW
--------
We are building a new authentication service that supports:
1. User registration with email validation
2. Login with username/password
3. JWT token generation
4. Multi-factor authentication (MFA) via SMS
5. Password reset functionality
6. Token refresh and revocation

KEY FEATURES
-----------

1. User Registration
   - Email must be unique and valid format
   - Password must be 8+ characters with mixed case and numbers
   - User activation link sent via email
   - TBD: Email verification timeout

2. Login
   - Support username or email login
   - Rate limiting: Max 5 failed attempts in 15 minutes
   - Return JWT token on success
   - Log failed attempts for security audit

3. MFA
   - SMS-based OTP for high-risk logins
   - OTP valid for 5 minutes
   - Maximum 3 retries per OTP
   - Fallback: Email-based backup codes

4. Security Requirements
   - All passwords must be hashed using bcrypt
   - Tokens expire after 1 hour (refresh tokens 7 days)
   - HTTPS only
   - CORS enabled for approved domains
   - SQL injection prevention via parameterized queries

5. Performance
   - Login must complete in <200ms
   - Support 1000 concurrent users
   - Database connection pooling

ERROR HANDLING
--------------
- Invalid credentials: Return 401 Unauthorized (no info leakage)
- Account locked: Return 429 Too Many Requests
- Invalid token: Return 401 with specific error code
- Server errors: Return 500 with request ID for tracing

TESTING REQUIREMENTS
--------------------
- Test all happy paths with valid data
- Test boundary conditions (empty fields, max length, etc.)
- Test security scenarios (SQL injection, XSS, CSRF)
- Test error handling and recovery
- Test concurrent access and rate limiting
- Test MFA flows and timeout scenarios

AMBIGUITIES
-----------
- What is exact password hashing algorithm? (bcrypt recommended)
- How long should password reset links be valid?
- Should we support biometric authentication?
- API versioning strategy?
""";
    }

    /**
     * Displays comprehensive workflow results
     */
    private static void displayWorkflowResults(ExecutionContext context, long workflowTime) {
        System.out.println("\n\n✅ WORKFLOW EXECUTION COMPLETE");
        System.out.println("═══════════════════════════════════════════════════════════════\n");
        System.out.printf("Status: %s\n", context.getStatus());
        System.out.printf("Total Time: %.2f seconds\n\n", workflowTime / 1000.0);

        // Stage 1: Test Plan Results
        displayTestPlanResults(context.getTestPlan());

        // Stage 2: Test Generation Results
        displayTestGenerationResults(context.getGeneratedTests());

        // Stage 3: Test Execution Results
        displayTestExecutionResults(context.getExecutionResults());

        // Stage 4: Defect Triaging Results
        displayDefectTriagingResults(context.getTriageDefects());

        // Display any errors
        if (!context.getErrorLog().isEmpty()) {
            System.out.println("\n⚠️  ERRORS AND WARNINGS");
            System.out.println("═══════════════════════════════════════════════════════════════");
            for (String error : context.getErrorLog()) {
                System.out.println(error);
            }
        }
    }

    private static void displayTestPlanResults(TestPlan plan) {
        if (plan == null) return;
        
        System.out.println("\n📋 STAGE 1: TEST PLANNING");
        System.out.println("───────────────────────────────────────────────────────────────");
        System.out.printf("Plan ID: %s\n", plan.getPlanId());
        System.out.printf("Overall Risk: %s\n", plan.getRiskAssessment().getOverallRisk());
        System.out.printf("Test Scenarios: %d\n", plan.getTestScenarios().size());
        System.out.printf("Coverage Areas: %d\n\n", plan.getCoverageAreas().size());

        // Display risk areas
        System.out.println("🎯 Risk Areas:");
        for (TestPlan.RiskArea risk : plan.getRiskAssessment().getRiskAreas()) {
            System.out.printf("  • %s [%s] (Likelihood: %s)\n    %s\n",
                risk.getArea(), risk.getSeverity(), risk.getLikelihood(), risk.getDescription());
        }

        // Display test scenarios
        System.out.println("\n📍 Test Scenarios (Priority-Ordered):");
        plan.getTestScenarios().stream()
            .sorted(Comparator.comparing(s -> {
                switch (s.getPriority()) {
                    case "CRITICAL": return 0;
                    case "HIGH": return 1;
                    case "MEDIUM": return 2;
                    default: return 3;
                }
            }))
            .limit(5)
            .forEach(scenario -> System.out.printf(
                "  • [%s] %s (%s) - %s\n",
                scenario.getPriority(), scenario.getId(), scenario.getType(), scenario.getTitle()));

        // Display ambiguities
        if (!plan.getAmbiguities().isEmpty()) {
            System.out.println("\n⚠️  Ambiguities Flagged for Review:");
            plan.getAmbiguities().forEach(a -> System.out.println("  • " + a));
        }

        System.out.println("\nRationale: " + plan.getRationale());
    }

    private static void displayTestGenerationResults(List<GeneratedTest> tests) {
        if (tests.isEmpty()) return;
        
        System.out.println("\n🧪 STAGE 2: TEST GENERATION");
        System.out.println("───────────────────────────────────────────────────────────────");
        System.out.printf("Tests Generated: %d\n\n", tests.size());

        long sanitized = tests.stream()
            .filter(t -> GeneratedTest.SanitizationStatus.SANITIZED == t.getSanitizationStatus())
            .count();
        long approved = tests.stream()
            .filter(t -> GeneratedTest.SanitizationStatus.APPROVED == t.getSanitizationStatus())
            .count();
        long rejected = tests.stream()
            .filter(t -> GeneratedTest.SanitizationStatus.REJECTED == t.getSanitizationStatus())
            .count();

        System.out.printf("✓ Sanitized: %d\n", sanitized);
        System.out.printf("✓ Approved: %d\n", approved);
        if (rejected > 0) System.out.printf("✗ Rejected: %d\n", rejected);

        System.out.println("\nSample Generated Tests:");
        tests.stream().limit(3).forEach(test -> {
            System.out.printf("  • %s: %s\n    Framework: %s | Status: %s\n",
                test.getTestId(), test.getTestName(),
                test.getFramework(), test.getSanitizationStatus());
            if (!test.getSecurityChecks().isEmpty()) {
                System.out.println("    Security Checks:");
                test.getSecurityChecks().forEach(check ->
                    System.out.printf("      - %s: %s\n", check.getCheckName(), check.getStatus()));
            }
        });
    }

    private static void displayTestExecutionResults(List<TestExecutionResult> results) {
        if (results.isEmpty()) return;
        
        System.out.println("\n🏃 STAGE 3: TEST EXECUTION");
        System.out.println("───────────────────────────────────────────────────────────────");

        int passed = (int) results.stream()
            .filter(r -> TestExecutionResult.ExecutionStatus.PASSED == r.getStatus()).count();
        int failed = (int) results.stream()
            .filter(r -> TestExecutionResult.ExecutionStatus.FAILED == r.getStatus()).count();
        int skipped = (int) results.stream()
            .filter(r -> TestExecutionResult.ExecutionStatus.SKIPPED == r.getStatus()).count();
        int flaky = (int) results.stream()
            .filter(TestExecutionResult::isFlaky).count();

        System.out.printf("Total Tests: %d\n", results.size());
        System.out.printf("  ✓ Passed: %d (%.1f%%)\n", passed, (passed * 100.0 / results.size()));
        System.out.printf("  ✗ Failed: %d (%.1f%%)\n", failed, (failed * 100.0 / results.size()));
        if (skipped > 0) System.out.printf("  ⊘ Skipped: %d\n", skipped);
        if (flaky > 0) System.out.printf("  ⚠️  Flaky: %d\n", flaky);

        double avgTime = results.stream()
            .mapToLong(TestExecutionResult::getExecutionTimeMs)
            .average().orElse(0);
        System.out.printf("Average Execution Time: %.0f ms\n\n", avgTime);

        // Show failed tests details
        if (failed > 0) {
            System.out.println("Failed Test Details:");
            results.stream()
                .filter(r -> TestExecutionResult.ExecutionStatus.FAILED == r.getStatus())
                .limit(3)
                .forEach(result -> System.out.printf(
                    "  • %s: %s\n    Error: %s\n",
                    result.getTestId(), result.getStatus(), result.getErrorMessage()));
        }
    }

    private static void displayDefectTriagingResults(List<Defect> defects) {
        if (defects.isEmpty()) {
            System.out.println("\n🐛 STAGE 4: DEFECT TRIAGING");
            System.out.println("───────────────────────────────────────────────────────────────");
            System.out.println("No defects detected - all tests passed! ✓\n");
            return;
        }
        
        System.out.println("\n🐛 STAGE 4: DEFECT TRIAGING");
        System.out.println("───────────────────────────────────────────────────────────────");
        System.out.printf("Total Defects: %d\n\n", defects.size());

        // Group by severity
        long critical = defects.stream().filter(d -> Defect.Severity.CRITICAL == d.getSeverity()).count();
        long high = defects.stream().filter(d -> Defect.Severity.HIGH == d.getSeverity()).count();
        long medium = defects.stream().filter(d -> Defect.Severity.MEDIUM == d.getSeverity()).count();
        long low = defects.stream().filter(d -> Defect.Severity.LOW == d.getSeverity()).count();

        System.out.printf("🔴 CRITICAL: %d\n", critical);
        System.out.printf("🟠 HIGH: %d\n", high);
        System.out.printf("🟡 MEDIUM: %d\n", medium);
        System.out.printf("🔵 LOW: %d\n\n", low);

        // Display defect details
        System.out.println("Defect Summary (sorted by severity):");
        defects.stream()
            .sorted((d1, d2) -> {
                int severityCompare = d2.getSeverity().compareTo(d1.getSeverity());
                if (severityCompare != 0) return severityCompare;
                return Double.compare(d2.getConfidenceScore(), d1.getConfidenceScore());
            })
            .forEach(defect -> {
                System.out.printf(
                    "\n  [%s] %s\n    Defect ID: %s | Priority: %s\n    Confidence: %.1f%%\n" +
                    "    Affected Tests: %d | Flaky: %b\n",
                    defect.getSeverity(), defect.getTitle(),
                    defect.getDefectId(), defect.getPriority(),
                    defect.getConfidenceScore() * 100,
                    defect.getTestResultIds().size(), defect.isFlaky());

                if (defect.getRootCauseAnalysis() != null) {
                    var rca = defect.getRootCauseAnalysis();
                    System.out.printf("    Root Cause Hypothesis: %s\n", rca.getHypothesis());
                    System.out.printf("    Likely Components: %s\n", String.join(", ", rca.getLikelyComponents()));
                    System.out.printf("    Investigation: %s\n", rca.getRecommendedInvestigation());
                }

                if (defect.isDuplicate()) {
                    System.out.printf("    Note: Duplicate of %s\n", defect.getDuplicateOf());
                }
            });
    }

    /**
     * Saves detailed results to JSON files
     */
    private static void saveResultsToFile(ExecutionContext context) throws IOException {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String resultDir = "qe-results-" + timestamp;
        new File(resultDir).mkdirs();

        // Save complete context
        mapper.writeValue(new File(resultDir + "/execution-context.json"), context);

        // Save test plan
        if (context.getTestPlan() != null) {
            mapper.writeValue(new File(resultDir + "/test-plan.json"), context.getTestPlan());
        }

        // Save defects
        if (!context.getTriageDefects().isEmpty()) {
            mapper.writeValue(new File(resultDir + "/defects.json"), context.getTriageDefects());
        }

        System.out.println("\n\n📁 Results saved to: " + resultDir);
        System.out.println("Files:");
        System.out.println("  • execution-context.json - Complete workflow execution data");
        System.out.println("  • test-plan.json - Generated test plan");
        if (!context.getTriageDefects().isEmpty()) {
            System.out.println("  • defects.json - Triaged defects");
        }
    }
}
