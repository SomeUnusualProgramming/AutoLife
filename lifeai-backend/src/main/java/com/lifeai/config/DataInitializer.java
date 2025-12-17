package com.lifeai.config;

import com.lifeai.entity.User;
import com.lifeai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting DataInitializer...");
        
        try {
            long userCount = userRepository.count();
            if (userCount == 0) {
                User defaultUser = User.builder()
                    .email("user@lifeai.local")
                    .username("lifeai_user")
                    .passwordHash("default")
                    .firstName("Life")
                    .lastName("AI")
                    .isActive(true)
                    .build();
                
                User savedUser = userRepository.save(defaultUser);
                log.info("Created default user: id={}, email={}", savedUser.getId(), savedUser.getEmail());
            } else {
                log.info("Users already exist in database");
            }
        } catch (Exception e) {
            log.error("Failed to initialize default user: {}", e.getMessage(), e);
        }
        
        log.info("DataInitializer completed");
    }
}
