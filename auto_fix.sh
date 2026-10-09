#!/bin/bash
set -e

cd /data/data/com.termux/files/home/minec

echo "=== Auto Fix Loop Started ==="
echo "Timestamp: $(date)"

# Function to check latest run
check_latest_run() {
  gh run list --limit 1 --json databaseId,status,conclusion,displayTitle,url 2>/dev/null
}

# Function to get failed logs
get_failed_logs() {
  local run_id=$1
  gh run view "$run_id" --log-failed > /tmp/failed_logs.txt 2>&1
  echo "Logs saved to /tmp/failed_logs.txt"
}

# Main loop
MAX_ITERATIONS=20
for i in $(seq 1 $MAX_ITERATIONS); do
  echo ""
  echo "=== Iteration $i/$MAX_ITERATIONS ==="
  
  # Check latest run
  LATEST=$(gh run list --limit 1 --json databaseId,status,conclusion 2>/dev/null)
  RUN_ID=$(echo "$LATEST" | jq -r '.[0].databaseId' 2>/dev/null || echo "")
  STATUS=$(echo "$LATEST" | jq -r '.[0].status' 2>/dev/null || echo "")
  CONCLUSION=$(echo "$LATEST" | jq -r '.[0].conclusion' 2>/dev/null || echo "")
  
  echo "Run ID: $RUN_ID | Status: $STATUS | Conclusion: $CONCLUSION"
  
  if [ "$STATUS" = "in_progress" ]; then
    echo "Build in progress, waiting..."
    sleep 120
    continue
  fi
  
  if [ "$CONCLUSION" = "success" ]; then
    echo "✅ Build succeeded! All done."
    exit 0
  fi
  
  if [ "$CONCLUSION" = "failure" ] && [ -n "$RUN_ID" ]; then
    echo "❌ Build failed. Analyzing and fixing..."
    get_failed_logs "$RUN_ID"
    
    # Create prompt for opencode
    cat > /tmp/fix_prompt.txt << 'PROMPT'
You are fixing compilation errors in mcJava (Minecraft Java port to Android).

Task: Fix the compilation errors shown in the failed logs.

Steps:
1. Read /tmp/failed_logs.txt to understand the errors
2. Identify the root cause - focus on the earliest errors
3. Fix the code in android/app/src/main/java
4. Make minimal, targeted fixes
5. Commit and push with a descriptive message

Use gh CLI if needed to understand context. Fix errors until they are resolved.
PROMPT
    
    opencode --model opencode/big-pickle -p "$(cat /tmp/fix_prompt.txt)" --yes
    echo "Fixes committed and pushed. Waiting for next build..."
    sleep 60
    continue
  fi
  
  echo "No recent build or unknown state. Waiting..."
  sleep 60
done

echo "Max iterations reached"
