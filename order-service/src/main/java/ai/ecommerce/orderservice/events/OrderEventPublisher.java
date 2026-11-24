package ai.ecommerce.orderservice.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

/** Publie les événements d'order-service sur le bus EventBridge ecommerce-bus */
@Component
public class OrderEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventPublisher.class);

    private static final String SOURCE = "ecommerce.order";
    private static final String DETAIL_TYPE_ORDER_CREATED = "OrderCreated";

    private final EventBridgeClient eventBridge;
    private final ObjectMapper objectMapper;
    private final String busName;

    public OrderEventPublisher(EventBridgeClient eventBridge,
                               ObjectMapper objectMapper,
                               @Value("${app.aws.eventbridge.bus-name:ecommerce-bus}") String busName) {
        this.eventBridge = eventBridge;
        this.objectMapper = objectMapper;
        this.busName = busName;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            String detail = objectMapper.writeValueAsString(event);

            PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                    .eventBusName(busName)
                    .source(SOURCE)
                    .detailType(DETAIL_TYPE_ORDER_CREATED)
                    .detail(detail)
                    .build();

            PutEventsResponse response = eventBridge.putEvents(
                    PutEventsRequest.builder().entries(entry).build());

            // PutEvents ne lève pas d'exception si une entrée est refusée : il faut vérifier
            if (response.failedEntryCount() != null && response.failedEntryCount() > 0) {
                logger.error("OrderCreated rejected by EventBridge: orderId={} error={} {}",
                        event.getOrderId(),
                        response.entries().get(0).errorCode(),
                        response.entries().get(0).errorMessage());
            } else {
                logger.info("OrderCreated published to EventBridge: orderId={} eventId={} detail={}",
                        event.getOrderId(), response.entries().get(0).eventId(), detail);
            }
        } catch (Exception e) {
            logger.error("Error publishing OrderCreated to EventBridge: orderId={} error={}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }
}