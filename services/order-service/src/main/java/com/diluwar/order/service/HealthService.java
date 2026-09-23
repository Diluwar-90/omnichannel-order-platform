package com.diluwar.order.service;

import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public String getHealthMessage() {
        return "Order Service is running";
    }
}