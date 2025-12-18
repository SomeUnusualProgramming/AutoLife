#!/bin/bash

MODELS=("mistral:7b" "neural-chat:7b" "dolphin-mixtral:latest")
TEST_PROMPTS=(
    "Zjadłem śniadanie z jajkami"
    "Trenowałem przez godzinę"
    "Mam ból głowy"
    "Byłem u lekarza"
    "Ważę 85 kilogramów"
)

RESULTS_DIR="benchmark-results"
mkdir -p $RESULTS_DIR

echo "=========================================="
echo "Ollama Model Performance Benchmark"
echo "=========================================="
echo ""

for model in "${MODELS[@]}"; do
    echo "Testing model: $model"
    echo ""
    
    RESULT_FILE="$RESULTS_DIR/${model//\//_}-results.txt"
    > "$RESULT_FILE"
    
    TOTAL_TIME=0
    SUCCESS_COUNT=0
    ERROR_COUNT=0
    RESPONSE_TIMES=()
    
    for prompt in "${TEST_PROMPTS[@]}"; do
        echo "  Testing prompt: $prompt"
        
        START=$(date +%s%3N)
        
        RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
            http://localhost:11434/api/generate \
            -H "Content-Type: application/json" \
            -d "{\"model\":\"$model\",\"prompt\":\"$prompt\",\"stream\":false}" 2>/dev/null)
        
        HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
        BODY=$(echo "$RESPONSE" | head -n-1)
        
        END=$(date +%s%3N)
        ELAPSED=$((END - START))
        
        echo "    Response time: ${ELAPSED}ms, HTTP: $HTTP_CODE" | tee -a "$RESULT_FILE"
        RESPONSE_TIMES+=($ELAPSED)
        TOTAL_TIME=$((TOTAL_TIME + ELAPSED))
        
        if [ "$HTTP_CODE" == "200" ]; then
            ((SUCCESS_COUNT++))
        else
            ((ERROR_COUNT++))
        fi
    done
    
    AVG_TIME=$((TOTAL_TIME / ${#TEST_PROMPTS[@]}))
    
    echo ""
    echo "Model: $model" >> "$RESULTS_DIR/summary.txt"
    echo "  Total requests: ${#TEST_PROMPTS[@]}" >> "$RESULTS_DIR/summary.txt"
    echo "  Successful: $SUCCESS_COUNT" >> "$RESULTS_DIR/summary.txt"
    echo "  Failed: $ERROR_COUNT" >> "$RESULTS_DIR/summary.txt"
    echo "  Average response time: ${AVG_TIME}ms" >> "$RESULTS_DIR/summary.txt"
    echo "  Total time: ${TOTAL_TIME}ms" >> "$RESULTS_DIR/summary.txt"
    echo "" >> "$RESULTS_DIR/summary.txt"
    
    echo "Results saved to: $RESULT_FILE"
    echo ""
done

echo "=========================================="
echo "Benchmark Complete"
echo "=========================================="
echo ""
echo "Summary Results:"
cat "$RESULTS_DIR/summary.txt"
