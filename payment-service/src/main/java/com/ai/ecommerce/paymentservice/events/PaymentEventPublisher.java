package com.ai.ecommerce.paymentservice.events;

import com.ai.ecommerce.paymentservice.Model.PaymentEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

/** Publie PaymentCompleted / PaymentFailed sur le bus EventBridge ecommerce-bus */
@Component
public class PaymentEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventPublisher.class);
    private static final String SOURCE = "ecommerce.payment";

    private final EventBridgeClient eventBridge;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String busName;

    public PaymentEventPublisher(EventBridgeClient eventBridge,
                                 @Value("${app.aws.eventbridge.bus-name:ecommerce-bus}") String busName) {
        this.eventBridge = eventBridge;
        this.busName = busName;
    }

    public void publish(PaymentEvent payment) {
        String detailType = "SUCCESS".equals(payment.getStatus()) ? "PaymentCompleted" : "PaymentFailed";
        try {
            String detail = objectMapper.writeValueAsString(payment);

            PutEventsResponse response = eventBridge.putEvents(PutEventsRequest.builder()
                    .entries(PutEventsRequestEntry.builder()
                            .eventBusName(busName)
                            .source(SOURCE)
                            .detailType(detailType)
                            .detail(detail)
                            .build())
                    .build());

            if (response.failedEntryCount() != null && response.failedEntryCount() > 0) {
                logger.error("{} rejected by EventBridge: orderId={} error={} {}", detailType,
                        payment.getOrderId(),
                        response.entries().get(0).errorCode(),
                        response.entries().get(0).errorMessage());
            } else {
                logger.info("{} published to EventBridge: orderId={} eventId={} detail={}", detailType,
                        payment.getOrderId(), response.entries().get(0).eventId(), detail);
            }
        } catch (Exception e) {
            logger.error("Error publishing {} to EventBridge: orderId={} error={}",
                    detailType, payment.getOrderId(), e.getMessage(), e);
        }
    }
}