package com.ai.ecommerce.inventoryservice.sqs;

import com.ai.ecommerce.inventoryservice.entity.InventoryItem;
import com.ai.ecommerce.inventoryservice.entity.StockReservation;
import com.ai.ecommerce.inventoryservice.repository.InventoryRepository;
import com.ai.ecommerce.inventoryservice.repository.StockReservationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Lit inventory-queue (deux règles EventBridge) :
 *  - OrderCreated     -> réserve le stock
 *  - PaymentCompleted -> confirme la réservation
 *  - PaymentFailed    -> libère la réservation et remet le stock (compensation, pattern Saga)
 */
@Component
public class InventorySqsListener {

    private static final Logger logger = LoggerFactory.getLogger(InventorySqsListener.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;

    public InventorySqsListener(InventoryRepository inventoryRepository,
                                StockReservationRepository reservationRepository) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
    }

    @SqsListener("${app.sqs.queue-name:inventory-queue}")
    @Transactional
    public void onMessage(String message) throws Exception {

        JsonNode envelope = objectMapper.readTree(message);
        String detailType = envelope.path("detail-type").asText();
        String eventId = envelope.path("id").asText();
        JsonNode detail = envelope.path("detail");
        long orderId = detail.path("orderId").asLong();

        switch (detailType) {
            case "OrderCreated"     -> reserveStock(orderId, detail.path("items"), eventId);
            case "PaymentCompleted" -> confirmReservation(orderId, eventId);
            case "PaymentFailed"    -> releaseReservation(orderId, eventId);
            default -> logger.warn("Ignored event on inventory-queue: detailType={} eventId={}",
                    detailType, eventId);
        }
    }

    /** OrderCreated : décrémente le stock et mémorise la réservation */
    private void reserveStock(long orderId, JsonNode items, String eventId) {

        // Idempotence : SQS peut livrer le même message deux fois
        if (reservationRepository.existsByOrderId(orderId)) {
            logger.warn("Duplicate OrderCreated ignored: orderId={} eventId={}", orderId, eventId);
            return;
        }

        for (JsonNode item : items) {
            long productId = item.path("productId").asLong();
            int requested = item.path("quantity").asInt();

            Optional<InventoryItem> stockOpt = inventoryRepository.findByProductId(productId);
            if (stockOpt.isEmpty()) {
                logger.warn("STOCK_UNKNOWN_PRODUCT: orderId={} productId={} eventId={}",
                        orderId, productId, eventId);
                continue;
            }

            InventoryItem stock = stockOpt.get();
            int available = stock.getQuantity() != null ? stock.getQuantity() : 0;
            if (available < requested) {
                logger.warn("INSUFFICIENT_STOCK: orderId={} productId={} available={} requested={} eventId={}",
                        orderId, productId, available, requested, eventId);
                continue;
            }

            stock.setQuantity(available - requested);
            stock.setLastUpdated(LocalDateTime.now());
            inventoryRepository.save(stock);
            reservationRepository.save(new StockReservation(orderId, productId, requested));

            logger.info("Stock reserved: orderId={} productId={} quantity={} remaining={} eventId={}",
                    orderId, productId, requested, stock.getQuantity(), eventId);
        }
    }

    /** PaymentCompleted : la réservation devient définitive */
    private void confirmReservation(long orderId, String eventId) {
        List<StockReservation> reservations =
                reservationRepository.findByOrderIdAndStatus(orderId, StockReservation.RESERVED);

        for (StockReservation r : reservations) {
            r.setStatus(StockReservation.CONFIRMED);
            reservationRepository.save(r);
        }
        logger.info("Stock reservation confirmed: orderId={} lines={} eventId={}",
                orderId, reservations.size(), eventId);
    }

    /** PaymentFailed : compensation -> on remet le stock */
    private void releaseReservation(long orderId, String eventId) {
        List<StockReservation> reservations =
                reservationRepository.findByOrderIdAndStatus(orderId, StockReservation.RESERVED);

        for (StockReservation r : reservations) {
            inventoryRepository.findByProductId(r.getProductId()).ifPresent(stock -> {
                stock.setQuantity(stock.getQuantity() + r.getQuantity());
                stock.setLastUpdated(LocalDateTime.now());
                inventoryRepository.save(stock);
                logger.info("Stock released: orderId={} productId={} quantity={} newStock={} eventId={}",
                        orderId, r.getProductId(), r.getQuantity(), stock.getQuantity(), eventId);
            });
            r.setStatus(StockReservation.RELEASED);
            reservationRepository.save(r);
        }

        if (reservations.isEmpty()) {
            logger.info("Nothing to release for orderId={} eventId={}", orderId, eventId);
        }
    }
}