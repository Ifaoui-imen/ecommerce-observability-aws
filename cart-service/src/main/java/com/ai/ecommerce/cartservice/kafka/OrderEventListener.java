package com.ai.ecommerce.cartservice.kafka;

import com.ai.ecommerce.cartservice.repository.CartRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventListener.class);

    private final CartRepository cartRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderEventListener(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @KafkaListener(topics = "order-created", groupId = "cart-service-group")
    public void consumeOrderEvent(String message) {
        logger.info("Received order event: {}", message);

        String userId = extractUserId(message);
        if (userId == null) {
            logger.warn("No userId found in order event, cart not cleared");
            return;
        }

        cartRepository.deleteCart(userId);   // même clé que l'enregistrement : "CART_" + userId
        logger.info("Cart cleared for user: {}", userId);
    }

    // Message attendu : {"orderId":1,"userId":1,"totalAmount":2400.0}
    private String extractUserId(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            JsonNode userIdNode = json.get("userId");
            if (userIdNode == null || userIdNode.isNull()) {
                return null;
            }
            return userIdNode.asText();   // fonctionne si userId est un nombre ou un texte
        } catch (Exception e) {
            logger.error("Failed to parse order event: {}", e.getMessage());
            return null;
        }
    }
}