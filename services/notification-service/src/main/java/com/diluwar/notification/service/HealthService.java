package com.diluwar.notification.service;

import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public String getHealthMessage() {
        return "Notification Service is running";
    }
}