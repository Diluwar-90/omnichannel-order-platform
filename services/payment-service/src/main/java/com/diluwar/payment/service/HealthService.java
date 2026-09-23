package com.diluwar.payment.service;

import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public String getHealthMessage() {
        return "Payment Service is running";
    }
}