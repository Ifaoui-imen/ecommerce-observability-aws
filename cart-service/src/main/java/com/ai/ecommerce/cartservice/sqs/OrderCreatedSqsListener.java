package com.ai.ecommerce.cartservice.sqs;

import com.ai.ecommerce.cartservice.repository.CartRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Lit la file cart-queue, alimentée par la règle EventBridge order-created-rule.
 * À chaque commande créée, on vide le panier de l'utilisateur.
 */
@Component
public class OrderCreatedSqsListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderCreatedSqsListener.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CartRepository cartRepository;

    public OrderCreatedSqsListener(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @SqsListener("${app.sqs.queue-name:cart-queue}")
    public void onMessage(String message) throws Exception {

        // 1. Enveloppe EventBridge
        JsonNode envelope = objectMapper.readTree(message);
        String detailType = envelope.path("detail-type").asText();
        String eventId = envelope.path("id").asText();

        if (!"OrderCreated".equals(detailType)) {
            logger.warn("Ignored event on cart-queue: detailType={} eventId={}", detailType, eventId);
            return;
        }

        // 2. Lire userId et orderId dans "detail"
        JsonNode detail = envelope.path("detail");
        JsonNode userIdNode = detail.path("userId");
        long orderId = detail.path("orderId").asLong();

        if (userIdNode.isMissingNode() || userIdNode.isNull()) {
            // Événement invalide : on lève une exception -> 3 essais puis cart-dlq
            throw new IllegalArgumentException("OrderCreated without userId, eventId=" + eventId);
        }
        String userId = userIdNode.asText();

        // 3. Vider le panier (même clé Redis que l'enregistrement : "CART_" + userId)
        cartRepository.deleteCart(userId);
        logger.info("Cart cleared for user: {} (orderId={} eventId={})", userId, orderId, eventId);
    }
}