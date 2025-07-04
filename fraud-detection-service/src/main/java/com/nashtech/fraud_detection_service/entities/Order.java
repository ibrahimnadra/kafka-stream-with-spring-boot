package com.nashtech.fraud_detection_service.entities;

public record Order(
        String orderId,
        String userId,
        double amount
) {
    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", amount=" + amount +
                '}';
    }
}