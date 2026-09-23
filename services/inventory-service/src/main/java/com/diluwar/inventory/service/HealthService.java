package com.diluwar.inventory.service;

import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public String getHealthMessage() {
        return "Inventory Service is running";
    }
}