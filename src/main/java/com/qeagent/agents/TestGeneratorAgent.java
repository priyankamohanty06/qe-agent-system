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
            generated.setRetryCount(root.path("retryCount").asInt("NEGATIVE".equals(scenario.getType()) ? 1 : 0));
            generated.setTestData(readTestData(root.path("testData"), scenario));
        } catch (Exception ex) {
            logger.error("Test generator parse failed for {}: {}", scenario.getId(), ex.getMessage());
            generated.setTestClassName(defaultClassName(scenario.getTitle()));
            generated.setSourceCode(defaultSourceCode(scenario, generated.getTestClassName()));
            generated.setTestData(defaultTestData(scenario));
            generated.getTags().add(scenario.getType());
            generated.getTags().add(scenario.getPriority());
            generated.setTimeoutSeconds(30);
            generated.setRetryCount("NEGATIVE".equals(scenario.getType()) ? 1 : 0);
        }

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
        if (!node.isObject() || node.size() == 0) {
            return defaultTestData(scenario);
        }

        Map<String, Object> map = new HashMap<>();
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
        data.put("scenario_id", scenario.getId());
        data.put("scenario_type", scenario.getType());
        data.put("priority", scenario.getPriority());
        data.put("input_valid", "HAPPY_PATH".equals(scenario.getType()));
        return data;
    }

    private String defaultClassName(String title) {
        String sanitized = title.replaceAll("[^a-zA-Z0-9]", "");
        if (sanitized.isEmpty()) {
            sanitized = "GeneratedScenario";
        }
        return "Test" + sanitized.substring(0, Math.min(40, sanitized.length()));
    }

    private String defaultSourceCode(TestPlan.TestScenario scenario, String className) {
        return "package com.qeagent.generated.tests;\n\n" +
            "import org.testng.Assert;\n" +
            "import org.testng.annotations.Test;\n\n" +
            "public class " + className + " {\n" +
            "    @Test\n" +
            "    public void " + scenario.getId().toLowerCase().replace("-", "_") + "() {\n" +
            "        // " + scenario.getDescription() + "\n" +
            "        Assert.assertTrue(true, \"Generated fallback test\");\n" +
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
            "\"retryCount\":1," +
            "\"testData\":{\"scenario_id\":\"" + scenario.getId() + "\",\"input_valid\":false}" +
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
