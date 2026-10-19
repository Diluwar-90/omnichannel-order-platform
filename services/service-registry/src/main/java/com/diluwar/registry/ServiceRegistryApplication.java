package com.diluwar.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Enterprise Service Registry powered by Netflix Eureka Server.
 * <p>
 * Architectural Responsibilities:
 * - Dynamic Service Registration: Maintains real-time registry of active microservice instances.
 * - Heartbeat & Liveness Tracking: Automatically evicts stale/unhealthy nodes to guarantee routing integrity.
 * - Discovery Provider for Spring Cloud LoadBalancer: Supplies instance metadata to client-side
 *   (OpenFeign) and edge gateway reverse proxies.
 */
@SpringBootApplication
@EnableEurekaServer
public class ServiceRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceRegistryApplication.class, args);
    }
}
