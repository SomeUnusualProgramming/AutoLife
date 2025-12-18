#!/bin/bash

# Event Type Classification Test Suite Runner
# Runs comprehensive classification tests and generates metrics report

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "=========================================="
echo "Event Type Classification Test Suite"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Run tests
echo "Running EventAnalysisTest..."
echo ""

if mvn test -Dtest=EventAnalysisTest -q; then
    echo -e "${GREEN}✓ Tests completed successfully${NC}"
else
    echo -e "${RED}✗ Tests failed${NC}"
    exit 1
fi

echo ""
echo "=========================================="
echo "Test Results Summary"
echo "=========================================="

# Extract test results
TEST_OUTPUT=$(mvn test -Dtest=EventAnalysisTest 2>&1 | grep -A 50 "EventAnalysisTest" || true)

if echo "$TEST_OUTPUT" | grep -q "Tests run:"; then
    TESTS_RUN=$(echo "$TEST_OUTPUT" | grep "Tests run:" | head -1 | sed 's/.*Tests run: \([0-9]*\).*/\1/')
    FAILURES=$(echo "$TEST_OUTPUT" | grep "Tests run:" | head -1 | sed 's/.*Failures: \([0-9]*\).*/\1/' || echo "0")
    ERRORS=$(echo "$TEST_OUTPUT" | grep "Tests run:" | head -1 | sed 's/.*Errors: \([0-9]*\).*/\1/' || echo "0")
    
    echo "Tests run: $TESTS_RUN"
    echo "Failures: $FAILURES"
    echo "Errors: $ERRORS"
    echo ""
    
    if [ "$FAILURES" = "0" ] && [ "$ERRORS" = "0" ]; then
        echo -e "${GREEN}All tests passed!${NC}"
    else
        echo -e "${RED}Some tests failed!${NC}"
        exit 1
    fi
else
    echo "Could not parse test results"
fi

echo ""
echo "=========================================="
echo "Quality Metrics"
echo "=========================================="
echo ""
echo "Target: >85% accuracy per type"
echo "Target: <10% false clarifications"
echo "Target: Confidence-accuracy correlation > 0.85"
echo ""

echo "Note: Detailed metrics are printed to stdout during test execution"
echo "Look for lines starting with 'Clear Cases', 'Ambiguous Cases', etc."
echo ""

echo "=========================================="
echo "Test Suite Completed"
echo "=========================================="
echo ""
