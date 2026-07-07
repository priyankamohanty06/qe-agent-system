package com.qeagent.agents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qeagent.models.*;
import java.util.*;

/**
 * TestPlannerAgent: Analyzes product artifacts (PRD, API specs, user stories)
 * and generates a risk-based test plan with clear prioritization and coverage areas.
 * 
 * Stage 1 of the QE workflow.
 */
public class TestPlannerAgent {
    private static final Logger logger = LoggerFactory.getLogger(TestPlannerAgent.class);
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Generates a test plan from a product artifact.
     * In a real system, this would call an LLM via Semantic Kernel.
     * This version demonstrates the workflow with hardcoded logic and simulated AI reasoning.
     */
    public TestPlan generateTestPlan(String artifactContent, TestPlan.ArtifactType type) {
        logger.info("TestPlannerAgent: Starting test plan generation for artifact type: {}", type);

        String planId = "TP-" + UUID.randomUUID().toString().substring(0, 8);
        TestPlan plan = new TestPlan(planId, "Product Artifact", type);

        // Step 1: Parse artifact for key features and risks
        Map<String, Object> artifactAnalysis = analyzeArtifact(artifactContent);
        logger.debug("Artifact analysis: {}", artifactAnalysis);

        // Step 2: Identify risks
        TestPlan.RiskAssessment risk = identifyRisks(artifactContent, type);
        plan.setRiskAssessment(risk);

        // Step 3: Create test scenarios based on risk areas
        List<TestPlan.TestScenario> scenarios = generateTestScenarios(artifactContent, risk);
        plan.setTestScenarios(scenarios);

        // Step 4: Define coverage areas
        List<TestPlan.CoverageArea> coverageAreas = defineCoverageAreas(scenarios);
        plan.setCoverageAreas(coverageAreas);

        // Step 5: Identify ambiguities in the spec
        List<String> ambiguities = flagAmbiguities(artifactContent);
        plan.setAmbiguities(ambiguities);

        // Step 6: Define test data requirements
        List<TestPlan.TestDataRequirement> dataReqs = defineTestDataRequirements(scenarios);
        plan.setTestDataRequirements(dataReqs);

        // Step 7: Set entry/exit criteria
        plan.setEntryCriteria(Arrays.asList(
            "System under test is deployed and accessible",
            "Test environment is configured",
            "Required test data is available"
        ));
        plan.setExitCriteria(Arrays.asList(
            "All critical tests passed",
            "Test coverage >= 80%",
            "No unresolved blockers"
        ));

        plan.setRationale("Test plan prioritizes critical user workflows and API contract violations. " +
            "Includes boundary testing and negative scenarios to catch integration issues early.");

        logger.info("TestPlannerAgent: Generated test plan {} with {} scenarios", planId, scenarios.size());
        return plan;
    }

    private Map<String, Object> analyzeArtifact(String content) {
        Map<String, Object> analysis = new HashMap<>();
        
        // Simple heuristic parsing
        String lower = content.toLowerCase();
        analysis.put("has_api_endpoints", lower.contains("api") || lower.contains("endpoint"));
        analysis.put("has_database", lower.contains("database") || lower.contains("data"));
        analysis.put("has_auth", lower.contains("auth") || lower.contains("login") || lower.contains("permission"));
        analysis.put("has_integration", lower.contains("integration") || lower.contains("external"));
        
        return analysis;
    }

    private TestPlan.RiskAssessment identifyRisks(String content, TestPlan.ArtifactType type) {
        TestPlan.RiskAssessment assessment = new TestPlan.RiskAssessment();
        
        String lower = content.toLowerCase();
        List<TestPlan.RiskArea> riskAreas = new ArrayList<>();

        // Check for common high-risk areas
        if (lower.contains("payment") || lower.contains("transaction")) {
            riskAreas.add(new TestPlan.RiskArea(
                "Payment Processing",
                "CRITICAL",
                "HIGH",
                "Financial transactions require high reliability and security"
            ));
        }

        if (lower.contains("auth") || lower.contains("security")) {
            riskAreas.add(new TestPlan.RiskArea(
                "Authentication & Authorization",
                "CRITICAL",
                "HIGH",
                "Security breaches can expose user data"
            ));
        }

        if (lower.contains("api") || lower.contains("endpoint")) {
            riskAreas.add(new TestPlan.RiskArea(
                "API Contract",
                "HIGH",
                "MEDIUM",
                "API changes break dependent services"
            ));
        }

        if (lower.contains("performance") || lower.contains("scale")) {
            riskAreas.add(new TestPlan.RiskArea(
                "Performance & Scalability",
                "HIGH",
                "MEDIUM",
                "Poor performance impacts user experience"
            ));
        }

        // Default risk areas
        if (riskAreas.isEmpty()) {
            riskAreas.add(new TestPlan.RiskArea(
                "Core Functionality",
                "HIGH",
                "HIGH",
                "Primary user workflows must work reliably"
            ));
        }

        assessment.setRiskAreas(riskAreas);
        assessment.setOverallRisk(riskAreas.stream()
            .anyMatch(r -> "CRITICAL".equals(r.getSeverity())) ? "CRITICAL" : "HIGH");
        assessment.setJustification("Risk assessment based on artifact analysis: " +
            riskAreas.size() + " high-risk areas identified");

        return assessment;
    }

    private List<TestPlan.TestScenario> generateTestScenarios(String content, TestPlan.RiskAssessment risk) {
        List<TestPlan.TestScenario> scenarios = new ArrayList<>();
        int scenarioNum = 1;

        String lower = content.toLowerCase();

        // Happy path scenarios
        TestPlan.TestScenario happyPath = new TestPlan.TestScenario();
        happyPath.setId("TS-" + scenarioNum++);
        happyPath.setTitle("Happy Path: Basic Workflow");
        happyPath.setDescription("Execute primary user workflow with valid data");
        happyPath.setPriority("CRITICAL");
        happyPath.setType("HAPPY_PATH");
        happyPath.getSteps().add("Step 1: Initialize system");
        happyPath.getSteps().add("Step 2: Execute primary action");
        happyPath.getSteps().add("Step 3: Verify expected outcome");
        happyPath.setExpectedResult("Action completes successfully with expected result");
        scenarios.add(happyPath);

        // Boundary scenarios
        if (lower.contains("api")) {
            TestPlan.TestScenario boundary = new TestPlan.TestScenario();
            boundary.setId("TS-" + scenarioNum++);
            boundary.setTitle("Boundary: Empty/Null API Parameters");
            boundary.setDescription("Test API behavior with boundary input values");
            boundary.setPriority("HIGH");
            boundary.setType("BOUNDARY");
            boundary.getSteps().add("Send request with empty parameters");
            boundary.getSteps().add("Send request with null values");
            boundary.getSteps().add("Verify graceful error handling");
            boundary.setExpectedResult("API returns appropriate error codes without crashing");
            scenarios.add(boundary);
        }

        // Negative scenarios
        TestPlan.TestScenario negative = new TestPlan.TestScenario();
        negative.setId("TS-" + scenarioNum++);
        negative.setTitle("Negative: Invalid Input Handling");
        negative.setDescription("Verify system handles invalid inputs gracefully");
        negative.setPriority("HIGH");
        negative.setType("NEGATIVE");
        negative.getSteps().add("Provide malformed input");
        negative.getSteps().add("Verify error message clarity");
        negative.getSteps().add("Verify system remains stable");
        negative.setExpectedResult("Clear error message, system remains functional");
        scenarios.add(negative);

        // Security scenarios
        if (lower.contains("auth")) {
            TestPlan.TestScenario security = new TestPlan.TestScenario();
            security.setId("TS-" + scenarioNum++);
            security.setTitle("Security: Unauthorized Access Attempt");
            security.setDescription("Verify unauthorized users cannot access protected resources");
            security.setPriority("CRITICAL");
            security.setType("NEGATIVE");
            security.getSteps().add("Attempt access without credentials");
            security.getSteps().add("Attempt access with invalid credentials");
            security.getSteps().add("Attempt privilege escalation");
            security.setExpectedResult("All unauthorized attempts rejected, audit logged");
            scenarios.add(security);
        }

        return scenarios;
    }

    private List<TestPlan.CoverageArea> defineCoverageAreas(List<TestPlan.TestScenario> scenarios) {
        List<TestPlan.CoverageArea> areas = new ArrayList<>();
        
        long criticalCount = scenarios.stream()
            .filter(s -> "CRITICAL".equals(s.getPriority())).count();
        long highCount = scenarios.stream()
            .filter(s -> "HIGH".equals(s.getPriority())).count();

        areas.add(new TestPlan.CoverageArea("Critical Functionality", 95.0, (int)criticalCount));
        areas.add(new TestPlan.CoverageArea("High Priority Features", 85.0, (int)highCount));
        areas.add(new TestPlan.CoverageArea("Edge Cases", 70.0, scenarios.size() / 2));
        areas.add(new TestPlan.CoverageArea("Security", 80.0, scenarios.size() / 3));

        return areas;
    }

    private List<String> flagAmbiguities(String content) {
        List<String> ambiguities = new ArrayList<>();
        String lower = content.toLowerCase();

        if (lower.contains("tbd") || lower.contains("todo") || lower.contains("pending")) {
            ambiguities.add("Artifact contains unresolved items (TBD/TODO)");
        }
        if (!lower.contains("error") && !lower.contains("fail")) {
            ambiguities.add("Missing error handling specification - clarify failure modes");
        }
        if (!lower.contains("timeout") && !lower.contains("performance")) {
            ambiguities.add("No performance/timeout requirements specified");
        }
        if (content.length() < 200) {
            ambiguities.add("Artifact is very brief - may be missing critical details");
        }

        return ambiguities;
    }

    private List<TestPlan.TestDataRequirement> defineTestDataRequirements(List<TestPlan.TestScenario> scenarios) {
        List<TestPlan.TestDataRequirement> requirements = new ArrayList<>();

        requirements.add(new TestPlan.TestDataRequirement("Valid User", 5));
        requirements.add(new TestPlan.TestDataRequirement("Invalid User", 3));
        requirements.add(new TestPlan.TestDataRequirement("Edge Case Data", 2));
        requirements.add(new TestPlan.TestDataRequirement("Security Test Data", 3));

        return requirements;
    }
}
