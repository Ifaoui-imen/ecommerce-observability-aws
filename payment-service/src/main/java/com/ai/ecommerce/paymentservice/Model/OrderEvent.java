package com.ai.ecommerce.paymentservice.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)   // ignore les champs inconnus au lieu de planter
public class OrderEvent {
    private Long orderId;
    private String userId;
    private Double totalAmount;                // champ envoyé par order-service
    private List<OrderItem> orderItems;

    public OrderEvent() {}

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderItem {
        private String productId;
        private int quantity;

        public OrderItem() {}
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}