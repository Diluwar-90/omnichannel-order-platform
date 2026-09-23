package com.diluwar.inventory.service;

import com.diluwar.inventory.dto.CreateInventoryRequest;
import com.diluwar.inventory.dto.InventoryResponse;
import com.diluwar.inventory.dto.QuantityRequest;
import com.diluwar.inventory.entity.Inventory;
import com.diluwar.inventory.exception.InsufficientStockException;
import com.diluwar.inventory.exception.InventoryAlreadyExistsException;
import com.diluwar.inventory.exception.InventoryNotFoundException;
import com.diluwar.inventory.repository.InventoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public InventoryResponse create(CreateInventoryRequest request) {

        if (inventoryRepository.existsByProductId(request.productId())) {
            throw new InventoryAlreadyExistsException(request.productId());
        }

        Inventory inventory = new Inventory();
        inventory.setProductId(request.productId());
        inventory.setQuantity(request.quantity());
        inventory.setReservedQuantity(0);

        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse get(Long productId) {
        return toResponse(findInventory(productId));
    }

    @Transactional
    public InventoryResponse reserve(Long productId, QuantityRequest request) {

        Inventory inventory = findInventory(productId);

        int available = inventory.getAvailableQuantity();

        if (available < request.quantity()) {
            throw new InsufficientStockException(
                    productId,
                    request.quantity(),
                    available
            );
        }

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() + request.quantity()
        );

        return toResponse(inventoryRepository.save(inventory));
    }
    
@Transactional
public InventoryResponse release(Long productId, QuantityRequest request) {

    Inventory inventory = findInventory(productId);

    int currentReserved = inventory.getReservedQuantity();
    int requestedRelease = request.quantity();

    if (requestedRelease > currentReserved) {
        throw new InsufficientStockException(
                productId,
                requestedRelease,
                currentReserved
        );
    }

    inventory.setReservedQuantity(
            currentReserved - requestedRelease
    );

    return toResponse(inventoryRepository.save(inventory));
}

    @Transactional
    public InventoryResponse reduce(Long productId, QuantityRequest request) {

        Inventory inventory = findInventory(productId);

        int available = inventory.getAvailableQuantity();

        if (available < request.quantity()) {
            throw new InsufficientStockException(
                    productId,
                    request.quantity(),
                    available
            );
        }

        inventory.setQuantity(
                inventory.getQuantity() - request.quantity()
        );

        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public boolean isAvailable(Long productId, int quantity) {

        Inventory inventory = findInventory(productId);

        return inventory.getAvailableQuantity() >= quantity;
    }

    private Inventory findInventory(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
    }

    private InventoryResponse toResponse(Inventory inventory) {

        return new InventoryResponse(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getAvailableQuantity(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }
}
