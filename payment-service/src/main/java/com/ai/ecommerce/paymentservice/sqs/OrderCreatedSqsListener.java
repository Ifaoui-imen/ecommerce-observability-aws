package com.ai.ecommerce.paymentservice.sqs;

import com.ai.ecommerce.paymentservice.Model.OrderEvent;
import com.ai.ecommerce.paymentservice.Model.PaymentEvent;
import com.ai.ecommerce.paymentservice.events.PaymentEventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Lit payment-queue (OrderCreated), simule le paiement,
 * puis publie PaymentCompleted ou PaymentFailed sur EventBridge.
 */
@Component
public class OrderCreatedSqsListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderCreatedSqsListener.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaymentEventPublisher publisher;
    private final double maxAmount;     // au-delà : refus systématique (échec prévisible)
    private final double failureRate;   // 0.0 à 1.0 : refus aléatoire (injection de pannes)

    public OrderCreatedSqsListener(PaymentEventPublisher publisher,
                                   @Value("${app.payment.max-amount:10000}") double maxAmount,
                                   @Value("${app.payment.failure-rate:0.0}") double failureRate) {
        this.publisher = publisher;
        this.maxAmount = maxAmount;
        this.failureRate = failureRate;
    }

    @SqsListener("${app.sqs.queue-name:payment-queue}")
    public void onMessage(String message) throws Exception {

        JsonNode envelope = objectMapper.readTree(message);
        String detailType = envelope.path("detail-type").asText();
        String eventId = envelope.path("id").asText();

        if (!"OrderCreated".equals(detailType)) {
            logger.warn("Ignored event on payment-queue: detailType={} eventId={}", detailType, eventId);
            return;
        }

        OrderEvent order = objectMapper.treeToValue(envelope.path("detail"), OrderEvent.class);
        double amount = order.getTotalAmount() != null ? order.getTotalAmount() : 0.0;
        logger.info("OrderCreated received from SQS: orderId={} userId={} totalAmount={} eventId={}",
                order.getOrderId(), order.getUserId(), amount, eventId);

        PaymentEvent payment = new PaymentEvent();
        payment.setPaymentId(UUID.randomUUID().toString());
        payment.setOrderId(order.getOrderId());
        payment.setUserId(order.getUserId());
        payment.setAmount(amount);

        // Simulation du prestataire de paiement
        if (amount > maxAmount) {
            payment.setStatus("FAILED");
            payment.setReason("amount exceeds limit (" + maxAmount + ")");
        } else if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            payment.setStatus("FAILED");
            payment.setReason("payment provider declined (simulated, failure-rate=" + failureRate + ")");
        } else {
            payment.setStatus("SUCCESS");
        }

        publisher.publish(payment);
    }
}