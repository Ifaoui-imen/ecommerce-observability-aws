package ai.ecommerce.orderservice.events;

import java.util.List;

/** Contenu du champ "detail" de l'événement OrderCreated (voir docs/events.md) */
public class OrderCreatedEvent {

    private Long orderId;
    private Long userId;
    private Double totalAmount;
    private List<Item> items;

    public OrderCreatedEvent() {}

    public OrderCreatedEvent(Long orderId, Long userId, Double totalAmount, List<Item> items) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.items = items;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public static class Item {
        private Long productId;
        private Integer quantity;
        private Double price;

        public Item() {}

        public Item(Long productId, Integer quantity, Double price) {
            this.productId = productId;
            this.quantity = quantity;
            this.price = price;
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
    }
}