package com.ai.ecommerce.inventoryservice.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Mémorise ce qui a été réservé pour une commande, pour pouvoir le libérer si le paiement échoue */
@Entity
@Table(name = "stock_reservations")
public class StockReservation {

    public static final String RESERVED = "RESERVED";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String RELEASED = "RELEASED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private Long productId;
    private Integer quantity;

    @Column(length = 20)
    private String status;

    private LocalDateTime updatedAt;

    public StockReservation() {}

    public StockReservation(Long orderId, Long productId, Integer quantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = RESERVED;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getProductId() { return productId; }
    public Integer getQuantity() { return quantity; }
    public String getStatus() { return status; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
}