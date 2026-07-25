package com.unzer.shop.inventory.service;

import java.util.UUID;

public class ConcurrentReservationException extends RuntimeException {
    public ConcurrentReservationException(UUID variantId) {
        super("Could not reserve stock for variant " + variantId + " after retries — high concurrency");
    }
}
