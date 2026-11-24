# Contrat des événements – bus ecommerce-bus

| source | detail-type | Producteur | Files destinataires |
|---|---|---|---|
| ecommerce.order | OrderCreated | order-service | payment, inventory, notification, cart |
| ecommerce.payment | PaymentCompleted | payment-service | order, inventory, notification |
| ecommerce.payment | PaymentFailed | payment-service | order, inventory, notification |

## OrderCreated – detail
{"orderId": 4, "userId": 1, "totalAmount": 2400.0,
 "items": [{"productId": 1, "quantity": 2, "price": 1200.0}]}

## PaymentCompleted – detail
{"paymentId": "uuid", "orderId": 4, "userId": 1, "amount": 2400.0, "status": "SUCCESS"}

## PaymentFailed – detail
{"paymentId": "uuid", "orderId": 4, "userId": 1, "amount": 2400.0, "status": "FAILED", "reason": "..."}