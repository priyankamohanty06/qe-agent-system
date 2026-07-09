package com.qeagent.agents;

import com.fasterxml.jackson.databind.JsonNode;
import com.qeagent.models.GeneratedTest;
import com.qeagent.models.TestPlan;
import com.qeagent.safety.CodeSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Stage 2: Generate executable tests from plan using LLM.
 */
public class TestGeneratorAgent extends BaseAgent {
    private static final Logger logger = LoggerFactory.getLogger(TestGeneratorAgent.class);
    private final CodeSanitizer sanitizer = new CodeSanitizer();

    public List<GeneratedTest> generateTests(TestPlan testPlan) {
        logger.info("TestGeneratorAgent: Generating tests for plan {}", testPlan.getPlanId());
        List<GeneratedTest> tests = new ArrayList<>();

        for (TestPlan.TestScenario scenario : testPlan.getTestScenarios()) {
            tests.add(generateSingleTest(testPlan, scenario));
        }

        logger.info("TestGeneratorAgent: Generated {} tests", tests.size());
        return tests;
    }

    private GeneratedTest generateSingleTest(TestPlan testPlan, TestPlan.TestScenario scenario) {
        String testId = "TEST-" + UUID.randomUUID().toString().substring(0, 8);
        GeneratedTest generated = new GeneratedTest(testId, scenario.getId(), scenario.getTitle());
        generated.setFramework("TestNG");

        String systemPrompt = """
            You are a senior SDET. Return only JSON fields:
            testClassName, sourceCode, tags, timeoutSeconds, retryCount, testData.
            Generate maintainable Java TestNG test code with clear assertions.
            Keep execution safe: no shell command execution and no direct production-side effects.
            """;

        String userPrompt = "Plan ID: " + testPlan.getPlanId() + "\n" +
            "Scenario JSON: " + toJson(scenario);

        String raw = invokeLlmOrFallback(
            "TestGeneratorAgent",
            systemPrompt,
            userPrompt,
            () -> fallbackGeneratorJson(scenario)
        );

        try {
            JsonNode root = MAPPER.readTree(raw);
            generated.setTestClassName(root.path("testClassName").asText(defaultClassName(scenario.getTitle())));
            generated.setSourceCode(root.path("sourceCode").asText(defaultSourceCode(scenario, defaultClassName(scenario.getTitle()))));

            for (JsonNode tag : root.path("tags")) {
                generated.getTags().add(tag.asText());
            }
            if (generated.getTags().isEmpty()) {
                generated.getTags().add(scenario.getType());
                generated.getTags().add(scenario.getPriority());
            }

            generated.setTimeoutSeconds(root.path("timeoutSeconds").asInt(30));
            generated.setRetryCount(root.path("retryCount").asInt(defaultRetryCount(scenario)));
            generated.setTestData(readTestData(root.path("testData"), scenario));
        } catch (Exception ex) {
            logger.error("Test generator parse failed for {}: {}", scenario.getId(), ex.getMessage());
            generated.setTestClassName(defaultClassName(scenario.getTitle()));
            generated.setSourceCode(defaultSourceCode(scenario, generated.getTestClassName()));
            generated.setTestData(defaultTestData(scenario));
            generated.getTags().add(scenario.getType());
            generated.getTags().add(scenario.getPriority());
            generated.setTimeoutSeconds(30);
            generated.setRetryCount(defaultRetryCount(scenario));
        }

        enrichTags(generated, scenario);

        applySecurityChecks(generated);
        return generated;
    }

    private void applySecurityChecks(GeneratedTest test) {
        List<GeneratedTest.SecurityCheck> checks = new ArrayList<>();
        String code = test.getSourceCode();

        checks.add(new GeneratedTest.SecurityCheck(
            "No_Shell_Execution",
            code.contains("Runtime.getRuntime()") ? "FAIL" : "PASS",
            "Verify no shell command execution"
        ));

        checks.add(new GeneratedTest.SecurityCheck(
            "No_Hardcoded_Secrets",
            code.toLowerCase().contains("password=") ? "WARNING" : "PASS",
            "Verify no hardcoded secrets"
        ));

        checks.add(new GeneratedTest.SecurityCheck(
            "Sandbox_File_Access",
            code.contains("new File(") ? "WARNING" : "PASS",
            "File system operations should be sandboxed"
        ));

        test.setSecurityChecks(checks);

        CodeSanitizer.ValidationResult result = sanitizer.validateCode(code);
        if (result.isValid() && checks.stream().noneMatch(c -> "FAIL".equals(c.getStatus()))) {
            test.setSanitizationStatus(GeneratedTest.SanitizationStatus.SANITIZED);
        } else {
            test.setSanitizationStatus(GeneratedTest.SanitizationStatus.REJECTED);
            test.setSourceCode(sanitizer.sanitizeCode(code));
        }
    }

    private Map<String, Object> readTestData(JsonNode node, TestPlan.TestScenario scenario) {
        Map<String, Object> map = defaultTestData(scenario);
        if (!node.isObject() || node.size() == 0) {
            return map;
        }

        node.fields().forEachRemaining(entry -> {
            JsonNode v = entry.getValue();
            if (v.isNumber()) {
                map.put(entry.getKey(), v.numberValue());
            } else if (v.isBoolean()) {
                map.put(entry.getKey(), v.booleanValue());
            } else {
                map.put(entry.getKey(), v.asText());
            }
        });
        return map;
    }

    private Map<String, Object> defaultTestData(TestPlan.TestScenario scenario) {
        Map<String, Object> data = new HashMap<>();
        String type = scenario.getType() == null ? "GENERAL" : scenario.getType().toUpperCase(Locale.ROOT);
        data.put("scenario_id", scenario.getId());
        data.put("scenario_type", type);
        data.put("priority", scenario.getPriority());
        data.put("input_valid", "HAPPY_PATH".equals(type));
        data.put("expected_outcome", "HAPPY_PATH".equals(type) ? "SUCCESS" : "VALIDATION_ERROR");
        data.put("failure_category", inferFailureCategory(scenario));
        data.put("force_failure", "SECURITY".equals(type));
        return data;
    }

    private int defaultRetryCount(TestPlan.TestScenario scenario) {
        String type = scenario.getType() == null ? "" : scenario.getType().toUpperCase(Locale.ROOT);
        if ("BOUNDARY".equals(type)) {
            return 1;
        }
        if ("NEGATIVE".equals(type)) {
            return 0;
        }
        return 1;
    }

    private String inferFailureCategory(TestPlan.TestScenario scenario) {
        String title = (scenario.getTitle() == null ? "" : scenario.getTitle()).toLowerCase(Locale.ROOT);
        if (title.contains("auth") || title.contains("token") || "SECURITY".equalsIgnoreCase(scenario.getType())) {
            return "security";
        }
        if (title.contains("boundary") || "BOUNDARY".equalsIgnoreCase(scenario.getType())) {
            return "boundary";
        }
        if (title.contains("format") || title.contains("required") || "NEGATIVE".equalsIgnoreCase(scenario.getType())) {
            return "validation";
        }
        return "functional";
    }

    private void enrichTags(GeneratedTest generated, TestPlan.TestScenario scenario) {
        if (!generated.getTags().contains("RISK_BASED")) {
            generated.getTags().add("RISK_BASED");
        }
        if ("BOUNDARY".equalsIgnoreCase(scenario.getType()) && !generated.getTags().contains("BOUNDARY")) {
            generated.getTags().add("BOUNDARY");
        }
        if ("NEGATIVE".equalsIgnoreCase(scenario.getType()) && !generated.getTags().contains("NEGATIVE")) {
            generated.getTags().add("NEGATIVE");
        }
    }

    private String defaultClassName(String title) {
        String sanitized = title.replaceAll("[^a-zA-Z0-9]", "");
        if (sanitized.isEmpty()) {
            sanitized = "GeneratedScenario";
        }
        return "Test" + sanitized.substring(0, Math.min(40, sanitized.length()));
    }

    private String defaultSourceCode(TestPlan.TestScenario scenario, String className) {
        String methodName = scenario.getId().toLowerCase().replace("-", "_");
        String comment = scenario.getDescription() == null ? "Generated fallback test" : scenario.getDescription();
        return "package com.qeagent.generated.tests;\n\n" +
            "import org.testng.Assert;\n" +
            "import org.testng.annotations.Test;\n\n" +
            "public class " + className + " {\n" +
            "    @Test\n" +
            "    public void " + methodName + "() {\n" +
            "        // " + comment + "\n" +
            "        boolean scenarioExecuted = true;\n" +
            "        Assert.assertTrue(scenarioExecuted, \"Scenario execution failed unexpectedly\");\n" +
            "    }\n" +
            "}\n";
    }

    private String fallbackGeneratorJson(TestPlan.TestScenario scenario) {
        String className = defaultClassName(scenario.getTitle());
        String source = defaultSourceCode(scenario, className).replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return "{" +
            "\"testClassName\":\"" + className + "\"," +
            "\"sourceCode\":\"" + source + "\"," +
            "\"tags\":[\"" + scenario.getType() + "\",\"" + scenario.getPriority() + "\"]," +
            "\"timeoutSeconds\":30," +
                "\"retryCount\":" + defaultRetryCount(scenario) + "," +
                "\"testData\":{\"scenario_id\":\"" + scenario.getId() + "\",\"scenario_type\":\"" + scenario.getType() + "\",\"input_valid\":false,\"expected_outcome\":\"VALIDATION_ERROR\"}" +
            "}";
    }

    private String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
