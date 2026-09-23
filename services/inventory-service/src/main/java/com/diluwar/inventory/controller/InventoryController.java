package com.diluwar.inventory.controller;

import com.diluwar.inventory.dto.CreateInventoryRequest;
import com.diluwar.inventory.dto.InventoryResponse;
import com.diluwar.inventory.dto.QuantityRequest;
import com.diluwar.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> create(
            @Valid @RequestBody CreateInventoryRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(inventoryService.create(request));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> get(
            @PathVariable Long productId) {

        return ResponseEntity.ok(
                inventoryService.get(productId)
        );
    }

    @PostMapping("/{productId}/reserve")
    public ResponseEntity<InventoryResponse> reserve(
            @PathVariable Long productId,
            @Valid @RequestBody QuantityRequest request) {

        return ResponseEntity.ok(
                inventoryService.reserve(productId, request)
        );
    }

    @PostMapping("/{productId}/release")
    public ResponseEntity<InventoryResponse> release(
            @PathVariable Long productId,
            @Valid @RequestBody QuantityRequest request) {

        return ResponseEntity.ok(
                inventoryService.release(productId, request)
        );
    }

    @PostMapping("/{productId}/reduce")
    public ResponseEntity<InventoryResponse> reduce(
            @PathVariable Long productId,
            @Valid @RequestBody QuantityRequest request) {

        return ResponseEntity.ok(
                inventoryService.reduce(productId, request)
        );
    }

    @GetMapping("/{productId}/availability")
    public ResponseEntity<Map<String, Object>> availability(
            @PathVariable Long productId,
            @RequestParam int quantity) {

        return ResponseEntity.ok(
                Map.of(
                        "productId", productId,
                        "requestedQuantity", quantity,
                        "available",
                        inventoryService.isAvailable(productId, quantity)
                )
        );
    }
}
