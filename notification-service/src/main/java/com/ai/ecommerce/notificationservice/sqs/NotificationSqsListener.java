package com.ai.ecommerce.notificationservice.sqs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Lit la file notification-queue, alimentée par deux règles EventBridge :
 *  - order-created-rule  -> OrderCreated
 *  - payment-result-rule -> PaymentCompleted / PaymentFailed
 */
@Component
public class NotificationSqsListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationSqsListener.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @SqsListener("${app.sqs.queue-name:notification-queue}")
    public void onMessage(String message) throws Exception {

        JsonNode envelope = objectMapper.readTree(message);
        String detailType = envelope.path("detail-type").asText();
        String eventId = envelope.path("id").asText();
        JsonNode detail = envelope.path("detail");

        switch (detailType) {

            case "OrderCreated" -> logger.info(
                    "Sending notification [ORDER_RECEIVED] → User: {}, Order: {}, Amount: {} (eventId={})",
                    detail.path("userId").asText(),
                    detail.path("orderId").asText(),
                    detail.path("totalAmount").asDouble(),
                    eventId);

            case "PaymentCompleted" -> logger.info(
                    "Sending notification [PAYMENT_SUCCESS] → User: {}, Order: {}, Amount: {} (eventId={})",
                    detail.path("userId").asText(),
                    detail.path("orderId").asText(),
                    detail.path("amount").asDouble(),
                    eventId);

            case "PaymentFailed" -> logger.warn(
                    "Sending notification [PAYMENT_FAILED] → User: {}, Order: {}, Amount: {}, Reason: {} (eventId={})",
                    detail.path("userId").asText(),
                    detail.path("orderId").asText(),
                    detail.path("amount").asDouble(),
                    detail.path("reason").asText("unknown"),
                    eventId);

            default -> logger.warn("Ignored event on notification-queue: detailType={} eventId={}",
                    detailType, eventId);
        }
    }
}