package com.qeagent.orchestration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qeagent.agents.*;
import com.qeagent.models.*;
import com.qeagent.safety.*;
import java.util.*;

/**
 * QEWorkflowOrchestrator: Coordinates the end-to-end QE workflow across all 4 stages.
 * 
 * Stages:
 * 1. Test Planning - Analyze artifact and create test plan
 * 2. Test Generation - Generate executable tests from plan
 * 3. Test Execution - Run tests in sandboxed environment
 * 4. Defect Triaging - Analyze results and create defects
 */
public class QEWorkflowOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(QEWorkflowOrchestrator.class);
    private static final String HITL_MODE_ENV = "QE_HITL_MODE";
    private static final String HITL_APPROVAL_ENV = "QE_HITL_APPROVAL_TOKEN";
    
    private final TestPlannerAgent plannerAgent;
    private final TestGeneratorAgent generatorAgent;
    private final TestExecutorAgent executorAgent;
    private final DefectTriageAgent triageAgent;
    private final PromptInjectionDetector injectionDetector;
    private final CodeSanitizer codeSanitizer;
    private final String hitlMode;
    private final String hitlApprovalToken;

    public QEWorkflowOrchestrator() {
        this.plannerAgent = new TestPlannerAgent();
        this.generatorAgent = new TestGeneratorAgent();
        this.executorAgent = new TestExecutorAgent();
        this.triageAgent = new DefectTriageAgent();
        this.injectionDetector = new PromptInjectionDetector();
        this.codeSanitizer = new CodeSanitizer();
        this.hitlMode = readEnvOrDefault(HITL_MODE_ENV, "advisory").toLowerCase(Locale.ROOT);
        this.hitlApprovalToken = readEnvOrDefault(HITL_APPROVAL_ENV, "");
    }

    /**
     * Executes the complete end-to-end QE workflow.
     */
    public ExecutionContext executeWorkflow(String artifactContent, TestPlan.ArtifactType artifactType) {
        logger.info("QEWorkflowOrchestrator: Starting end-to-end QE workflow");
        
        ExecutionContext context = new ExecutionContext();
        context.setProductArtifact(artifactContent);
        context.setStatus(ExecutionContext.WorkflowStatus.INITIALIZING);

        try {
            // Pre-flight: Security checks
            logger.info("Stage 0: Security pre-flight checks");
            if (!performSecurityPreFlight(context, artifactContent)) {
                context.setStatus(ExecutionContext.WorkflowStatus.FAILED);
                context.addError("Security pre-flight checks failed");
                return context;
            }

            // Stage 1: Test Planning
            logger.info("Stage 1: Test Planning");
            context.setStatus(ExecutionContext.WorkflowStatus.PLANNING);
            TestPlan testPlan = executeTestPlanning(context, artifactContent, artifactType);
            if (testPlan == null) {
                context.setStatus(ExecutionContext.WorkflowStatus.FAILED);
                context.addError("Test planning failed");
                return context;
            }
            context.setTestPlan(testPlan);

            if (!humanReviewCheckpoint(
                context,
                "POST_PLANNING",
                String.format("Plan=%s, Scenarios=%d, Ambiguities=%d",
                    testPlan.getPlanId(),
                    testPlan.getTestScenarios().size(),
                    testPlan.getAmbiguities().size())
            )) {
                context.setStatus(ExecutionContext.WorkflowStatus.FAILED);
                context.addError("Human review gate failed at POST_PLANNING");
                return context;
            }

            // Stage 2: Test Generation
            logger.info("Stage 2: Test Generation");
            context.setStatus(ExecutionContext.WorkflowStatus.GENERATING);
            List<GeneratedTest> tests = executeTestGeneration(context, testPlan);
            if (tests.isEmpty()) {
                context.setStatus(ExecutionContext.WorkflowStatus.FAILED);
                context.addError("No tests were generated");
                return context;
            }
            context.setGeneratedTests(tests);

            // Stage 3: Test Execution
            logger.info("Stage 3: Test Execution");
            context.setStatus(ExecutionContext.WorkflowStatus.EXECUTING);
            List<TestExecutionResult> results = executeTests(context, tests);
            context.setExecutionResults(results);

            // Stage 4: Defect Triaging
            logger.info("Stage 4: Defect Triaging");
            context.setStatus(ExecutionContext.WorkflowStatus.TRIAGING);
            List<Defect> defects = triageFailures(context, results, testPlan);
            context.setTriageDefects(defects);

            if (!humanReviewCheckpoint(
                context,
                "POST_TRIAGE",
                String.format("Defects=%d, FailedTests=%d",
                    defects.size(),
                    (int) results.stream().filter(r -> TestExecutionResult.ExecutionStatus.FAILED == r.getStatus()
                        || TestExecutionResult.ExecutionStatus.ERROR == r.getStatus()
                        || TestExecutionResult.ExecutionStatus.TIMEOUT == r.getStatus()).count())
            )) {
                context.setStatus(ExecutionContext.WorkflowStatus.FAILED);
                context.addError("Human review gate failed at POST_TRIAGE");
                return context;
            }

            // Workflow complete
            context.setStatus(ExecutionContext.WorkflowStatus.COMPLETED);
            context.setCompletedAt(System.currentTimeMillis());
            logger.info("QEWorkflowOrchestrator: Workflow completed successfully");

        } catch (Exception e) {
            logger.error("Workflow execution failed: {}", e.getMessage(), e);
            context.setStatus(ExecutionContext.WorkflowStatus.FAILED);
            context.addError("Exception: " + e.getMessage());
        } finally {
            // Cleanup
            executorAgent.shutdown();
        }

        return context;
    }

    /**
     * Stage 0: Security pre-flight checks
     */
    private boolean performSecurityPreFlight(ExecutionContext context, String artifactContent) {
        logger.debug("Performing security pre-flight checks...");

        // Check for injection attempts
        PromptInjectionDetector.InjectionAnalysis injectionAnalysis =
            injectionDetector.analyzeForInjection(artifactContent);
        
        if (!injectionAnalysis.isClean()) {
            logger.warn("Potential injection attempts detected: {}", injectionAnalysis.getFindings());
            if ("HIGH".equals(injectionAnalysis.getRiskLevel())) {
                logger.error("HIGH risk injection detected - aborting workflow");
                context.addError("Security: High-risk injection attempt detected");
                return false;
            }
            // Log warnings but continue for MEDIUM/LOW
            for (String finding : injectionAnalysis.getFindings()) {
                context.addError("Security Warning: " + finding);
            }
        }

        logger.debug("Security pre-flight passed");
        return true;
    }

    /**
     * Stage 1: Test Planning
     */
    private TestPlan executeTestPlanning(ExecutionContext context, String artifactContent,
                                        TestPlan.ArtifactType artifactType) {
        try {
            logger.debug("Executing test planning stage...");
            TestPlan plan = plannerAgent.generateTestPlan(artifactContent, artifactType);

            logger.info("Test plan generated: {} scenarios, {} coverage areas, {} ambiguities",
                plan.getTestScenarios().size(),
                plan.getCoverageAreas().size(),
                plan.getAmbiguities().size());

            // Flag ambiguities for human review
            if (!plan.getAmbiguities().isEmpty()) {
                logger.warn("Ambiguities detected in artifact:");
                plan.getAmbiguities().forEach(a -> logger.warn("  - {}", a));
            }

            return plan;
        } catch (Exception e) {
            logger.error("Test planning failed: {}", e.getMessage(), e);
            context.addError("Test Planning: " + e.getMessage());
            return null;
        }
    }

    /**
     * Stage 2: Test Generation
     */
    private List<GeneratedTest> executeTestGeneration(ExecutionContext context, TestPlan testPlan) {
        try {
            logger.debug("Executing test generation stage...");
            List<GeneratedTest> tests = generatorAgent.generateTests(testPlan);

            // Validate and sanitize all generated tests
            List<GeneratedTest> validTests = new ArrayList<>();
            for (GeneratedTest test : tests) {
                CodeSanitizer.ValidationResult validationResult =
                    codeSanitizer.validateCode(test.getSourceCode());

                if (!validationResult.isValid()) {
                    logger.warn("Test {} failed validation:", test.getTestId());
                    validationResult.getErrors().forEach(e -> logger.warn("  - {}", e));
                    test.setSanitizationStatus(GeneratedTest.SanitizationStatus.REJECTED);
                } else {
                    // All checks passed
                    test.setSanitizationStatus(GeneratedTest.SanitizationStatus.APPROVED);
                    validTests.add(test);
                }
            }

            logger.info("Test generation complete: {} valid tests from {} generated",
                validTests.size(), tests.size());

            return validTests;
        } catch (Exception e) {
            logger.error("Test generation failed: {}", e.getMessage(), e);
            context.addError("Test Generation: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Stage 3: Test Execution
     */
    private List<TestExecutionResult> executeTests(ExecutionContext context, List<GeneratedTest> tests) {
        try {
            logger.debug("Executing test execution stage...");
            List<TestExecutionResult> results = executorAgent.executeTests(tests);

            int passed = (int) results.stream()
                .filter(r -> TestExecutionResult.ExecutionStatus.PASSED == r.getStatus())
                .count();
            int failed = (int) results.stream()
                .filter(r -> TestExecutionResult.ExecutionStatus.FAILED == r.getStatus())
                .count();
            int flaky = (int) results.stream()
                .filter(TestExecutionResult::isFlaky)
                .count();

            logger.info("Test execution complete: {} passed, {} failed, {} flaky",
                passed, failed, flaky);

            return results;
        } catch (Exception e) {
            logger.error("Test execution failed: {}", e.getMessage(), e);
            context.addError("Test Execution: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Stage 4: Defect Triaging
     */
    private List<Defect> triageFailures(ExecutionContext context, List<TestExecutionResult> results,
                                       TestPlan testPlan) {
        try {
            logger.debug("Executing defect triaging stage...");
            List<Defect> defects = triageAgent.triageResults(results, testPlan);

            logger.info("Defect triaging complete: {} defects identified", defects.size());

            // Log defect summary
            for (Defect defect : defects) {
                logger.info("Defect: {} - {} [{}] Confidence: {}",
                    defect.getDefectId(),
                    defect.getTitle(),
                    defect.getSeverity(),
                    String.format("%.1f%%", defect.getConfidenceScore() * 100));
            }

            return defects;
        } catch (Exception e) {
            logger.error("Defect triaging failed: {}", e.getMessage(), e);
            context.addError("Defect Triaging: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private boolean humanReviewCheckpoint(ExecutionContext context, String checkpoint, String summary) {
        String keyPrefix = "hitl." + checkpoint.toLowerCase(Locale.ROOT);
        context.getMetadata().put(keyPrefix + ".mode", hitlMode);
        context.getMetadata().put(keyPrefix + ".summary", summary);

        if (!"enforced".equals(hitlMode)) {
            logger.info("HITL [{}] advisory: {}", checkpoint, summary);
            context.getMetadata().put(keyPrefix + ".decision", "ADVISORY_AUTO_CONTINUE");
            return true;
        }

        boolean approved = "APPROVED".equalsIgnoreCase(hitlApprovalToken);
        if (approved) {
            logger.info("HITL [{}] enforced: approval token accepted", checkpoint);
            context.getMetadata().put(keyPrefix + ".decision", "APPROVED");
            return true;
        }

        logger.warn("HITL [{}] enforced: approval token missing/invalid", checkpoint);
        context.getMetadata().put(keyPrefix + ".decision", "REJECTED");
        context.addError("HITL enforcement active. Set QE_HITL_APPROVAL_TOKEN=APPROVED to proceed.");
        return false;
    }

    private String readEnvOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
