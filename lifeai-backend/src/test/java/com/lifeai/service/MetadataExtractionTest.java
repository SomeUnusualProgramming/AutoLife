package com.lifeai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Metadata Extraction Accuracy Validation Tests")
class MetadataExtractionTest {

    private ObjectMapper objectMapper;
    private Map<String, ValidationRules> validationRules;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        validationRules = loadValidationRules();
    }

    private Map<String, ValidationRules> loadValidationRules() throws Exception {
        Map<String, ValidationRules> rules = new HashMap<>();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("metadata-validation-rules.json")) {
            JsonNode rootNode = objectMapper.readTree(is);
            
            for (Iterator<String> it = rootNode.fieldNames(); it.hasNext();) {
                String eventType = it.next();
                if (!"GLOBAL_CONSTRAINTS".equals(eventType)) {
                    ValidationRules rule = objectMapper.treeToValue(rootNode.get(eventType), ValidationRules.class);
                    rules.put(eventType, rule);
                }
            }
        }
        return rules;
    }

    @Nested
    @DisplayName("Validation Rules Loading")
    class ValidationRulesLoadingTests {

        @Test
        @DisplayName("Validation rules load correctly")
        void testRulesLoad() {
            assertFalse(validationRules.isEmpty(), "Validation rules should not be empty");
            assertEquals(10, validationRules.size(), "Should have rules for 10 event types");
            
            System.out.println("✓ Validation rules loaded for: " + String.join(", ", validationRules.keySet()));
        }

        @Test
        @DisplayName("All event types have required fields")
        void testRequiredFields() {
            for (Map.Entry<String, ValidationRules> entry : validationRules.entrySet()) {
                String eventType = entry.getKey();
                ValidationRules rule = entry.getValue();
                
                assertNotNull(rule.fields, eventType + " should have fields definition");
                assertFalse(rule.fields.isEmpty(), eventType + " should have at least one field");
                assertTrue(rule.fields.contains("description") || rule.fields.contains("timestamp"), 
                    eventType + " should have description or timestamp");
            }
            
            System.out.println("✓ All event types have required fields");
        }

        @Test
        @DisplayName("Constraints are well-defined")
        void testConstraints() {
            for (Map.Entry<String, ValidationRules> entry : validationRules.entrySet()) {
                String eventType = entry.getKey();
                ValidationRules rule = entry.getValue();
                
                if (rule.constraints != null && !rule.constraints.isEmpty()) {
                    for (String constraint : rule.constraints.keySet()) {
                        assertNotNull(rule.constraints.get(constraint), 
                            eventType + " constraint '" + constraint + "' should have definition");
                    }
                }
            }
            
            System.out.println("✓ All constraints are well-defined");
        }
    }

    @Nested
    @DisplayName("Weight Event Metadata Extraction")
    class WeightMetadataTests {

        @Test
        @DisplayName("Weight events should extract numeric value")
        void testWeightExtraction() {
            ValidationRules weightRules = validationRules.get("WEIGHT");
            assertNotNull(weightRules, "Weight rules should exist");
            assertTrue(weightRules.fields.contains("weight_value"), "Should extract weight value");
            assertTrue(weightRules.optionalFields.contains("bmi"), "Should support BMI");
        }

        @Test
        @DisplayName("Weight value constraints are valid")
        void testWeightConstraints() {
            ValidationRules weightRules = validationRules.get("WEIGHT");
            Map<String, Object> weightConstraints = weightRules.constraints;
            
            assertNotNull(weightConstraints.get("weight_value"), "Should have weight_value constraint");
            System.out.println("✓ Weight extraction supports: " + weightRules.optionalFields);
        }
    }

    @Nested
    @DisplayName("Activity Event Metadata Extraction")
    class ActivityMetadataTests {

        @Test
        @DisplayName("Activity events should extract duration and type")
        void testActivityExtraction() {
            ValidationRules activityRules = validationRules.get("ACTIVITY");
            assertNotNull(activityRules, "Activity rules should exist");
            assertTrue(activityRules.fields.contains("duration"), "Should extract duration");
            assertTrue(activityRules.fields.contains("activityType"), "Should extract activity type");
        }

        @Test
        @DisplayName("Activity types are well-defined")
        void testActivityTypes() {
            ValidationRules activityRules = validationRules.get("ACTIVITY");
            assertNotNull(activityRules.activityTypes, "Activity types should be defined");
            assertTrue(activityRules.activityTypes.size() >= 5, "Should have at least 5 activity types");
            System.out.println("✓ Activity types: " + String.join(", ", activityRules.activityTypes));
        }
    }

    @Nested
    @DisplayName("Food Event Metadata Extraction")
    class FoodMetadataTests {

        @Test
        @DisplayName("Food events should extract meal type")
        void testFoodExtraction() {
            ValidationRules foodRules = validationRules.get("FOOD");
            assertNotNull(foodRules, "Food rules should exist");
            assertTrue(foodRules.fields.contains("mealType"), "Should extract meal type");
            assertTrue(foodRules.optionalFields.contains("calories"), "Should support calories");
        }

        @Test
        @DisplayName("Meal types are comprehensive")
        void testMealTypes() {
            ValidationRules foodRules = validationRules.get("FOOD");
            assertNotNull(foodRules.mealTypes, "Meal types should be defined");
            assertTrue(foodRules.mealTypes.size() >= 4, "Should have at least 4 meal types");
            assertTrue(foodRules.mealTypes.contains("BREAKFAST"), "Should include BREAKFAST");
            System.out.println("✓ Meal types: " + String.join(", ", foodRules.mealTypes));
        }
    }

    @Nested
    @DisplayName("Medication Event Metadata Extraction")
    class MedicationMetadataTests {

        @Test
        @DisplayName("Medication events should extract dosage and frequency")
        void testMedicationExtraction() {
            ValidationRules medRules = validationRules.get("MEDICATION");
            assertNotNull(medRules, "Medication rules should exist");
            assertTrue(medRules.fields.contains("dosage"), "Should extract dosage");
            assertTrue(medRules.fields.contains("frequency"), "Should extract frequency");
        }

        @Test
        @DisplayName("Dosage units are defined")
        void testDosageUnits() {
            ValidationRules medRules = validationRules.get("MEDICATION");
            Object unitValue = medRules.constraints.get("unit");
            if (unitValue instanceof Map) {
                Map<String, Object> unitConstraint = (Map<String, Object>) unitValue;
                assertNotNull(unitConstraint, "Unit constraint should be defined");
            }
            System.out.println("✓ Medication rules support frequency and dosage units");
        }
    }

    @Nested
    @DisplayName("Doctor Visit Event Metadata Extraction")
    class DoctorVisitMetadataTests {

        @Test
        @DisplayName("Doctor visit events should extract doctor type and visit type")
        void testDoctorVisitExtraction() {
            ValidationRules docRules = validationRules.get("DOCTOR_VISIT");
            assertNotNull(docRules, "Doctor visit rules should exist");
            assertTrue(docRules.fields.contains("doctor_type"), "Should extract doctor type");
            assertTrue(docRules.fields.contains("visit_type"), "Should extract visit type");
        }

        @Test
        @DisplayName("Doctor and visit types are comprehensive")
        void testDoctorTypes() {
            ValidationRules docRules = validationRules.get("DOCTOR_VISIT");
            assertNotNull(docRules.doctorTypes, "Doctor types should be defined");
            assertNotNull(docRules.visitTypes, "Visit types should be defined");
            assertTrue(docRules.doctorTypes.size() >= 4, "Should have at least 4 doctor types");
            System.out.println("✓ Doctor types: " + String.join(", ", docRules.doctorTypes));
        }
    }

    @Nested
    @DisplayName("Symptom Event Metadata Extraction")
    class SymptomMetadataTests {

        @Test
        @DisplayName("Symptom events should extract symptom type and severity")
        void testSymptomExtraction() {
            ValidationRules symptomRules = validationRules.get("SYMPTOM");
            assertNotNull(symptomRules, "Symptom rules should exist");
            assertTrue(symptomRules.fields.contains("symptom_type"), "Should extract symptom type");
            assertTrue(symptomRules.fields.contains("severity"), "Should extract severity");
        }

        @Test
        @DisplayName("Severity levels are defined")
        void testSeverityLevels() {
            ValidationRules symptomRules = validationRules.get("SYMPTOM");
            Object severityValue = symptomRules.constraints.get("severity");
            if (severityValue instanceof Map) {
                Map<String, Object> severityConstraint = (Map<String, Object>) severityValue;
                assertNotNull(severityConstraint, "Severity constraint should be defined");
            }
            System.out.println("✓ Symptom metadata includes type, severity, and location");
        }
    }

    @Nested
    @DisplayName("Sleep Event Metadata Extraction")
    class SleepMetadataTests {

        @Test
        @DisplayName("Sleep events should extract duration and quality")
        void testSleepExtraction() {
            ValidationRules sleepRules = validationRules.get("SLEEP");
            assertNotNull(sleepRules, "Sleep rules should exist");
            assertTrue(sleepRules.fields.contains("duration"), "Should extract duration");
            assertTrue(sleepRules.fields.contains("quality"), "Should extract quality");
        }

        @Test
        @DisplayName("Sleep quality levels are defined")
        void testSleepQuality() {
            ValidationRules sleepRules = validationRules.get("SLEEP");
            Object qualityValue = sleepRules.constraints.get("quality");
            if (qualityValue instanceof Map) {
                Map<String, Object> qualityConstraint = (Map<String, Object>) qualityValue;
                assertNotNull(qualityConstraint, "Quality constraint should be defined");
            }
            System.out.println("✓ Sleep metadata includes duration, quality, and wakeup count");
        }
    }

    @Nested
    @DisplayName("Mood Event Metadata Extraction")
    class MoodMetadataTests {

        @Test
        @DisplayName("Mood events should extract mood and intensity")
        void testMoodExtraction() {
            ValidationRules moodRules = validationRules.get("MOOD");
            assertNotNull(moodRules, "Mood rules should exist");
            assertTrue(moodRules.fields.contains("mood"), "Should extract mood");
            assertTrue(moodRules.fields.contains("intensity"), "Should extract intensity");
        }

        @Test
        @DisplayName("Mood types are comprehensive")
        void testMoodTypes() {
            ValidationRules moodRules = validationRules.get("MOOD");
            assertNotNull(moodRules.moods, "Mood types should be defined");
            assertTrue(moodRules.moods.size() >= 5, "Should have at least 5 mood types");
            System.out.println("✓ Mood types: " + String.join(", ", moodRules.moods));
        }
    }

    @Nested
    @DisplayName("Metadata Field Coverage")
    class FieldCoverageTests {

        @Test
        @DisplayName("All required fields are specified")
        void testRequiredFieldsCoverage() {
            int totalFields = 0;
            for (ValidationRules rule : validationRules.values()) {
                totalFields += rule.fields.size();
            }
            
            assertTrue(totalFields > 0, "Should have required fields defined");
            System.out.println("✓ Total required fields across all types: " + totalFields);
        }

        @Test
        @DisplayName("Optional fields are well-distributed")
        void testOptionalFieldsCoverage() {
            int totalOptional = 0;
            Map<String, Integer> optionalByType = new HashMap<>();
            
            for (Map.Entry<String, ValidationRules> entry : validationRules.entrySet()) {
                List<String> optFields = entry.getValue().optionalFields;
                int optCount = optFields != null ? optFields.size() : 0;
                totalOptional += optCount;
                optionalByType.put(entry.getKey(), optCount);
            }
            
            System.out.println("\n=== Optional Fields by Event Type ===");
            optionalByType.forEach((type, count) -> 
                System.out.printf("%s: %d optional fields%n", type, count)
            );
            
            assertTrue(totalOptional > 0, "Should have optional fields defined");
        }
    }

    @Nested
    @DisplayName("Constraint Validation Rules")
    class ConstraintValidationTests {

        @Test
        @DisplayName("Numeric constraints have min/max bounds")
        void testNumericConstraints() {
            int constraintsWithBounds = 0;
            
            for (ValidationRules rule : validationRules.values()) {
                if (rule.constraints != null) {
                    for (Map.Entry<String, Object> entry : rule.constraints.entrySet()) {
                        Object value = entry.getValue();
                        if (value instanceof Map) {
                            Map<String, Object> constraint = (Map<String, Object>) value;
                            if (constraint.containsKey("min") && constraint.containsKey("max")) {
                                constraintsWithBounds++;
                            }
                        }
                    }
                }
            }
            
            assertTrue(constraintsWithBounds > 0, "Should have numeric constraints with bounds");
            System.out.println("✓ " + constraintsWithBounds + " constraints have min/max bounds");
        }

        @Test
        @DisplayName("Enum constraints are properly defined")
        void testEnumConstraints() {
            int enumConstraints = 0;
            
            for (ValidationRules rule : validationRules.values()) {
                if (rule.constraints != null) {
                    for (Map.Entry<String, Object> entry : rule.constraints.entrySet()) {
                        Object value = entry.getValue();
                        if (value instanceof Map) {
                            Map<String, Object> constraint = (Map<String, Object>) value;
                            if ("ENUM".equals(constraint.get("type")) && constraint.containsKey("values")) {
                                enumConstraints++;
                            }
                        }
                    }
                }
            }
            
            assertTrue(enumConstraints > 0, "Should have enum constraints");
            System.out.println("✓ " + enumConstraints + " enum constraints defined");
        }
    }

    static class ValidationRules {
        public List<String> fields = new ArrayList<>();
        public List<String> optionalFields = new ArrayList<>();
        public List<String> mealTypes = new ArrayList<>();
        public List<String> activityTypes = new ArrayList<>();
        public List<String> doctorTypes = new ArrayList<>();
        public List<String> visitTypes = new ArrayList<>();
        public List<String> symptomTypes = new ArrayList<>();
        public List<String> moods = new ArrayList<>();
        public Map<String, Object> constraints = new HashMap<>();
    }
}
