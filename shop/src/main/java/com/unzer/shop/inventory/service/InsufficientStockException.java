package com.unzer.shop.inventory.service;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(UUID variantId, int requested, int available) {
        super(String.format("Insufficient stock for variant %s: requested %d, available %d",
                variantId, requested, available));
    }
}
