package com.lifeai;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Integration test - requires full context")
class LifeAiBackendApplicationTests {

    @Test
    void contextLoads() {
    }
}
