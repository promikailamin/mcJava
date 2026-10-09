#!/bin/bash
cd /data/data/com.termux/files/home/minec

while true; do
  # Check latest workflow run
  LATEST=$(gh run list --limit 1 --json databaseId,status,conclusion,workflowName,headSha 2>/dev/null)
  RUN_ID=$(echo "$LATEST" | jq -r '.[0].databaseId // empty')
  STATUS=$(echo "$LATEST" | jq -r '.[0].status // empty')
  CONCLUSION=$(echo "$LATEST" | jq -r '.[0].conclusion // empty')
  
  echo "$(date): Run=$RUN_ID Status=$STATUS Conclusion=$CONCLUSION"
  
  if [ "$STATUS" = "completed" ] && [ "$CONCLUSION" = "failure" ]; then
    echo "Build failed. Triggering opencode fix..."
    gh run view "$RUN_ID" --log-failed > /tmp/failed_logs.txt 2>&1
    
    opencode --model opencode/big-pickle -p "
      Fix mcJava Android build errors.
      
      1. Read /tmp/failed_logs.txt for compilation errors
      2. Find and fix the errors in android/app/src/main/java
      3. Commit with message 'fix: auto-fix build errors'
      4. Push to main
    " --yes
  elif [ "$STATUS" = "completed" ] && [ "$CONCLUSION" = "success" ]; then
    echo "Build successful!"
    break
  fi
  
  sleep 60
done
