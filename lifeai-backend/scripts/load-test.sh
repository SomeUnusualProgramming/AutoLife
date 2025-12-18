#!/bin/bash

CONCURRENT=${1:-50}
DURATION=${2:-5m}
API_URL=${3:-"http://localhost:8080/api/events"}

RESULTS_DIR="load-test-results"
mkdir -p $RESULTS_DIR

echo "=========================================="
echo "Load Testing: Event Classification API"
echo "=========================================="
echo "Concurrent Users: $CONCURRENT"
echo "Duration: $DURATION"
echo "Target URL: $API_URL"
echo ""

RESULTS_FILE="$RESULTS_DIR/load-test-$(date +%Y%m%d-%H%M%S).txt"

TEST_PAYLOADS=(
    '{"userId":1,"textInput":"Zjadłem śniadanie z jajkami"}'
    '{"userId":1,"textInput":"Trenowałem przez godzinę na siłowni"}'
    '{"userId":1,"textInput":"Mam ból głowy i gorąckę"}'
    '{"userId":1,"textInput":"Byłem u lekarza dzisiaj"}'
    '{"userId":1,"textInput":"Ważę 85 kilogramów"}'
    '{"userId":1,"textInput":"Spaliśmy osiem godzin"}'
    '{"userId":1,"textInput":"Czuję się smutny"}'
)

echo "Starting load test..."
echo ""

TOTAL_REQUESTS=0
SUCCESSFUL_REQUESTS=0
FAILED_REQUESTS=0
TOTAL_RESPONSE_TIME=0
MIN_RESPONSE_TIME=10000
MAX_RESPONSE_TIME=0
RESPONSE_TIMES=()

START_TIMESTAMP=$(date +%s)

for ((i = 1; i <= CONCURRENT; i++)); do
    (
        PAYLOAD=${TEST_PAYLOADS[$((i % ${#TEST_PAYLOADS[@]}))]}
        
        while [ $(($(date +%s) - START_TIMESTAMP)) -lt 300 ]; do
            REQ_START=$(date +%s%3N)
            
            RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
                "$API_URL" \
                -H "Content-Type: application/json" \
                -d "$PAYLOAD" 2>/dev/null)
            
            HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
            BODY=$(echo "$RESPONSE" | head -n-1)
            
            REQ_END=$(date +%s%3N)
            RESPONSE_TIME=$((REQ_END - REQ_START))
            
            echo "$i,$HTTP_CODE,$RESPONSE_TIME" >> "$RESULTS_FILE"
            
            sleep 0.1
        done
    ) &
done

wait

echo ""
echo "=========================================="
echo "Load Test Complete"
echo "=========================================="
echo ""

SUCCESSFUL=$(grep ",200," "$RESULTS_FILE" | wc -l)
TOTAL=$(wc -l < "$RESULTS_FILE")
FAILED=$((TOTAL - SUCCESSFUL))

AVG_TIME=$(awk -F',' '{sum+=$3; count++} END {print int(sum/count)}' "$RESULTS_FILE")
MIN_TIME=$(awk -F',' '{if (NR==1 || $3 < min) min=$3} END {print min}' "$RESULTS_FILE")
MAX_TIME=$(awk -F',' '{if (NR==1 || $3 > max) max=$3} END {print max}' "$RESULTS_FILE")

echo "Total Requests: $TOTAL"
echo "Successful: $SUCCESSFUL ($(( SUCCESSFUL * 100 / TOTAL ))%)"
echo "Failed: $FAILED"
echo ""
echo "Response Times:"
echo "  Min: ${MIN_TIME}ms"
echo "  Max: ${MAX_TIME}ms"
echo "  Average: ${AVG_TIME}ms"
echo ""

P95=$(awk -F',' '{print $3}' "$RESULTS_FILE" | sort -n | awk '{if(NR==int(NR*0.95)) print}')
P99=$(awk -F',' '{print $3}' "$RESULTS_FILE" | sort -n | awk '{if(NR==int(NR*0.99)) print}')

echo "Percentiles:"
echo "  P95: ${P95}ms"
echo "  P99: ${P99}ms"
echo ""

echo "Results saved to: $RESULTS_FILE"
