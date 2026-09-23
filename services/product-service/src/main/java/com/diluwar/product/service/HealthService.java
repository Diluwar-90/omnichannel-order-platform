package com.diluwar.product.service;

import org.springframework.stereotype.Service;

@Service
public class HealthService {
    public String getHealthMessage() {
        return "Product Service is running";
    }
}