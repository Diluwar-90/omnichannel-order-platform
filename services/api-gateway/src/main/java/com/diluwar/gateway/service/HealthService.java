package com.diluwar.gateway.service;

import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public String getHealthMessage() {
        return "API Gateway is running";
    }
}