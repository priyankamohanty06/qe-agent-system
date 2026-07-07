package com.qeagent.safety;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.regex.Pattern;

/**
 * PromptInjectionDetector: Detects and prevents prompt injection attacks
 * in product artifacts (PRDs, user stories, API specs).
 * 
 * Prevents adversarial input from manipulating agent behavior.
 */
public class PromptInjectionDetector {
    private static final Logger logger = LoggerFactory.getLogger(PromptInjectionDetector.class);
    
    // Patterns that indicate potential prompt injection
    private static final Pattern IGNORE_INSTRUCTIONS = Pattern.compile(
        "ignore|forget|disregard|override|new (task|instruction|rule)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );
    
    private static final Pattern PROMPT_ESCAPE = Pattern.compile(
        "```|\"\"\"|\\'\"\'",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern COMMAND_INJECTION = Pattern.compile(
        "(?:execute|run|eval|system|command|shell|bash|sh|cmd|powershell)\\s*\\(",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern JAILBREAK_ATTEMPT = Pattern.compile(
        "pretend|assume|act as|you are|simulate|imagine|role",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );

    /**
     * Analyzes artifact content for prompt injection attempts.
     * Returns detailed findings.
     */
    public InjectionAnalysis analyzeForInjection(String artifactContent) {
        InjectionAnalysis analysis = new InjectionAnalysis();
        
        if (artifactContent == null || artifactContent.trim().isEmpty()) {
            return analysis;
        }

        // Check for ignore/override instructions
        if (IGNORE_INSTRUCTIONS.matcher(artifactContent).find()) {
            analysis.addFinding("Potential instruction override attempt detected");
            analysis.setRiskLevel("MEDIUM");
        }

        // Check for escape sequences
        if (PROMPT_ESCAPE.matcher(artifactContent).find()) {
            analysis.addFinding("Prompt escape sequences detected");
            analysis.setRiskLevel("MEDIUM");
        }

        // Check for command injection
        if (COMMAND_INJECTION.matcher(artifactContent).find()) {
            analysis.addFinding("Potential command injection attempt detected");
            analysis.setRiskLevel("HIGH");
        }

        // Check for jailbreak attempts
        if (JAILBREAK_ATTEMPT.matcher(artifactContent).find()) {
            int matchCount = 0;
            var matcher = JAILBREAK_ATTEMPT.matcher(artifactContent);
            while (matcher.find()) matchCount++;
            
            if (matchCount > 3) {
                analysis.addFinding("Multiple jailbreak/roleplay patterns detected");
                analysis.setRiskLevel("HIGH");
            }
        }

        // Check for unusual special characters concentration
        long specialCharCount = artifactContent.chars()
            .filter(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))
            .count();
        
        if (specialCharCount > artifactContent.length() * 0.2) {
            analysis.addFinding("Unusual concentration of special characters");
            analysis.setRiskLevel("LOW");
        }

        // Check for suspicious length patterns
        if (artifactContent.length() > 100000) {
            analysis.addFinding("Very large artifact - potential obfuscation attempt");
            analysis.setRiskLevel("MEDIUM");
        }

        logger.info("Injection analysis complete: {} findings, risk level: {}",
            analysis.getFindings().size(), analysis.getRiskLevel());
        
        return analysis;
    }

    /**
     * Sanitizes artifact content by removing or neutralizing injection attempts.
     */
    public String sanitizeArtifact(String content) {
        String sanitized = content;
        
        // Remove or comment out suspicious patterns
        sanitized = sanitized.replaceAll(IGNORE_INSTRUCTIONS.pattern(), "[REDACTED]");
        sanitized = sanitized.replaceAll(COMMAND_INJECTION.pattern(), "[REDACTED]");
        
        // Escape prompt delimiters
        sanitized = sanitized.replace("\"\"\"", "\"[ESCAPED]\"");
        
        logger.debug("Artifact sanitization complete");
        return sanitized;
    }

    /**
     * Analysis result for injection attempts
     */
    public static class InjectionAnalysis {
        private List<String> findings = new ArrayList<>();
        private String riskLevel = "LOW";
        private boolean isClean = true;

        public void addFinding(String finding) {
            findings.add(finding);
            isClean = false;
        }

        public List<String> getFindings() { return findings; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public boolean isClean() { return isClean; }
    }
}
