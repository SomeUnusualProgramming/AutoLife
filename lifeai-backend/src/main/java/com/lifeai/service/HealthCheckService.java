package com.lifeai.service;

import com.lifeai.dto.HealthCheckResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthCheckService {

    public HealthCheckResponse getHealthStatus() {
        return new HealthCheckResponse(
            "UP",
            "Application is running",
            System.currentTimeMillis()
        );
    }
}
