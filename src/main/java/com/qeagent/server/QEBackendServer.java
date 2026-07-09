package com.qeagent.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.qeagent.models.ExecutionContext;
import com.qeagent.models.TestPlan;
import com.qeagent.orchestration.QEWorkflowOrchestrator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class QEBackendServer {
    private static final Logger logger = LoggerFactory.getLogger(QEBackendServer.class);

    private final HttpServer server;
    private final ObjectMapper mapper;
    public QEBackendServer(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        server.createContext("/health", this::handleHealth);
        server.createContext("/api/workflow", this::handleWorkflow);
        server.setExecutor(null);
    }

    public void start() {
        server.start();
        logger.info("QE backend server started");
    }

    public void stop(int delaySeconds) {
        server.stop(delaySeconds);
        logger.info("QE backend server stopped");
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) {
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        sendJson(exchange, 200, Map.of("status", "ok"));
    }

    private void handleWorkflow(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) {
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode root = body.isBlank() ? mapper.createObjectNode() : mapper.readTree(body);

            String artifactContent = getText(root, "artifactContent", "");
            if (artifactContent.isBlank()) {
                artifactContent = getText(root, "prd", "");
            }

            if (artifactContent.isBlank()) {
                sendJson(exchange, 400, Map.of("error", "artifactContent is required"));
                return;
            }

            String artifactTypeText = getText(root, "artifactType", "PRD");
            TestPlan.ArtifactType artifactType = parseArtifactType(artifactTypeText);

            QEWorkflowOrchestrator orchestrator = new QEWorkflowOrchestrator();
            ExecutionContext context = orchestrator.executeWorkflow(artifactContent, artifactType);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("executionContext", context);
            response.put("plan", context.getTestPlan());
            response.put("triageDefects", context.getTriageDefects());

            sendJson(exchange, 200, response);
        } catch (Exception ex) {
            logger.error("Workflow API failed", ex);
            sendJson(exchange, 500, Map.of("error", ex.getMessage()));
        }
    }

    private TestPlan.ArtifactType parseArtifactType(String value) {
        try {
            return TestPlan.ArtifactType.valueOf(value.trim().toUpperCase());
        } catch (Exception ignore) {
            return TestPlan.ArtifactType.PRD;
        }
    }

    private String getText(JsonNode root, String field, String fallback) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return fallback;
        }
        return node.asText(fallback);
    }

    private boolean handleCorsPreflight(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return true;
        }
        return false;
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] payload = mapper.writeValueAsBytes(body);
        addCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, payload.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(payload);
        }
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}
