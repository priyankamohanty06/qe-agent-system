package com.qeagent.agents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Shared base for all LLM-backed QE agents.
 *
 * Environment variables:
 * - QE_LLM_API_KEY (default: DUMMY_API_KEY)
 * - QE_LLM_MODEL (default: openai/gpt-4o-mini)
 * - QE_LLM_BASE_URL (default: https://openrouter.ai/api/v1/chat/completions)
 */
public abstract class BaseAgent {
    protected static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(BaseAgent.class);
    private static final MediaType JSON = MediaType.parse("application/json");

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .callTimeout(Duration.ofSeconds(60))
        .build();

    protected String invokeLlmOrFallback(String stageName,
                                         String systemPrompt,
                                         String userPrompt,
                                         Supplier<String> fallbackSupplier) {
        String apiKey = getEnvOrDefault("QE_LLM_API_KEY", "DUMMY_API_KEY");
        String model = getEnvOrDefault("QE_LLM_MODEL", "openai/gpt-4o-mini");
        String baseUrl = getEnvOrDefault("QE_LLM_BASE_URL", "https://openrouter.ai/api/v1/chat/completions");

        if (apiKey.startsWith("DUMMY")) {
            logger.warn("{}: Using dummy API key; falling back to deterministic output", stageName);
            return fallbackSupplier.get();
        }

        try {
            String response = invokeChatCompletion(baseUrl, apiKey, model, systemPrompt, userPrompt);
            if (response == null || response.trim().isEmpty()) {
                logger.warn("{}: Empty LLM response; using fallback", stageName);
                return fallbackSupplier.get();
            }
            return stripCodeFences(response);
        } catch (Exception ex) {
            logger.error("{}: LLM invocation failed, using fallback: {}", stageName, ex.getMessage());
            return fallbackSupplier.get();
        }
    }

    private String invokeChatCompletion(String baseUrl,
                                        String apiKey,
                                        String model,
                                        String systemPrompt,
                                        String userPrompt) throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0.2);

        Map<String, String> sys = new HashMap<>();
        sys.put("role", "system");
        sys.put("content", systemPrompt);

        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", userPrompt);

        payload.put("messages", new Object[]{sys, user});

        Request.Builder builder = new Request.Builder()
            .url(baseUrl)
            .post(RequestBody.create(MAPPER.writeValueAsString(payload), JSON))
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://github.com/priyankamohanty06/qe-agent-system")
            .addHeader("X-Title", "QE Agent System");

        try (Response response = httpClient.newCall(builder.build()).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("HTTP " + response.code() + " from provider");
            }
            String body = response.body().string();
            JsonNode root = MAPPER.readTree(body);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw new IOException("Provider response missing choices[0].message.content");
            }
            return content.asText();
        }
    }

    protected static String stripCodeFences(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("```json", "")
            .replaceAll("```", "")
            .trim();
    }

    private static String getEnvOrDefault(String key, String fallback) {
        String val = System.getenv(key);
        return val == null || val.isBlank() ? fallback : val;
    }
}
