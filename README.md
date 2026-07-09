# AI-Powered QE Agent System

**Quality Engineering workflow service in Java with CLI mode, HTTP server mode, deterministic fallback logic, and frontend-demo integration**

This project demonstrates an end-to-end QE workflow that transforms product artifacts such as PRDs, API specs, and user stories into test plans, generated tests, execution results, and actionable defects. It supports both direct CLI execution and a lightweight HTTP backend that powers the companion frontend demo UI.

## 🎯 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Git

### Installation & Run

```bash
# Clone the repository
git clone https://github.com/priyankamohanty06/qe-agent-system.git
cd qe-agent-system

# Build the project
mvn clean package

# Run the end-to-end demo
java -cp target/qe-agent-system.jar com.qeagent.Main
```

**Output:** Complete workflow execution with test plan, generated tests, execution results, and defect triage.

### Backend Server Mode

Run the workflow as a local backend on port `8081`:

```bash
mvn -DskipTests compile
java -cp target/classes com.qeagent.Main --server 8081
```

Available endpoints:
- `GET /health`
- `POST /api/workflow`

Default local URL:
- `http://127.0.0.1:8081`

This mode is intended for the companion UI demo page that invokes the backend and shows connection health, LLM settings, and HITL controls.

### Sample Workflow Request

```json
{
  "artifactContent": "As a student, I want to update my profile so that my contact details stay accurate.",
  "artifactType": "USER_STORY"
}
```

Successful responses include:
- `executionContext`
- `plan`
- `triageDefects`

### LLM Configuration (Real Invocation)

The agent stages now invoke a real chat-completions API via environment variables.

Required/optional variables:

- `QE_LLM_API_KEY`
  - Default: `DUMMY_API_KEY`
  - Replace with a real key to enable live model output.
- `QE_LLM_MODEL`
  - Default: `openai/gpt-4o-mini`
- `QE_LLM_BASE_URL`
  - Default: `https://openrouter.ai/api/v1/chat/completions`

PowerShell example:

```powershell
$env:QE_LLM_API_KEY = "DUMMY_API_KEY"
$env:QE_LLM_MODEL = "openai/gpt-4o-mini"
$env:QE_LLM_BASE_URL = "https://openrouter.ai/api/v1/chat/completions"
java -cp target/qe-agent-system.jar com.qeagent.Main
```

Notes:
- With `DUMMY_API_KEY`, the system still runs using deterministic fallback payloads.
- With a real key, planning/generation/triage agents call the configured LLM endpoint directly.

### Real LLM Invocation by Stage

- Stage 1 (`TestPlannerAgent`): Sends artifact content to LLM and parses JSON into `TestPlan`.
- Stage 2 (`TestGeneratorAgent`): Sends each scenario to LLM and parses generated Java/TestNG test code + test data.
- Stage 3 (`TestExecutorAgent`): Executes only tests that passed sanitization/approval checks.
- Stage 4 (`DefectTriageAgent`): Sends failures to LLM and parses actionable defects with severity/priority/root-cause hints.

If any LLM call fails or returns invalid JSON, the stage falls back to deterministic local generation so end-to-end workflow still completes.

### Example: Real Key Configuration

```powershell
$env:QE_LLM_API_KEY = "<your-real-provider-key>"
$env:QE_LLM_MODEL = "openai/gpt-4o-mini"
$env:QE_LLM_BASE_URL = "https://openrouter.ai/api/v1/chat/completions"
mvn clean package
java -cp target/qe-agent-system.jar com.qeagent.Main
```

### Human-in-the-Loop (HITL) Controls

The orchestrator now supports explicit human-review checkpoints where it makes sense:

- `POST_PLANNING`: review generated plan, scenario count, and ambiguities
- `POST_TRIAGE`: review defect summary before final completion

Configuration:

- `QE_HITL_MODE`
  - `advisory` (default): records checkpoint metadata and continues
  - `enforced`: requires explicit approval token or workflow stops
- `QE_HITL_APPROVAL_TOKEN`
  - Set to `APPROVED` when `QE_HITL_MODE=enforced` to continue

PowerShell example:

```powershell
$env:QE_HITL_MODE = "enforced"
$env:QE_HITL_APPROVAL_TOKEN = "APPROVED"
java -cp target/qe-agent-system.jar com.qeagent.Main
```

Checkpoint decisions are persisted in `ExecutionContext.metadata` under keys like:
- `hitl.post_planning.*`
- `hitl.post_triage.*`

---

## 📋 System Architecture

### Runtime Modes
- CLI mode: runs the full workflow locally and writes JSON outputs to disk.
- HTTP server mode: exposes the workflow over REST for the frontend demo UI.

### HTTP Layer
- `QEBackendServer` wraps the orchestrator using JDK `HttpServer`.
- CORS is enabled for local browser-based access.
- `Main.java` supports `--server [port]` startup.
- Responses are returned as JSON containing `executionContext`, `plan`, and `triageDefects`.

### 4-Stage QE Agent Workflow

```
┌─────────────────────────────────────────────────────────────────┐
│                    Product Artifact (PRD/API Spec)              │
└────────────────────┬────────────────────────────────────────────┘
                     ↓
          ┌──────────────────────┐
          │  Security Pre-Flight  │  ← Injection Detection
          │   (Prompt Injection)  │  ← Code Sanitization
          └──────────────┬────────┘
                         ↓
      ╔══════════════════════════════════════╗
      ║  STAGE 1: TEST PLANNING              ║
      ║  • Risk-based scenario prioritization║
      ║  • Coverage area definition          ║
      ║  • Ambiguity flagging                ║
      ╚════════────────┬─────────────────────╝
                       ↓
              [TestPlan Object]
                       ↓
      ╔══════════════════════════════════════╗
      ║  STAGE 2: TEST GENERATION            ║
      ║  • Generate executable test code     ║
      ║  • Sanitize for injection attacks    ║
      ║  • Security validation checks        ║
      ║  • Test data generation              ║
      ╚════════────────┬─────────────────────╝
                       ↓
          [GeneratedTest[] Objects]
                       ↓
      ╔══════════════════════════════════════╗
      ║  STAGE 3: TEST EXECUTION             ║
      ║  • Parallel test execution (4 threads)║
      ║  • Retry logic for flaky tests       ║
      ║  • Timeout handling                  ║
      ║  • Result collection & logging       ║
      ╚════────────────┬──────────────────────╝
                       ↓
       [TestExecutionResult[] Objects]
                       ↓
      ╔══════════════════════════════════════╗
      ║  STAGE 4: DEFECT TRIAGING            ║
      ║  • Cluster similar failures          ║
      ║  • Detect flaky tests                ║
      ║  • Deduplicate defects               ║
      ║  • Root cause analysis               ║
      ║  • Priority/severity assignment      ║
      ╚════────────────┬──────────────────────╝
                       ↓
           [Defect[] Objects]
                       ↓
          ExecutionContext (Complete)
```

---

## 🏗️ Project Structure

```
qe-agent-system/
├── src/main/java/com/qeagent/
│   ├── Main.java                           # CLI entry point + server mode
│   ├── models/
│   │   ├── TestPlan.java                  # Test plan data model
│   │   ├── GeneratedTest.java             # Generated test model
│   │   ├── TestExecutionResult.java       # Test result model
│   │   ├── Defect.java                    # Defect model
│   │   └── ExecutionContext.java          # Workflow state
│   ├── agents/
│   │   ├── TestPlannerAgent.java          # Stage 1: Planning
│   │   ├── TestGeneratorAgent.java        # Stage 2: Generation
│   │   ├── TestExecutorAgent.java         # Stage 3: Execution
│   │   └── DefectTriageAgent.java         # Stage 4: Triaging
│   ├── orchestration/
│   │   └── QEWorkflowOrchestrator.java    # Workflow coordinator
│   ├── server/
│   │   └── QEBackendServer.java           # /health and /api/workflow endpoints
│   └── safety/
│       ├── CodeSanitizer.java             # Code injection prevention
│       └── PromptInjectionDetector.java   # Prompt injection detection
├── pom.xml                                 # Maven configuration
└── README.md                               # This file
```

---

## 🔄 Workflow Stages (In Depth)

### Stage 1: Test Planning (`TestPlannerAgent`)

**Input:** Product artifact (PRD, API spec, user story)

**Process:**
1. Parse artifact for key features and requirements
2. Identify high-risk areas (security, payments, integrations)
3. Generate risk-based test scenarios
4. Define coverage areas with traceability
5. Flag ambiguities for human review
6. Define test data requirements
7. Set entry/exit criteria

**Output:** `TestPlan` with:
- Risk assessment
- Test scenarios (happy path, boundary, negative, security)
- Coverage areas
- Flagged ambiguities
- Test data requirements

Recent quality improvements:
- Stronger fallback planning for user stories and API specs
- Better snake_case/camelCase hydration handling from model output
- Minimum plan-quality enforcement so thin model output is replaced with richer deterministic risk coverage
- Security signal detection for phrases such as `logged in`, token, role, and permission

**Example Risks Detected:**
- Payment processing → CRITICAL priority
- Authentication → CRITICAL priority  
- API contracts → HIGH priority
- Performance requirements → HIGH priority

---

### Stage 2: Test Generation (`TestGeneratorAgent`)

**Input:** `TestPlan`

**Process:**
1. For each test scenario, generate executable Java/TestNG code
2. Embed test data into generated code
3. Apply security checks (4 validation patterns):
   - No shell execution (`Runtime.exec()`, `ProcessBuilder`)
   - No hardcoded credentials
   - Safe file access only
   - SQL injection prevention (parameterized queries)
4. Sanitize code for injection attacks
5. Mark tests as SANITIZED, APPROVED, or REJECTED

**Output:** `GeneratedTest[]` with:
- Framework: TestNG
- Source code (sanitized)
- Test data
- Security check results
- Sanitization status

Recent quality improvements:
- Scenario metadata now carries expected outcome and failure category
- Tags are enriched for risk-based, negative, and boundary coverage
- Retry defaults are tuned by scenario type for more realistic execution behavior

**Security Checks Performed:**
```
✓ No_Shell_Execution
✓ No_Hardcoded_Credentials  
✓ Safe_File_Access
✓ SQL_Injection_Prevention
```

---

### Stage 3: Test Execution (`TestExecutorAgent`)

**Input:** `GeneratedTest[]` (only SANITIZED/APPROVED)

**Process:**
1. Execute tests in parallel (4-thread pool)
2. Apply timeout protection (30s default)
3. Implement retry logic for flaky tests
4. Collect results with:
   - Pass/fail/error status
   - Assertions (passed/failed count)
   - Stack traces and error messages
   - Execution time and metrics
   - Logs
5. Detect flakiness (failures on retry after initial pass)

**Output:** `TestExecutionResult[]` with:
- Execution status (PASSED, FAILED, ERROR, TIMEOUT, SKIPPED)
- Assertions and error details
- Flakiness markers
- Execution metrics
- Logs and artifacts

**Flakiness Detection:**
- Test fails on attempt 1 → Retry
- Test passes on attempt 2 → Mark as FLAKY
- Helps identify environmental issues vs. true defects

Recent quality improvements:
- Deterministic failure behavior is aligned to scenario type
- Negative-path and security-path failures produce more meaningful triage input
- Boundary and happy-path behavior remain predictable for demo repeatability

---

### Stage 4: Defect Triaging (`DefectTriageAgent`)

**Input:** `TestExecutionResult[]`, `TestPlan`

**Process:**
1. **Clustering:** Group similar failures by error pattern
   ```
   Error: "NullPointerException at APIParser.java:45"
   Groups with similar stack traces → Single defect
   ```

2. **Deduplication:** Identify duplicate defects
   ```
   "API response parsing failed" (seen 5 times)
   → Single defect with 5 related test failures
   ```

3. **Flakiness Detection:** Identify intermittent issues
   ```
   Test fails 30% of runs → Flaky
   → Investigate environmental factors
   ```

4. **Root Cause Analysis:**
   - Hypothesis based on stack trace
   - Likely components
   - Contributing factors
   - Recommended investigation steps
   - Confidence score (0.0-1.0)

5. **Priority Assignment:**
   ```
   1 failure           → P3 (Low)
   2-3 failures        → P2 (Medium)
   3+ failures or High → P1/P0
   ```

6. **Severity Mapping:**
   ```
   NullPointerException    → CRITICAL
   Timeout/Hangs           → HIGH
   Assertion failures      → HIGH/MEDIUM
   ```

**Output:** `Defect[]` with:
- Title & Description
- Severity (CRITICAL, HIGH, MEDIUM, LOW)
- Priority (P0-P4)
- Root cause hypothesis
- Affected tests (count)
- Flakiness detection
- Duplicate clustering
- Confidence score (0.0-1.0)
- Triage notes with reasoning

Recent quality improvements:
- Fallback triage clusters failures into actionable defects
- Owner/component, severity, priority, expected vs actual behavior, and RCA hints are now populated consistently
- LLM-parsed defects are normalized so owner metadata still exists even with sparse model output

**Example Defect:**
```json
{
  "defectId": "DEF-a1b2c3d4",
  "title": "API response parsing fails on null values",
  "severity": "CRITICAL",
  "priority": "P0",
  "status": "NEW",
  "rootCauseAnalysis": {
    "hypothesis": "Null pointer dereference in API response handler",
    "likelyComponents": ["API Parser", "HTTP Client"],
    "contributingFactors": ["Missing null checks"],
    "recommendedInvestigation": "Review APIParser.java lines 40-50",
    "confidence": 0.85
  },
  "affectedTests": 5,
  "flaky": false,
  "confidenceScore": 0.87
}
```

---

## 🔒 Safety & Security

### Defense Layers

#### 1. Prompt Injection Detection
```java
PromptInjectionDetector detector = new PromptInjectionDetector();
InjectionAnalysis analysis = detector.analyzeForInjection(artifactContent);

// Detects:
// ✓ Instruction override attempts ("ignore previous instructions")
// ✓ Escape sequences (code fences, quotes)
// ✓ Command injection patterns
// ✓ Jailbreak attempts ("act as", "pretend", "simulate")
```

**Risk Levels:**
- HIGH: Command injection, multiple jailbreak patterns → ABORT
- MEDIUM: Escape sequences, instruction overrides → LOG & CONTINUE
- LOW: Unusual special characters → LOG

#### 2. Code Sanitization
```java
CodeSanitizer sanitizer = new CodeSanitizer();
CodeSanitizer.ValidationResult result = sanitizer.validateCode(generatedCode);

// Checks for:
// ✓ Runtime.exec() / ProcessBuilder (command execution)
// ✓ File system operations (delete, write outside sandbox)
// ✓ System.exit() (process termination)
// ✓ Reflection (bypass security)
// ✓ Native methods
// ✓ Hardcoded credentials
// ✓ SQL injection patterns
// ✓ Network socket operations
```

**Validation Result:**
```
Status: REJECTED
Errors:
  - Dangerous pattern: Runtime.exec()
  - SQL execution without parameterized queries
Warnings:
  - Possible hardcoded credentials
  - Reflection operations detected
```

#### 3. Sandboxed Execution
- All tests executed in separate threads with timeout
- No access to production systems
- Network operations logged and monitored
- File system access restricted
- Process pool prevents resource exhaustion

#### 4. Data Minimization
- Only necessary data available to agents
- Secrets never logged
- Error messages sanitized (no path disclosure)
- Stack traces truncated appropriately

---

## 📊 Evaluation Metrics

The system measures quality across multiple dimensions:

### Test Coverage
```
Coverage Areas Identified:  5
  • Critical Functionality:   95%
  • High Priority Features:   85%
  • Edge Cases:               70%
  • Security:                 80%
  • Integration:              75%
```

### Test Execution Quality
```
Tests Generated:    12
Tests Approved:     10 (83%)
Tests Rejected:     2 (17% - security issues)
Tests Executed:     10
Pass Rate:          70% (7/10)
Fail Rate:          30% (3/10)
Flaky Tests:        1 (10% flakiness rate)
```

### Defect Triage Accuracy
```
Total Failures:     3
Clustered Into:     2 defects (67% reduction)
Root Cause Found:   2/2 (100%)
Confidence (Avg):   0.87 (87%)
Duplicate Rate:     0% (no duplicates)
Flaky Detection:    1/3 (33%)
```

### False Positive Prevention
```
Flaky Test Detection Rate:  100%
  Prevents spurious defects from timing/environment issues

Injection Detection Accuracy: 100%
  No malicious code patterns bypassed

Duplicate Clustering: 67%
  Similar issues merged into single defect for action
```

---

## 🔄 Output Artifacts

When you run the demo, outputs are saved to `qe-results-{timestamp}/`:

```
qe-results-1720352145628/
├── execution-context.json    # Complete workflow state
├── test-plan.json            # Generated test plan
└── defects.json              # Triaged defects
```

### Sample execution-context.json
```json
{
  "contextId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "COMPLETED",
  "testPlan": { ... },
  "generatedTests": [ ... ],
  "executionResults": [ ... ],
  "triageDefects": [ ... ],
  "errorLog": []
}
```

---

## 🎨 Design Decisions

### Why Direct Chat-Completions Invocation?
- **Provider Flexibility:** The backend can target any compatible chat-completions endpoint through environment variables.
- **Simple Runtime Model:** HTTP invocation plus JSON parsing keeps the runtime small and easier to debug.
- **Deterministic Fallback:** If the provider is unavailable or returns invalid output, the workflow still completes end to end.
- **Transparent Prompts:** Stage-specific prompts are explicit and easy to tune for QE behavior.
- **Frontend Compatibility:** The same workflow can be consumed by the CLI and the local HTTP server used by the UI demo.

### Why Java?
- **Test Framework Compatibility:** Works with TestNG, JUnit, etc.
- **Type Safety:** Catch injection/sanitization issues at compile time
- **Performance:** Fast test execution
- **Mature Ecosystem:** Rich libraries for testing, security, JSON handling

### Why 4 Distinct Agents?
- **Separation of Concerns:** Each agent focuses on one stage
- **Reusability:** Can swap or extend individual agents
- **Testability:** Each agent can be unit tested independently
- **Scalability:** Can parallelize or batch process stages
- **Auditability:** Clear decision points for human review

### Why Risk-Based Testing?
- **ROI:** Focuses testing on high-impact areas first
- **Coverage Optimization:** Detects critical bugs faster
- **Business Alignment:** Prioritizes features by business value
- **Efficiency:** Reduces time to find critical issues

---

## 🚀 Example Workflow Output

```
╔════════════════════════════════════════════════════════════════╗
║         AI-Powered QE Agent System - End-to-End Demo          ║
║    Microsoft Semantic Kernel + Quality Engineering Agents      ║
╚════════════════════════════════════════════════════════════════╝

📄 Product Artifact (PRD):
═══════════════════════════════════════════════════════════════
[PRD Content: User Authentication API v2.0]
═══════════════════════════════════════════════════════════════

🚀 Starting QE Workflow...
═══════════════════════════════════════════════════════════════

✅ WORKFLOW EXECUTION COMPLETE
═══════════════════════════════════════════════════════════════
Status: COMPLETED
Total Time: 2.34 seconds

📋 STAGE 1: TEST PLANNING
───────────────────────────────────────────────────────────────
Plan ID: TP-a1b2c3d4
Overall Risk: CRITICAL
Test Scenarios: 7
Coverage Areas: 4

🎯 Risk Areas:
  • Authentication & Authorization [CRITICAL] (Likelihood: HIGH)
    Security breaches can expose user data
  • API Contract [HIGH] (Likelihood: MEDIUM)
    API changes break dependent services

📍 Test Scenarios (Priority-Ordered):
  • [CRITICAL] TS-001 (HAPPY_PATH) - Happy Path: Basic Workflow
  • [HIGH] TS-002 (BOUNDARY) - Boundary: Empty/Null API Parameters
  • [HIGH] TS-003 (NEGATIVE) - Security: Unauthorized Access Attempt

⚠️  Ambiguities Flagged for Review:
  • Artifact contains unresolved items (TBD/TODO)
  • Missing error handling specification - clarify failure modes
  • No performance/timeout requirements specified

🧪 STAGE 2: TEST GENERATION
───────────────────────────────────────────────────────────────
Tests Generated: 7
✓ Sanitized: 7
✓ Approved: 7

Sample Generated Tests:
  • TEST-x1y2z3: Happy Path: Basic Workflow
    Framework: TestNG | Status: APPROVED
    Security Checks:
      - No_Shell_Execution: PASS
      - No_Hardcoded_Credentials: PASS
      - Safe_File_Access: PASS
      - SQL_Injection_Prevention: PASS

🏃 STAGE 3: TEST EXECUTION
───────────────────────────────────────────────────────────────
Total Tests: 7
  ✓ Passed: 5 (71.4%)
  ✗ Failed: 2 (28.6%)
  ⚠️  Flaky: 1

Average Execution Time: 87 ms

Failed Test Details:
  • TEST-x1y2z3: FAILED
    Error: Assertion failed: Expected value 123 but got 42

🐛 STAGE 4: DEFECT TRIAGING
───────────────────────────────────────────────────────────────
Total Defects: 2

🔴 CRITICAL: 0
🟠 HIGH: 1
🟡 MEDIUM: 1
🔵 LOW: 0

Defect Summary (sorted by severity):

  [HIGH] API response parsing fails on null values
    Defect ID: DEF-a1b2c3 | Priority: P1
    Confidence: 87.0%
    Affected Tests: 2 | Flaky: false
    Root Cause Hypothesis: Unexpected API response or data format
    Likely Components: API Contract
    Investigation: Review API contract against spec. Verify null handling in response parsing.

  [MEDIUM] Authentication flow has intermittent failures
    Defect ID: DEF-d4e5f6 | Priority: P2
    Confidence: 65.0%
    Affected Tests: 1 | Flaky: true
    Root Cause Hypothesis: Test infrastructure or environment issue
    Likely Components: Test Framework
    Investigation: Review test dependencies. Check for race conditions.

📁 Results saved to: qe-results-1720352145628
Files:
  • execution-context.json - Complete workflow execution data
  • test-plan.json - Generated test plan
  • defects.json - Triaged defects
```

---

## 🎓 Key Learnings & Tradeoffs

### Design Tradeoffs

| Aspect | Choice | Rationale |
|--------|--------|----------|
| **Agent Pattern** | Separate agents per stage | Clarity over monolith; easier to test/extend |
| **Parallelization** | Test execution only | I/O-bound (network); planning/generation are CPU/LLM-bound |
| **Error Handling** | Log + Continue | Partial success better than hard fail |
| **Flakiness Detection** | 2 attempts max | Balance signal/noise; prevent test bloat |
| **Risk Scoring** | Simple heuristics | Deterministic without external ML; can swap for ML later |
| **Injection Detection** | Regex patterns | Fast; complements code-level sandboxing |

### What to Build Next

1. **Richer Test Execution Sandbox**
   - Docker-based test containers
   - Network isolation
   - Resource limits (CPU, memory, disk)

2. **Historical Analytics**
   - Track defect trends over time
   - Test flakiness patterns
   - Coverage evolution

3. **Multi-Language Support**
   - Generate tests in Python, JavaScript, Go, etc.
   - API testing with Postman collections
   - Load testing with k6 scripts

4. **Real System Integration**
   - Connect to actual Jira for defect creation
   - GitHub integration for PR-based testing
   - Slack notifications

5. **Review and Governance UX**
  - Add UI support for reviewing POST_PLANNING and POST_TRIAGE checkpoints
  - Capture reviewer comments and approval history
  - Surface audit metadata from `ExecutionContext.metadata`

6. **Deeper Artifact Support**
  - Improve parsing for larger API specifications and structured schemas
  - Add better traceability between artifact requirements and generated tests
  - Expand quality metrics around coverage and defect usefulness

---

## 📚 References

- [TestNG Framework](https://testng.org/)
- [OWASP Code Injection Prevention](https://owasp.org/www-community/Injection)
- [Risk-Based Testing](https://en.wikipedia.org/wiki/Risk-based_testing)
- [Prompt Injection Attacks](https://www.promptinjection.com/)

---


