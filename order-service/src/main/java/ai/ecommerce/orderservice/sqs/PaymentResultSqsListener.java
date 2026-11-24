package ai.ecommerce.orderservice.sqs;

import ai.ecommerce.orderservice.entity.Order;
import ai.ecommerce.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lit order-queue (règle payment-result-rule) et met à jour le statut de la commande :
 *  - PaymentCompleted -> PAID
 *  - PaymentFailed    -> PAYMENT_FAILED (+ raison)
 */
@Component
public class PaymentResultSqsListener {

    private static final Logger logger = LoggerFactory.getLogger(PaymentResultSqsListener.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OrderRepository orderRepository;

    public PaymentResultSqsListener(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @SqsListener("${app.sqs.queue-name:order-queue}")
    @Transactional
    public void onMessage(String message) throws Exception {

        JsonNode envelope = objectMapper.readTree(message);
        String detailType = envelope.path("detail-type").asText();
        String eventId = envelope.path("id").asText();
        JsonNode detail = envelope.path("detail");

        String newStatus;
        switch (detailType) {
            case "PaymentCompleted" -> newStatus = Order.STATUS_PAID;
            case "PaymentFailed"    -> newStatus = Order.STATUS_PAYMENT_FAILED;
            default -> {
                logger.warn("Ignored event on order-queue: detailType={} eventId={}", detailType, eventId);
                return;
            }
        }

        long orderId = detail.path("orderId").asLong();

        // Commande inconnue : exception -> 3 essais puis order-dlq (incident visible)
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "Order " + orderId + " not found for " + detailType + " (eventId=" + eventId + ")"));

        String oldStatus = order.getStatus();
        order.setStatus(newStatus);
        if (Order.STATUS_PAYMENT_FAILED.equals(newStatus)) {
            order.setPaymentFailureReason(detail.path("reason").asText(null));
        }
        orderRepository.save(order);

        logger.info("Order status updated: orderId={} {} -> {} (paymentId={} eventId={})",
                orderId, oldStatus, newStatus, detail.path("paymentId").asText(), eventId);
    }
}