# Local Deployment Guide for QE Agent System

## 📋 Prerequisites

Before you start, ensure your laptop has:

### 1. Java Development Kit (JDK) 17 or later

**Check if Java is installed:**
```bash
java -version
```

**Expected output:**
```
openjdk version "17.0.x" 2024-xx-xx
OpenJDK Runtime Environment (build 17.0.x...)
OpenJDK 64-Bit Server VM (build 17.0.x...)
```

**If not installed:**

#### On Windows:
1. Download from: https://adoptium.net/temurin/releases/?version=17
2. Choose: Windows x64 Installer
3. Run the installer
4. During installation, check "Set JAVA_HOME variable"
5. Restart your laptop

#### On macOS:
```bash
# Using Homebrew (recommended)
brew install openjdk@17

# Or download from https://adoptium.net/
```

#### On Linux (Ubuntu/Debian):
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

### 2. Maven 3.8.x or later

**Check if Maven is installed:**
```bash
mvn -version
```

**Expected output:**
```
Apache Maven 3.8.x (...)
Maven home: /path/to/maven
Java version: 17.0.x
```

**If not installed:**

#### On Windows:
1. Download from: https://maven.apache.org/download.cgi
2. Choose: Binary zip archive
3. Extract to: `C:\Program Files\maven` (or similar)
4. Add to PATH:
   - Open System Properties → Environment Variables
   - New System Variable: `MAVEN_HOME` = `C:\Program Files\maven`
   - Edit PATH: Add `%MAVEN_HOME%\bin`
5. Restart your laptop

#### On macOS:
```bash
# Using Homebrew
brew install maven
```

#### On Linux:
```bash
sudo apt update
sudo apt install maven
```

### 3. Git (for cloning the repository)

**Check if Git is installed:**
```bash
git --version
```

**If not installed:**

#### On Windows:
- Download from: https://git-scm.com/download/win
- Run installer with default settings
- Restart your laptop

#### On macOS:
```bash
brew install git
```

#### On Linux:
```bash
sudo apt install git
```

---

## 🚀 Step-by-Step Deployment

### Step 1: Clone the Repository

```bash
# Choose a directory for the project (e.g., ~/projects or C:\projects)
cd ~/projects  # or cd C:\projects on Windows

# Clone the repository
git clone https://github.com/priyankamohanty06/qe-agent-system.git

# Navigate to the project directory
cd qe-agent-system
```

**Verify the directory structure:**
```bash
ls -la  # On macOS/Linux
dir     # On Windows
```

**You should see:**
```
DESIGN_DOCUMENT.md
DELIVERABLES.md
README.md
pom.xml
src/
target/  (will appear after build)
```

### Step 2: Build the Project

```bash
# Navigate to project root (if not already there)
cd ~/projects/qe-agent-system

# Clean and build
mvn clean package
```

**What happens:**
1. Maven downloads dependencies from Maven Central Repository (~30-60 seconds on first run)
2. Compiles Java source code
3. Runs any tests
4. Creates JAR file: `target/qe-agent-system.jar`

**Expected output (last few lines):**
```
[INFO] Building jar: /path/to/target/qe-agent-system.jar
[INFO] --------
[INFO] BUILD SUCCESS
[INFO] --------
[INFO] Total time: XX.XXXs
```

**If build fails:**

**Error: "Maven command not found"**
- Ensure Maven is in your PATH
- Try: `mvn --version`
- If not found, reinstall Maven and restart your laptop

**Error: "Java version not supported"**
- Ensure you have JDK 17 or later (not JRE)
- Check: `java -version` should show "openjdk" or "javac -version"

**Error: "Could not find artifact"**
- Check your internet connection
- Try: `mvn clean install` (instead of package)
- Clear Maven cache: `rm -rf ~/.m2/repository` (macOS/Linux) or `rmdir %USERPROFILE%\.m2\repository` (Windows)

### Step 3: Run the Demo

```bash
# Run the QE Agent System
java -cp target/qe-agent-system.jar com.qeagent.Main
```

**Expected output:**
```
╔════════════════════════════════════════════════════════════════════════════╗
║         AI-Powered QE Agent System - End-to-End Demo              ║
║    Microsoft Semantic Kernel + Quality Engineering Agents      ║
╚════════════════════════════════════════════════════════════════════════════╝

📄 Product Artifact (PRD):
────────────────────────────────────────────────────────────────────────────
[PRD Content...]
────────────────────────────────────────────────────────────────────────────

🚀 Starting QE Workflow...
────────────────────────────────────────────────────────────────────────────

✅ WORKFLOW EXECUTION COMPLETE
...
📁 Results saved to: qe-results-1720352145628
Files:
  • execution-context.json - Complete workflow execution data
  • test-plan.json - Generated test plan
  • defects.json - Triaged defects
```

**Execution time:** ~2-3 seconds

### Step 4: View Results

**List output files:**
```bash
# Navigate to the most recent results directory
ls qe-results-*/  # macOS/Linux
dir qe-results-*  # Windows
```

**View test plan:**
```bash
# macOS/Linux
cat qe-results-*/test-plan.json | head -100

# Windows
type qe-results-*\test-plan.json | more
```

**View defects:**
```bash
# macOS/Linux (pretty-print with jq if installed)
cat qe-results-*/defects.json | jq .

# Windows
type qe-results-*\defects.json
```

**Full execution context:**
```bash
# macOS/Linux
cat qe-results-*/execution-context.json | jq .

# Windows
type qe-results-*\execution-context.json
```

---

## 🔧 Advanced Configuration

### Custom Logging

**Create `src/main/resources/logback.xml`:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>

  <appender name="FILE" class="ch.qos.logback.core.FileAppender">
    <file>qe-agent-system.log</file>
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="CONSOLE" />
    <appender-ref ref="FILE" />
  </root>

  <!-- Reduce noise from specific packages -->
  <logger name="org.apache" level="WARN" />
  <logger name="org.springframework" level="WARN" />
</configuration>
```

Then rebuild:
```bash
mvn clean package
```

### Modify Demo Artifact

**Edit the sample PRD in Main.java:**

```bash
# Open the file
# macOS/Linux
gedit src/main/java/com/qeagent/Main.java

# Windows
code src/main/java/com/qeagent/Main.java  # If Visual Studio Code installed
# Or use Notepad:
notepad src/main/java/com/qeagent/Main.java
```

**Find the `loadSamplePRD()` method and modify the PRD content:**

```java
private static String loadSamplePRD() {
    return """PRODUCT REQUIREMENT DOCUMENT (PRD)
==========================================
Product: YOUR PRODUCT NAME
...
""";
}
```

**Rebuild and run:**
```bash
mvn clean package
java -cp target/qe-agent-system.jar com.qeagent.Main
```

### Increase JVM Memory (for large projects)

```bash
# Allocate 2GB of RAM to JVM
java -Xmx2048m -cp target/qe-agent-system.jar com.qeagent.Main
```

---

## 📱 IDE Setup (Optional)

### IntelliJ IDEA

1. **Open the project:**
   - File → Open → Select `qe-agent-system` directory
   - Click "Trust Project" if prompted

2. **Verify JDK:**
   - File → Project Structure → Project
   - Ensure SDK is set to JDK 17+

3. **Run the demo:**
   - Right-click `Main.java` → Run 'Main.main()'
   - Or use keyboard shortcut: Ctrl+Shift+F10 (Windows/Linux) or Ctrl+Shift+R (macOS)

### Eclipse

1. **Open the project:**
   - File → Import → Existing Maven Projects
   - Browse to `qe-agent-system` directory
   - Click Finish

2. **Verify JDK:**
   - Project → Properties → Java Compiler
   - Ensure Compiler compliance level is 17 or higher

3. **Run the demo:**
   - Right-click `Main.java` → Run As → Java Application

### Visual Studio Code

1. **Install extensions:**
   - Extension Pack for Java
   - Maven for Java

2. **Open the project:**
   - File → Open Folder → Select `qe-agent-system`

3. **Run the demo:**
   - Click the Run button above the `main()` method in Main.java
   - Or use: Ctrl+F5

---

## 🐛 Troubleshooting

### Problem: Build fails with "Could not transfer artifact"

**Solution:**
```bash
# Clear Maven cache
rm -rf ~/.m2/repository  # macOS/Linux
rmdir /s %USERPROFILE%\.m2\repository  # Windows

# Rebuild
mvn clean install -U
```

### Problem: "java: command not found"

**Solution:**
1. Verify Java installation: `which java` (macOS/Linux) or `where java` (Windows)
2. Check JAVA_HOME: `echo $JAVA_HOME` (macOS/Linux) or `echo %JAVA_HOME%` (Windows)
3. If not set, add to PATH:
   - macOS: `export PATH=$(/usr/libexec/java_home -v 17)/bin:$PATH` (add to ~/.zshrc)
   - Linux: `export PATH=/usr/lib/jvm/java-17-openjdk/bin:$PATH` (add to ~/.bashrc)
   - Windows: Add JDK bin directory to System PATH

### Problem: "mvn: command not found"

**Solution:**
1. Verify Maven installation: `which mvn` (macOS/Linux) or `where mvn` (Windows)
2. Check MAVEN_HOME: `echo $MAVEN_HOME` (macOS/Linux) or `echo %MAVEN_HOME%` (Windows)
3. Add Maven to PATH:
   - macOS/Linux: `export PATH=$MAVEN_HOME/bin:$PATH` (add to ~/.zshrc or ~/.bashrc)
   - Windows: Add Maven bin directory to System PATH
4. Restart your laptop

### Problem: "BUILD FAILURE" with compilation errors

**Solution:**
```bash
# Ensure JDK 17 is being used
javac -version

# Force Maven to use JDK 17
export JAVA_HOME=/path/to/jdk-17
mvn clean compile

# Or specify in pom.xml (already configured, but verify)
# Should have: <maven.compiler.target>17</maven.compiler.target>
```

### Problem: Results files not being created

**Solution:**
1. Check if results directory was created:
   ```bash
   ls qe-results-*  # macOS/Linux
   dir qe-results-*  # Windows
   ```

2. Verify write permissions in current directory:
   ```bash
   touch test.txt  # macOS/Linux
   echo test > test.txt  # Windows
   rm test.txt  # macOS/Linux
   del test.txt  # Windows
   ```

3. Run from a different directory:
   ```bash
   mkdir ~/qe-test
   cd ~/qe-test
   java -cp ~/projects/qe-agent-system/target/qe-agent-system.jar com.qeagent.Main
   ```

### Problem: Very slow first build

**Explanation:** Maven is downloading dependencies (~200MB)  
**Solution:** This is normal on first build. Subsequent builds will be much faster.

**Workaround:** If download is timing out:
```bash
# Use a faster Maven repository mirror
mvn -Dorg.slf4j.simpleLogger.defaultLogLevel=debug clean package
```

---

## ✅ Verification Checklist

After deployment, verify everything works:

- [ ] Java 17+ installed: `java -version`
- [ ] Maven 3.8+ installed: `mvn -version`
- [ ] Git installed: `git --version`
- [ ] Repository cloned: `ls qe-agent-system/pom.xml`
- [ ] Build successful: `target/qe-agent-system.jar` exists
- [ ] Demo runs: `java -cp target/qe-agent-system.jar com.qeagent.Main`
- [ ] Results created: `ls qe-results-*/`
- [ ] Results contain data: `cat qe-results-*/test-plan.json` shows content

---

## 📚 Next Steps

### 1. Explore the Code
```bash
# Open in your IDE
code qe-agent-system  # Visual Studio Code
idea qe-agent-system  # IntelliJ IDEA

# Or read the key files
cat src/main/java/com/qeagent/Main.java
cat README.md
cat DESIGN_DOCUMENT.md
```

### 2. Modify & Experiment

**Try changing the sample PRD:**
- Edit `src/main/java/com/qeagent/Main.java`
- Modify `loadSamplePRD()` method
- Rebuild: `mvn clean package`
- Run: `java -cp target/qe-agent-system.jar com.qeagent.Main`

**Adjust test parameters:**
- Edit `TestPlannerAgent.java` to change risk scoring
- Edit `TestExecutorAgent.java` to change timeout (30s)
- Edit `DefectTriageAgent.java` to change confidence calculation

### 3. Integrate with Your Workflow

**Use as a library in your project:**
```bash
# Install JAR to local Maven repository
mvn install:install-file -Dfile=target/qe-agent-system.jar \
  -DgroupId=com.qeagent -DartifactId=qe-agent-system -Dversion=1.0 \
  -Dpackaging=jar
```

Then in your project's `pom.xml`:
```xml
<dependency>
    <groupId>com.qeagent</groupId>
    <artifactId>qe-agent-system</artifactId>
    <version>1.0</version>
</dependency>
```

### 4. Learn the System

**Read in this order:**
1. `README.md` - Overview and usage
2. `DESIGN_DOCUMENT.md` - Architecture and design rationale
3. `src/main/java/com/qeagent/Main.java` - Entry point
4. `src/main/java/com/qeagent/orchestration/QEWorkflowOrchestrator.java` - Workflow
5. Individual agent classes in `src/main/java/com/qeagent/agents/`

---

## 🆘 Getting Help

### Check Logs
```bash
# Maven build logs
mvn clean package -X  # Enable debug mode

# Application logs (if you set up logback.xml)
cat qe-agent-system.log
```

### Review Documentation
- **README.md** - Usage examples and architecture
- **DESIGN_DOCUMENT.md** - Detailed design rationale
- **DELIVERABLES.md** - Summary of all components

### Common Issues

**Q: How do I update the code and rebuild?**
A:
```bash
cd ~/projects/qe-agent-system
git pull  # Get latest changes
mvn clean package  # Rebuild
java -cp target/qe-agent-system.jar com.qeagent.Main  # Run
```

**Q: Can I run this on Windows/Mac/Linux?**
A: Yes! This is 100% cross-platform Java code. Just ensure JDK 17+ and Maven are installed.

**Q: What if I want to run tests with my own PRD?**
A: Edit `Main.java` and modify the `loadSamplePRD()` method, or add a file input parameter.

**Q: How do I integrate this with CI/CD?**
A: This project is ready for Jenkins, GitHub Actions, GitLab CI, etc. Just run `mvn clean package`.

---

## 🎉 You're Ready!

Your local deployment is complete. The system is now running on your laptop and ready for exploration and experimentation.

**Quick reference:**
```bash
# One-line build and run
cd ~/projects/qe-agent-system && mvn clean package && java -cp target/qe-agent-system.jar com.qeagent.Main
```

Happy testing! 🚀
