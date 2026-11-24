package ai.ecommerce.orderservice.service;

import ai.ecommerce.orderservice.dto.OrderItemRequest;
import ai.ecommerce.orderservice.dto.OrderRequest;
import ai.ecommerce.orderservice.entity.Order;
import ai.ecommerce.orderservice.entity.OrderItem;
import ai.ecommerce.orderservice.events.OrderCreatedEvent;
import ai.ecommerce.orderservice.events.OrderEventPublisher;
import ai.ecommerce.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;    // EventBridge

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Order placeOrder(OrderRequest orderRequest) {

        // Create Order
        Order order = new Order();
        order.setUserId(orderRequest.getUserId());
        order.setStatus(Order.STATUS_CREATED);

        List<OrderItem> orderItems = new ArrayList<>();
        double total = 0.0;

        for (OrderItemRequest itemReq : orderRequest.getItems()) {
            OrderItem item = new OrderItem();
            item.setProductId(itemReq.getProductId());
            item.setQuantity(itemReq.getQuantity());
            item.setPrice(itemReq.getPrice());
            item.setOrder(order);

            total += itemReq.getPrice() * itemReq.getQuantity();
            orderItems.add(item);
        }

        order.setItems(orderItems);
        order.setTotalAmount(total);

        // Save Order
        Order savedOrder = orderRepository.save(order);

        // Publish OrderCreated on EventBridge
        List<OrderCreatedEvent.Item> eventItems = new ArrayList<>();
        for (OrderItem item : savedOrder.getItems()) {
            eventItems.add(new OrderCreatedEvent.Item(
                    item.getProductId(), item.getQuantity(), item.getPrice()));
        }
        eventPublisher.publishOrderCreated(new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getTotalAmount(),
                eventItems
        ));

        return savedOrder;
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + id));
    }

    @Override
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }
}