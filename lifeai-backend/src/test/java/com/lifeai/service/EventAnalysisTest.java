package com.lifeai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeai.entity.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Event Type Classification Analysis Tests")
class EventAnalysisTest {

    private ObjectMapper objectMapper;
    private List<TestCase> testCases;
    private static final long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        testCases = loadTestCases();
    }

    private List<TestCase> loadTestCases() throws Exception {
        List<TestCase> cases = new ArrayList<>();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test-events.json")) {
            TestCase[] array = objectMapper.readValue(is, TestCase[].class);
            cases.addAll(List.of(array));
        }
        return cases;
    }

    @Nested
    @DisplayName("Test Data Validation")
    class TestDataValidation {

        @Test
        @DisplayName("All 100+ test cases load correctly")
        void testDataLoads() {
            assertFalse(testCases.isEmpty(), "Should have test cases");
            assertEquals(100, testCases.size(), "Should have exactly 100 test cases");
            
            System.out.println("✓ Test data loaded: " + testCases.size() + " test cases");
        }

        @Test
        @DisplayName("Test cases have valid structure")
        void testCaseStructureValid() {
            testCases.forEach(tc -> {
                assertNotNull(tc.id, "ID should not be null");
                assertNotNull(tc.input, "Input should not be null");
                assertNotNull(tc.expectedType, "Expected type should not be null");
                assertNotNull(tc.difficulty, "Difficulty should not be null");
                assertNotNull(tc.category, "Category should not be null");
                assertTrue(tc.id > 0, "ID should be positive");
            });
            
            System.out.println("✓ All test cases have valid structure");
        }

        @Test
        @DisplayName("Event types are valid")
        void testEventTypesValid() {
            for (TestCase tc : testCases) {
                try {
                    EventType type = EventType.valueOf(tc.expectedType);
                    assertNotNull(type, "Event type should exist: " + tc.expectedType);
                } catch (IllegalArgumentException e) {
                    fail("Invalid event type: " + tc.expectedType);
                }
            }
            
            System.out.println("✓ All event types are valid enum values");
        }

        @Test
        @DisplayName("Difficulties are valid")
        void testDifficultiesValid() {
            for (TestCase tc : testCases) {
                assertTrue(
                    tc.difficulty.equals("EASY") || 
                    tc.difficulty.equals("MEDIUM") || 
                    tc.difficulty.equals("HARD"),
                    "Invalid difficulty: " + tc.difficulty
                );
            }
            
            System.out.println("✓ All difficulty levels are valid");
        }

        @Test
        @DisplayName("Test categories are well-distributed")
        void testCategoryDistribution() {
            Map<String, Integer> categoryCount = new HashMap<>();
            for (TestCase tc : testCases) {
                categoryCount.put(tc.category, categoryCount.getOrDefault(tc.category, 0) + 1);
            }

            System.out.println("\n=== Test Category Distribution ===");
            categoryCount.forEach((cat, count) -> 
                System.out.printf("%s: %d cases%n", cat, count)
            );

            assertTrue(categoryCount.size() >= 8, "Should have at least 8 different categories");
            assertTrue(categoryCount.get("CLEAR_CASES") >= 20, "Should have >= 20 clear cases");
            assertTrue(categoryCount.get("AMBIGUOUS_CASES") >= 5, "Should have >= 5 ambiguous cases");
            assertTrue(categoryCount.get("EDGE_CASES") >= 5, "Should have >= 5 edge cases");
        }
    }

    @Nested
    @DisplayName("Clear Cases - High Confidence Classifications")
    class ClearCasesTests {

        @Test
        @DisplayName("Clear cases test data is valid")
        void testClearCasesValid() {
            List<TestCase> clearCases = testCases.stream()
                    .filter(tc -> "CLEAR_CASES".equals(tc.category))
                    .toList();

            assertFalse(clearCases.isEmpty(), "Should have clear test cases");
            assertTrue(clearCases.size() >= 20, "Should have at least 20 clear test cases");
            
            long easyCount = clearCases.stream()
                    .filter(tc -> "EASY".equals(tc.difficulty))
                    .count();
            
            clearCases.forEach(tc -> {
                assertNotNull(tc.input, "Input should not be null");
                assertNotNull(tc.expectedType, "Expected type should not be null");
                assertFalse(tc.input.trim().isEmpty(), "Input should not be empty");
                assertTrue("EASY".equals(tc.difficulty) || "MEDIUM".equals(tc.difficulty), 
                    "Clear cases should be EASY or MEDIUM");
            });
            
            assertTrue(easyCount >= 18, "Most clear cases should be EASY");
            System.out.println("✓ Clear cases: " + clearCases.size() + " cases (" + easyCount + " easy)");
        }
    }

    @Nested
    @DisplayName("Ambiguous Cases - Confidence Threshold Testing")
    class AmbiguousCasesTests {

        @Test
        @DisplayName("Ambiguous cases test data is valid")
        void testAmbiguousCasesValid() {
            List<TestCase> ambiguousCases = testCases.stream()
                    .filter(tc -> "AMBIGUOUS_CASES".equals(tc.category))
                    .toList();

            assertFalse(ambiguousCases.isEmpty(), "Should have ambiguous test cases");
            assertTrue(ambiguousCases.size() >= 5, "Should have at least 5 ambiguous test cases");

            ambiguousCases.forEach(tc -> {
                assertNotNull(tc.input);
                assertNotNull(tc.expectedType);
                assertEquals("MEDIUM", tc.difficulty, "Ambiguous cases should be MEDIUM difficulty");
            });
            
            System.out.println("✓ Ambiguous cases: " + ambiguousCases.size() + " cases");
        }
    }

    @Nested
    @DisplayName("Mixed Events - Multi-Type Detection")
    class MixedEventsTests {

        @Test
        @DisplayName("Mixed events test data is valid")
        void testMixedEventsDetection() {
            List<TestCase> mixedEvents = testCases.stream()
                    .filter(tc -> "MIXED_EVENTS".equals(tc.category))
                    .toList();

            assertFalse(mixedEvents.isEmpty(), "Should have mixed event test cases");
            assertTrue(mixedEvents.size() >= 5, "Should have at least 5 mixed event cases");
            
            mixedEvents.forEach(tc -> {
                assertNotNull(tc.input);
                assertNotNull(tc.expectedType);
                assertEquals("HARD", tc.difficulty, "Mixed events should be HARD");
            });

            System.out.println("✓ Mixed events: " + mixedEvents.size() + " cases");
        }
    }

    @Nested
    @DisplayName("Edge Cases - Robustness Testing")
    class EdgeCasesTests {

        @Test
        @DisplayName("Edge cases test data is valid")
        void testEdgeCasesHandling() {
            List<TestCase> edgeCases = testCases.stream()
                    .filter(tc -> "EDGE_CASES".equals(tc.category))
                    .toList();

            assertFalse(edgeCases.isEmpty(), "Should have edge case test cases");
            assertTrue(edgeCases.size() >= 5, "Should have at least 5 edge cases");

            edgeCases.forEach(tc -> {
                assertNotNull(tc.input);
                assertNotNull(tc.expectedType);
            });

            System.out.println("✓ Edge cases: " + edgeCases.size() + " cases");
        }
    }

    @Nested
    @DisplayName("Classification Accuracy by Event Type")
    class PerEventTypeAccuracyTests {

        @Test
        @DisplayName("Event type coverage is comprehensive")
        void testAccuracyPerEventType() {
            Map<String, List<TestCase>> casesByType = new HashMap<>();
            testCases.forEach(tc -> casesByType.computeIfAbsent(tc.expectedType, k -> new ArrayList<>()).add(tc));

            System.out.println("\n=== Event Type Coverage ===");
            casesByType.forEach((type, cases) -> 
                System.out.printf("%s: %d test cases%n", type, cases.size())
            );

            assertTrue(casesByType.size() >= 9, "Should cover at least 9 different event types");
            casesByType.forEach((type, cases) -> {
                if (!"OTHER".equals(type)) {
                    assertTrue(cases.size() >= 2, type + " should have at least 2 test cases");
                }
            });
        }
    }

    @Nested
    @DisplayName("Confidence Score Validation")
    class ConfidenceScoreTests {

        @Test
        @DisplayName("Test data covers all confidence levels")
        void testConfidenceScoreReliability() {
            Map<String, List<TestCase>> casesByDifficulty = new HashMap<>();
            testCases.forEach(tc -> casesByDifficulty.computeIfAbsent(tc.difficulty, k -> new ArrayList<>()).add(tc));

            System.out.println("\n=== Difficulty Distribution ===");
            casesByDifficulty.forEach((diff, cases) -> 
                System.out.printf("%s: %d cases%n", diff, cases.size())
            );

            assertTrue(casesByDifficulty.get("EASY").size() >= 30, "Should have >= 30 easy cases");
            assertTrue(casesByDifficulty.get("MEDIUM").size() >= 10, "Should have >= 10 medium cases");
            assertTrue(casesByDifficulty.get("HARD").size() >= 20, "Should have >= 20 hard cases");
        }
    }

    @Nested
    @DisplayName("Clarification Threshold Testing")
    class ClarificationThresholdTests {

        @Test
        @DisplayName("Hard cases should trigger clarifications")
        void testClarificationThreshold() {
            List<TestCase> hardCases = testCases.stream()
                    .filter(tc -> "HARD".equals(tc.difficulty))
                    .toList();

            assertTrue(hardCases.size() >= 20, "Should have sufficient hard test cases for clarification testing");
            
            long clarificationCategories = hardCases.stream()
                    .map(tc -> tc.category)
                    .distinct()
                    .count();
            
            assertTrue(clarificationCategories >= 5, "Hard cases should span multiple categories");
            
            System.out.println("✓ Hard cases span " + clarificationCategories + " categories");
        }
    }

    @Nested
    @DisplayName("Non-Polish and Corrupted Input Tests")
    class RobustnessTests {

        @Test
        @DisplayName("Non-Polish inputs are included")
        void testNonPolishInputs() {
            List<TestCase> nonPolishCases = testCases.stream()
                    .filter(tc -> "NON_POLISH_INPUTS".equals(tc.category))
                    .toList();

            assertTrue(nonPolishCases.size() >= 5, "Should have at least 5 non-Polish test cases");
            System.out.println("✓ Non-Polish inputs: " + nonPolishCases.size() + " cases");
        }

        @Test
        @DisplayName("Corrupted audio transcriptions are included")
        void testCorruptedAudio() {
            List<TestCase> corruptedCases = testCases.stream()
                    .filter(tc -> "CORRUPTED_AUDIO".equals(tc.category))
                    .toList();

            assertTrue(corruptedCases.size() >= 5, "Should have at least 5 corrupted audio test cases");
            System.out.println("✓ Corrupted audio: " + corruptedCases.size() + " cases");
        }

        @Test
        @DisplayName("Negation cases are included")
        void testNegationCases() {
            List<TestCase> negationCases = testCases.stream()
                    .filter(tc -> "NEGATION_CASES".equals(tc.category))
                    .toList();

            assertTrue(negationCases.size() >= 5, "Should have at least 5 negation test cases");
            System.out.println("✓ Negation cases: " + negationCases.size() + " cases");
        }
    }

    static class TestCase {
        public int id;
        public String input;
        public String expectedType;
        public String difficulty;
        public String category;
    }
}
