package com.qeagent.safety;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.regex.Pattern;

/**
 * CodeSanitizer: Validates and sanitizes generated test code to prevent
 * injection attacks, unsafe patterns, and malicious code execution.
 * 
 * Implements defense-in-depth with multiple sanitization layers.
 */
public class CodeSanitizer {
    private static final Logger logger = LoggerFactory.getLogger(CodeSanitizer.class);
    
    // Patterns for dangerous operations
    private static final Pattern RUNTIME_EXEC = Pattern.compile("Runtime\\.getRuntime\\(\\)\\.exec", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROCESS_BUILDER = Pattern.compile("new\\s+ProcessBuilder", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_DELETE = Pattern.compile("\\.delete\\(\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SYSTEM_EXIT = Pattern.compile("System\\.exit", Pattern.CASE_INSENSITIVE);
    private static final Pattern REFLECTION = Pattern.compile("Class\\.forName|getMethod|invoke", Pattern.CASE_INSENSITIVE);
    private static final Pattern NATIVE_METHOD = Pattern.compile("native\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_WRITE = Pattern.compile("new\\s+File\\(|FileWriter|FileOutputStream", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_EXECUTE = Pattern.compile("executeQuery|executeUpdate|execute(?!Test)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JDBC_DRIVER = Pattern.compile("forName\\(.*jdbc", Pattern.CASE_INSENSITIVE);
    private static final Pattern NETWORK_SOCKET = Pattern.compile("new\\s+Socket|ServerSocket|URLConnection\\.openConnection", Pattern.CASE_INSENSITIVE);

    /**
     * Validates if generated code is safe to execute.
     * Returns validation result with detailed issues.
     */
    public ValidationResult validateCode(String code) {
        ValidationResult result = new ValidationResult();
        
        if (code == null || code.trim().isEmpty()) {
            result.addIssue("ERROR", "Code is empty");
            result.setValid(false);
            return result;
        }

        // Check for dangerous patterns
        checkDangerousPatterns(code, result);
        
        // Check for suspicious strings (potential injection)
        checkSuspiciousStrings(code, result);
        
        // Check structure validity
        checkCodeStructure(code, result);
        
        // Set overall validity
        result.setValid(result.getErrors().isEmpty());
        
        logger.info("Code validation: {} - {} issues found",
            result.isValid() ? "PASS" : "FAIL",
            result.getErrors().size() + result.getWarnings().size());
        
        return result;
    }

    private void checkDangerousPatterns(String code, ValidationResult result) {
        // Check for command execution
        if (RUNTIME_EXEC.matcher(code).find()) {
            result.addIssue("ERROR", "Dangerous pattern: Runtime.exec() - arbitrary command execution");
        }
        if (PROCESS_BUILDER.matcher(code).find()) {
            result.addIssue("ERROR", "Dangerous pattern: ProcessBuilder - process execution");
        }
        
        // Check for file system operations
        if (FILE_DELETE.matcher(code).find()) {
            result.addIssue("ERROR", "Dangerous pattern: File deletion operations");
        }
        if (FILE_WRITE.matcher(code).find()) {
            result.addIssue("WARNING", "Potentially dangerous: File write operations outside sandbox");
        }
        
        // Check for system control
        if (SYSTEM_EXIT.matcher(code).find()) {
            result.addIssue("ERROR", "Dangerous pattern: System.exit() - process termination");
        }
        
        // Check for reflection
        if (REFLECTION.matcher(code).find()) {
            result.addIssue("WARNING", "Potentially dangerous: Reflection operations may bypass security checks");
        }
        
        // Check for native code
        if (NATIVE_METHOD.matcher(code).find()) {
            result.addIssue("ERROR", "Dangerous pattern: Native method declaration");
        }
        
        // Check for network operations
        if (NETWORK_SOCKET.matcher(code).find()) {
            result.addIssue("WARNING", "Potentially dangerous: Network socket operations");
        }
        
        // Check for SQL without parameterization
        if (SQL_EXECUTE.matcher(code).find() && !code.contains("PreparedStatement")) {
            result.addIssue("WARNING", "SQL execution detected without parameterized queries - SQL injection risk");
        }
        if (JDBC_DRIVER.matcher(code).find()) {
            result.addIssue("WARNING", "Direct JDBC usage - ensure connections use sandboxed database");
        }
    }

    private void checkSuspiciousStrings(String code, ValidationResult result) {
        String lower = code.toLowerCase();
        
        // Check for hardcoded credentials
        if ((lower.contains("password") || lower.contains("secret") || lower.contains("token")) &&
            (code.contains("=\"") || code.contains("= '\"")) &&
            !code.contains("getenv") && !code.contains("@")) {
            result.addIssue("WARNING", "Possible hardcoded credentials detected");
        }
        
        // Check for SQL-like strings with variables
        if (lower.contains("select") || lower.contains("insert") || lower.contains("update") || lower.contains("delete")) {
            if (code.contains("+") && !code.contains("PreparedStatement")) {
                result.addIssue("WARNING", "SQL string concatenation detected - potential SQL injection");
            }
        }
        
        // Check for suspicious URLs
        if (code.contains("http") && (code.contains("exec") || code.contains("eval"))) {
            result.addIssue("WARNING", "Network request combined with code execution - verify intent");
        }
    }

    private void checkCodeStructure(String code, ValidationResult result) {
        // Basic Java syntax validation
        if (!code.contains("class")) {
            result.addIssue("WARNING", "No class definition found - verify code structure");
        }
        
        // Check for unbalanced braces
        int openBraces = code.split("\\{", -1).length - 1;
        int closeBraces = code.split("\\}", -1).length - 1;
        if (openBraces != closeBraces) {
            result.addIssue("ERROR", "Unbalanced braces - malformed code");
        }
        
        // Check for invalid method declarations in test context
        if (code.contains("void main")) {
            result.addIssue("WARNING", "Test code should not contain main method");
        }
    }

    /**
     * Sanitizes code by removing or replacing dangerous patterns.
     * Returns sanitized code safe for execution.
     */
    public String sanitizeCode(String code) {
        String sanitized = code;
        
        // Remove dangerous patterns
        sanitized = sanitized.replaceAll(RUNTIME_EXEC.pattern(), "// REMOVED: Runtime.exec");
        sanitized = sanitized.replaceAll(PROCESS_BUILDER.pattern(), "// REMOVED: ProcessBuilder");
        sanitized = sanitized.replaceAll(FILE_DELETE.pattern(), "// REMOVED: .delete()");
        sanitized = sanitized.replaceAll(SYSTEM_EXIT.pattern(), "// REMOVED: System.exit");
        sanitized = sanitized.replaceAll(NATIVE_METHOD.pattern(), "// REMOVED: native");
        
        logger.debug("Code sanitization complete");
        return sanitized;
    }

    /**
     * Validation result container
     */
    public static class ValidationResult {
        private boolean valid = true;
        private List<ValidationIssue> errors = new ArrayList<>();
        private List<ValidationIssue> warnings = new ArrayList<>();

        public void addIssue(String level, String message) {
            ValidationIssue issue = new ValidationIssue(level, message);
            if ("ERROR".equals(level)) {
                errors.add(issue);
            } else if ("WARNING".equals(level)) {
                warnings.add(issue);
            }
        }

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public List<ValidationIssue> getErrors() { return errors; }
        public List<ValidationIssue> getWarnings() { return warnings; }
    }

    public static class ValidationIssue {
        public final String level;
        public final String message;

        public ValidationIssue(String level, String message) {
            this.level = level;
            this.message = message;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s", level, message);
        }
    }
}
