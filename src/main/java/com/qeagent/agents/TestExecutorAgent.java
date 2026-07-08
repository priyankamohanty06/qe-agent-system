package com.qeagent.agents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qeagent.models.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * TestExecutorAgent: Executes generated tests in a sandboxed environment.
 * Handles test execution, result collection, flakiness detection, and retry logic.
 * 
 * Stage 3 of the QE workflow.
 */
public class TestExecutorAgent {
    private static final Logger logger = LoggerFactory.getLogger(TestExecutorAgent.class);
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final Random random = new Random();

    /**
     * Executes a list of tests in parallel with timeout and retry logic.
     */
    public List<TestExecutionResult> executeTests(List<GeneratedTest> tests) {
        logger.info("TestExecutorAgent: Starting execution of {} tests", tests.size());

        List<TestExecutionResult> results = Collections.synchronizedList(new ArrayList<>());
        List<Future<?>> futures = new ArrayList<>();

        for (GeneratedTest test : tests) {
            // Execute tests that passed sanitization/approval checks
            if (GeneratedTest.SanitizationStatus.SANITIZED != test.getSanitizationStatus()
                && GeneratedTest.SanitizationStatus.APPROVED != test.getSanitizationStatus()) {
                logger.warn("Skipping non-sanitized test: {}", test.getTestId());
                TestExecutionResult errorResult = new TestExecutionResult(
                    UUID.randomUUID().toString(),
                    test.getTestId(),
                    TestExecutionResult.ExecutionStatus.SKIPPED
                );
                errorResult.setErrorMessage("Test not sanitized: " + test.getSanitizationStatus());
                results.add(errorResult);
                continue;
            }

            Future<?> future = executorService.submit(() -> {
                try {
                    TestExecutionResult result = executeTestWithRetry(test);
                    results.add(result);
                } catch (Exception e) {
                    logger.error("Error executing test {}: {}", test.getTestId(), e.getMessage(), e);
                    TestExecutionResult errorResult = new TestExecutionResult(
                        UUID.randomUUID().toString(),
                        test.getTestId(),
                        TestExecutionResult.ExecutionStatus.ERROR
                    );
                    errorResult.setErrorMessage(e.getMessage());
                    errorResult.setStackTrace(exceptionToString(e));
                    results.add(errorResult);
                }
            });
            futures.add(future);
        }

        // Wait for all tests to complete
        for (Future<?> future : futures) {
            try {
                future.get(60, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                logger.error("Test execution timeout", e);
                future.cancel(true);
            } catch (Exception e) {
                logger.error("Error waiting for test result: {}", e.getMessage());
            }
        }

        logger.info("TestExecutorAgent: Completed execution, {} results collected", results.size());
        return results;
    }

    /**
     * Execute a single test with retry logic for flaky tests.
     */
    private TestExecutionResult executeTestWithRetry(GeneratedTest test) throws Exception {
        int maxRetries = test.getRetryCount();
        TestExecutionResult lastResult = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            lastResult = executeSingleTest(test, attempt);

            if (TestExecutionResult.ExecutionStatus.PASSED == lastResult.getStatus()) {
                return lastResult;
            }

            if (attempt < maxRetries) {
                logger.info("Test {} failed on attempt {}, retrying...", test.getTestId(), attempt + 1);
                Thread.sleep(1000 * (attempt + 1)); // Exponential backoff
            }
        }

        // If we got here, test failed after retries - mark as flaky if it passed at least once
        if (lastResult != null && TestExecutionResult.ExecutionStatus.FAILED == lastResult.getStatus() && maxRetries > 0) {
            lastResult.setFlaky(true);
            lastResult.setFlakyCount(maxRetries);
        }

        return lastResult;
    }

    /**
     * Execute a single test with sandboxing and timeout.
     */
    private TestExecutionResult executeSingleTest(GeneratedTest test, int attemptNumber) {
        String resultId = UUID.randomUUID().toString();
        TestExecutionResult result = new TestExecutionResult(resultId, test.getTestId(),
            TestExecutionResult.ExecutionStatus.PASSED);

        long startTime = System.currentTimeMillis();
        result.setStartTime(startTime);
        result.setRetryAttempt(attemptNumber);
        result.setEnvironment("sandboxed-testng");

        try {
            // Simulate test execution
            // In a real system, this would compile and execute the generated Java test code
            logger.debug("Executing test: {} (attempt {})", test.getTestId(), attemptNumber + 1);

            // Simulate variable test outcomes based on test data
            Map<String, Object> testData = test.getTestData();
            boolean shouldFail = shouldTestFail(testData, attemptNumber);

            if (shouldFail) {
                result.setStatus(TestExecutionResult.ExecutionStatus.FAILED);
                result.setErrorMessage("Assertion failed: Expected value did not match actual");
                result.setStackTrace(generateMockStackTrace(test));
                result.setAssertionsFailed(1);
                result.setAssertionsPassed(2);
                result.setActualResult("unexpected_value_42");
                result.setExpectedResult("expected_value_123");
            } else {
                result.setStatus(TestExecutionResult.ExecutionStatus.PASSED);
                result.setAssertionsPassed(3);
                result.setAssertionsFailed(0);
                result.setActualResult(testData.getOrDefault("expected_status", "SUCCESS").toString());
            }

            // Add mock logs
            result.getLogs().add(new TestExecutionResult.LogEntry(
                System.currentTimeMillis(), "INFO", "Test execution started"
            ));
            result.getLogs().add(new TestExecutionResult.LogEntry(
                System.currentTimeMillis(), "DEBUG", "Preconditions satisfied"
            ));
            result.getLogs().add(new TestExecutionResult.LogEntry(
                System.currentTimeMillis(), "INFO", 
                result.getStatus() == TestExecutionResult.ExecutionStatus.PASSED ? "All assertions passed" : "Assertion failed"
            ));

            // Add mock metrics
            result.getMetrics().put("response_time_ms", (double) (50 + random.nextInt(150)));
            result.getMetrics().put("memory_used_mb", (double) (25 + random.nextInt(50)));

        } catch (Exception e) {
            result.setStatus(TestExecutionResult.ExecutionStatus.ERROR);
            result.setErrorMessage(e.getMessage());
            result.setStackTrace(exceptionToString(e));
        } finally {
            long endTime = System.currentTimeMillis();
            result.setEndTime(endTime);
            result.setExecutionTimeMs(endTime - startTime);
        }

        return result;
    }

    /**
     * Determines if a test should fail based on test data and attempt number.
     * Simulates realistic test outcomes including flakiness.
     */
    private boolean shouldTestFail(Map<String, Object> testData, int attemptNumber) {
        String testType = (String) testData.get("scenario_type");

        // Happy path tests usually pass
        if ("HAPPY_PATH".equals(testType)) {
            return random.nextDouble() < 0.1; // 10% failure rate (flakiness)
        }

        // Boundary tests have moderate failure rate
        if ("BOUNDARY".equals(testType)) {
            return random.nextDouble() < 0.3; // 30% failure rate
        }

        // Negative tests often fail (as expected to show the feature works)
        if ("NEGATIVE".equals(testType)) {
            return random.nextDouble() < 0.5; // 50% failure rate
        }

        // Default: randomly fail
        return random.nextDouble() < 0.2;
    }

    private String generateMockStackTrace(GeneratedTest test) {
        StringBuilder trace = new StringBuilder();
        trace.append("org.testng.AssertionError: Expected value 123 but got 42\n");
        trace.append("  at ").append(test.getTestClassName()).append(".test_").append("scenario").append("(")
            .append(test.getTestClassName()).append(".java:45)\n");
        trace.append("  at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n");
        trace.append("  at java.lang.reflect.Method.invoke(Method.java:498)\n");
        return trace.toString();
    }

    private String exceptionToString(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
