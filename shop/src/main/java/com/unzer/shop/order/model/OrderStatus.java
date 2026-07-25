package com.unzer.shop.order.model;

public enum OrderStatus {
    CREATED,
    AWAITING_PAYMENT,
    PAID,
    FULFILLING,
    SHIPPED,
    COMPLETED,
    CANCELLED,
    PAYMENT_FAILED,
    REFUNDED
}
