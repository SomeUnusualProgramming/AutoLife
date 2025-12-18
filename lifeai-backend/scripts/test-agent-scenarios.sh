#!/bin/bash

# AI Agent Test Scenarios Script
# Tests the clarification loop and event processing

set -e

API_URL="http://localhost:8080/api"
USER_ID="1"

echo "============================================"
echo "LifeAI AI Agent Test Scenarios"
echo "============================================"
echo ""

# Test 1: Complete event - should save immediately
echo "TEST 1: Complete Event (Direct Save)"
echo "Sending: 'I ran 30 minutes at moderate intensity'"
echo ""

RESPONSE_1=$(curl -s -X POST "${API_URL}/speech-to-text/process" \
  -H "Content-Type: application/json" \
  -d "{
    \"inputType\": \"TEXT\",
    \"textInput\": \"I ran 30 minutes at moderate intensity\",
    \"userId\": ${USER_ID}
  }")

echo "Response:"
echo "$RESPONSE_1" | jq '.' 2>/dev/null || echo "$RESPONSE_1"
echo ""

STATUS_1=$(echo "$RESPONSE_1" | jq -r '.status' 2>/dev/null || echo "ERROR")
if [ "$STATUS_1" == "COMPLETE" ]; then
  echo "✓ TEST 1 PASSED: Event created with status COMPLETE"
else
  echo "✗ TEST 1 FAILED: Expected COMPLETE, got $STATUS_1"
fi
echo ""
echo "-------------------------------------------"
echo ""

# Test 2: Incomplete event - should ask for clarification
echo "TEST 2: Incomplete Event (Clarification Loop)"
echo "Sending: 'I took my medication'"
echo ""

RESPONSE_2=$(curl -s -X POST "${API_URL}/speech-to-text/process" \
  -H "Content-Type: application/json" \
  -d "{
    \"inputType\": \"TEXT\",
    \"textInput\": \"I took my medication\",
    \"userId\": ${USER_ID}
  }")

echo "Response:"
echo "$RESPONSE_2" | jq '.' 2>/dev/null || echo "$RESPONSE_2"
echo ""

STATUS_2=$(echo "$RESPONSE_2" | jq -r '.status' 2>/dev/null || echo "ERROR")
SESSION_ID=$(echo "$RESPONSE_2" | jq -r '.sessionId' 2>/dev/null || echo "")

if [ "$STATUS_2" == "PENDING_CLARIFICATION" ]; then
  echo "✓ TEST 2 PASSED: Event marked as PENDING_CLARIFICATION"
  echo "Session ID: $SESSION_ID"
else
  echo "✗ TEST 2 FAILED: Expected PENDING_CLARIFICATION, got $STATUS_2"
  echo "Skipping Test 3 (no session)"
  exit 1
fi
echo ""
echo "-------------------------------------------"
echo ""

# Test 3: Clarification response - provide missing data
echo "TEST 3: Clarification Response (Round 1)"
echo "Sending: '5mg of metformin at 8am'"
echo ""

RESPONSE_3=$(curl -s -X POST "${API_URL}/clarification/${SESSION_ID}/respond" \
  -H "Content-Type: application/json" \
  -d "{
    \"sessionId\": \"${SESSION_ID}\",
    \"userResponse\": \"5mg of metformin at 8am\"
  }")

echo "Response:"
echo "$RESPONSE_3" | jq '.' 2>/dev/null || echo "$RESPONSE_3"
echo ""

STATUS_3=$(echo "$RESPONSE_3" | jq -r '.status' 2>/dev/null || echo "ERROR")
if [ "$STATUS_3" == "COMPLETE" ] || [ "$STATUS_3" == "PENDING_CLARIFICATION" ]; then
  echo "✓ TEST 3 PASSED: Clarification processed with status $STATUS_3"
else
  echo "✗ TEST 3 FAILED: Expected COMPLETE or PENDING_CLARIFICATION, got $STATUS_3"
fi
echo ""
echo "-------------------------------------------"
echo ""

# Test 4: Calendar-aware event
echo "TEST 4: Calendar Date Parsing"
echo "Sending: 'I have a doctor appointment tomorrow at 2pm'"
echo ""

RESPONSE_4=$(curl -s -X POST "${API_URL}/speech-to-text/process" \
  -H "Content-Type: application/json" \
  -d "{
    \"inputType\": \"TEXT\",
    \"textInput\": \"I have a doctor appointment tomorrow at 2pm\",
    \"userId\": ${USER_ID}
  }")

echo "Response:"
echo "$RESPONSE_4" | jq '.' 2>/dev/null || echo "$RESPONSE_4"
echo ""

STATUS_4=$(echo "$RESPONSE_4" | jq -r '.status' 2>/dev/null || echo "ERROR")
if [ "$STATUS_4" == "COMPLETE" ]; then
  echo "✓ TEST 4 PASSED: Calendar event created with status COMPLETE"
else
  echo "✗ TEST 4 FAILED: Expected COMPLETE, got $STATUS_4"
fi
echo ""
echo "-------------------------------------------"
echo ""

echo "============================================"
echo "Test Suite Completed"
echo "============================================"
echo ""
echo "Summary:"
echo "- TEST 1 (Direct Save): Complete event processing"
echo "- TEST 2 (Clarification): Incomplete event detection"
echo "- TEST 3 (Response): Clarification round processing"
echo "- TEST 4 (Calendar): Date parsing for future events"
echo ""
