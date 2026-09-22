package com.diluwar.auth.service;

import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public String getHealthMessage() {
        return "Auth Service is running";
    }
}