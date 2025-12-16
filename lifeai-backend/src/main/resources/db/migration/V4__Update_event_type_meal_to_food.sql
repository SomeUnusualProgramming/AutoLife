ALTER TABLE events ADD CONSTRAINT check_valid_event_type 
    CHECK (event_type IN ('FOOD', 'ACTIVITY', 'DOCTOR_VISIT', 'MEDICATION', 'SYMPTOM', 'WEIGHT', 'SLEEP', 'MOOD', 'MEDICAL', 'OTHER'));
