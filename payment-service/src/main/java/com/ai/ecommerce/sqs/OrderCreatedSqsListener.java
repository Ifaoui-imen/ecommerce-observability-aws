package com.ai.ecommerce.paymentservice.sqs;

import com.ai.ecommerce.paymentservice.Model.OrderEvent;
import com.ai.ecommerce.paymentservice.Model.PaymentEvent;
import com.ai.ecommerce.paymentservice.kafka.PaymentEventProducer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Lit la file payment-queue, alimentée par la règle EventBridge order-created-rule.
 * Le message SQS contient l'enveloppe EventBridge : les données utiles sont dans "detail".
 */
@Component
public class OrderCreatedSqsListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderCreatedSqsListener.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaymentEventProducer producer;

    public OrderCreatedSqsListener(PaymentEventProducer producer) {
        this.producer = producer;
    }

    @SqsListener("${app.sqs.queue-name:payment-queue}")
    public void onMessage(String message) throws Exception {

        // 1. Lire l'enveloppe EventBridge
        JsonNode envelope = objectMapper.readTree(message);
        String detailType = envelope.path("detail-type").asText();
        String eventId = envelope.path("id").asText();

        if (!"OrderCreated".equals(detailType)) {
            logger.warn("Ignored event on payment-queue: detailType={} eventId={}", detailType, eventId);
            return;   // message supprimé de la file : il ne nous concerne pas
        }

        // 2. Extraire "detail" -> OrderEvent
        OrderEvent order = objectMapper.treeToValue(envelope.path("detail"), OrderEvent.class);
        logger.info("OrderCreated received from SQS: orderId={} userId={} totalAmount={} eventId={}",
                order.getOrderId(), order.getUserId(), order.getTotalAmount(), eventId);

        // 3. Traiter le paiement (même logique qu'avant, toujours simulé)
        PaymentEvent payment = new PaymentEvent();
        payment.setPaymentId(UUID.randomUUID().toString());
        payment.setOrderId(order.getOrderId());
        payment.setUserId(order.getUserId());
        payment.setStatus("SUCCESS");
        payment.setAmount(order.getTotalAmount() != null ? order.getTotalAmount() : 0.0);

        // 4. Publier le résultat (encore sur Kafka jusqu'à l'étape E)
        producer.sendPaymentEvent(payment);
    }
}