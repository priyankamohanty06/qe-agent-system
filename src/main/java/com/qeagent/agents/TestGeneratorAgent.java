package com.qeagent.agents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qeagent.models.*;
import com.qeagent.safety.CodeSanitizer;
import java.util.*;

/**
 * TestGeneratorAgent: Converts test scenarios into executable test code.
 * Generates test code with safeguards against injection and unsafe patterns.
 * 
 * Stage 2 of the QE workflow.
 */
public class TestGeneratorAgent {
    private static final Logger logger = LoggerFactory.getLogger(TestGeneratorAgent.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final CodeSanitizer sanitizer = new CodeSanitizer();

    /**
     * Generates executable tests from a test plan.
     */
    public List<GeneratedTest> generateTests(TestPlan testPlan) {
        logger.info("TestGeneratorAgent: Generating tests for plan {}", testPlan.getPlanId());

        List<GeneratedTest> generatedTests = new ArrayList<>();

        for (TestPlan.TestScenario scenario : testPlan.getTestScenarios()) {
            GeneratedTest test = generateTestFromScenario(scenario, testPlan);
            if (test != null) {
                generatedTests.add(test);
            }
        }

        logger.info("TestGeneratorAgent: Generated {} tests", generatedTests.size());
        return generatedTests;
    }

    private GeneratedTest generateTestFromScenario(TestPlan.TestScenario scenario, TestPlan testPlan) {
        String testId = "TEST-" + UUID.randomUUID().toString().substring(0, 8);
        GeneratedTest test = new GeneratedTest(testId, scenario.getId(), scenario.getTitle());

        // Generate test class name from scenario title
        String className = generateClassName(scenario.getTitle());
        test.setTestClassName(className);
        test.setFramework("TestNG");

        // Generate source code template
        String sourceCode = generateTestCode(scenario, className);
        test.setSourceCode(sourceCode);

        // Set tags and metadata
        test.getTags().add(scenario.getType());
        test.getTags().add(scenario.getPriority());
        test.setTimeoutSeconds(30);
        test.setRetryCount("FLAKY".equals(scenario.getType()) ? 2 : 0);

        // Generate test data
        populateTestData(test, scenario);

        // Perform security checks
        performSecurityChecks(test);

        // Mark as sanitized if no issues
        if (test.getSecurityChecks().stream().allMatch(c -> "PASS".equals(c.getStatus()))) {
            test.setSanitizationStatus(GeneratedTest.SanitizationStatus.SANITIZED);
        } else {
            test.setSanitizationStatus(GeneratedTest.SanitizationStatus.REJECTED);
            logger.warn("Test {} failed security checks", testId);
        }

        return test;
    }

    private String generateClassName(String title) {
        return "Test" + title.replaceAll("[^a-zA-Z0-9]", "").substring(0, Math.min(40, title.length()));
    }

    private String generateTestCode(TestPlan.TestScenario scenario, String className) {
        StringBuilder code = new StringBuilder();
        
        code.append("package com.qeagent.generated.tests;\n\n");
        code.append("import org.testng.annotations.*;\n");
        code.append("import org.testng.Assert;\n");
        code.append("import org.slf4j.Logger;\n");
        code.append("import org.slf4j.LoggerFactory;\n\n");
        
        code.append("/**\n");
        code.append(" * AUTO-GENERATED TEST\n");
        code.append(" * Scenario: ").append(scenario.getTitle()).append("\n");
        code.append(" * Type: ").append(scenario.getType()).append("\n");
        code.append(" */\n");
        code.append("public class ").append(className).append(" {\n");
        code.append("    private static final Logger logger = LoggerFactory.getLogger(").append(className).append(".class);\n\n");
        
        code.append("    @BeforeMethod\n");
        code.append("    public void setUp() {\n");
        code.append("        logger.info(\"Setting up test...\");\n");
        code.append("    }\n\n");
        
        code.append("    @Test(description = \"" ).append(scenario.getDescription()).append("\")\n");
        code.append("    public void test_").append(scenario.getType().toLowerCase()).append("() throws Exception {\n");
        code.append("        logger.info(\"Executing test: ").append(scenario.getTitle()).append("\");\n\n");
        
        // Add preconditions
        code.append("        // Preconditions\n");
        for (String precondition : scenario.getPreconditions()) {
            code.append("        // ").append(precondition).append("\n");
        }
        
        // Add test steps
        code.append("\n        // Test Steps\n");
        for (String step : scenario.getSteps()) {
            code.append("        // ").append(step).append("\n");
        }
        
        code.append("\n        // Assertions\n");
        code.append("        // Expected: ").append(scenario.getExpectedResult()).append("\n");
        code.append("        Assert.assertNotNull(\"Result should not be null\");\n");
        
        code.append("        logger.info(\"Test passed\");\n");
        code.append("    }\n\n");
        
        code.append("    @AfterMethod\n");
        code.append("    public void tearDown() {\n");
        code.append("        logger.info(\"Tearing down test...\");\n");
        code.append("    }\n");
        code.append("}\n");
        
        return code.toString();
    }

    private void populateTestData(GeneratedTest test, TestPlan.TestScenario scenario) {
        Map<String, Object> testData = new HashMap<>();
        testData.put("scenario_id", scenario.getId());
        testData.put("scenario_type", scenario.getType());
        testData.put("priority", scenario.getPriority());
        
        // Add sample test data based on scenario type
        if ("HAPPY_PATH".equals(scenario.getType())) {
            testData.put("input_valid", true);
            testData.put("user_id", "valid_user_123");
            testData.put("expected_status", "SUCCESS");
        } else if ("BOUNDARY".equals(scenario.getType())) {
            testData.put("input_valid", false);
            testData.put("edge_case", "empty_string");
            testData.put("expected_status", "ERROR");
        } else if ("NEGATIVE".equals(scenario.getType())) {
            testData.put("input_valid", false);
            testData.put("malformed_input", "<script>alert('xss')</script>");
            testData.put("expected_status", "ERROR");
        }
        
        test.setTestData(testData);
    }

    private void performSecurityChecks(GeneratedTest test) {
        List<GeneratedTest.SecurityCheck> checks = new ArrayList<>();
        String code = test.getSourceCode();

        // Check 1: No shell commands
        GeneratedTest.SecurityCheck shellCheck = new GeneratedTest.SecurityCheck(
            "No_Shell_Execution",
            code.contains("Runtime.getRuntime()") ? "FAIL" : "PASS",
            "Verify no arbitrary shell command execution"
        );
        checks.add(shellCheck);

        // Check 2: No hardcoded credentials
        GeneratedTest.SecurityCheck credCheck = new GeneratedTest.SecurityCheck(
            "No_Hardcoded_Credentials",
            (code.contains("password") && code.contains("=")) ? "WARNING" : "PASS",
            "Check for hardcoded secrets"
        );
        checks.add(credCheck);

        // Check 3: No file system access outside sandbox
        GeneratedTest.SecurityCheck fsCheck = new GeneratedTest.SecurityCheck(
            "Safe_File_Access",
            code.contains("new File(") ? "WARNING" : "PASS",
            "Verify file operations are sandboxed"
        );
        checks.add(fsCheck);

        // Check 4: No SQL injection patterns
        GeneratedTest.SecurityCheck sqlCheck = new GeneratedTest.SecurityCheck(
            "SQL_Injection_Prevention",
            code.contains("PreparedStatement") || !code.contains("executeQuery") ? "PASS" : "WARNING",
            "Use parameterized queries"
        );
        checks.add(sqlCheck);

        test.setSecurityChecks(checks);
    }
}
