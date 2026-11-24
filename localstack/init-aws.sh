#!/bin/bash
# Exécuté automatiquement par LocalStack quand il est prêt (dossier /etc/localstack/init/ready.d)
# Crée : le bus ecommerce-bus, 5 files SQS + 5 DLQ, 2 règles EventBridge
set -euo pipefail

REGION=us-east-1
ACCOUNT=000000000000          # compte fictif de LocalStack
BUS=ecommerce-bus

echo ">>> Création du bus EventBridge $BUS"
awslocal events create-event-bus --name "$BUS"

for SVC in order payment inventory notification cart; do
  Q="${SVC}-queue"
  DLQ="${SVC}-dlq"
  echo ">>> File $Q (+ DLQ $DLQ)"

  # 1. La DLQ (reçoit les messages qui échouent 3 fois)
  awslocal sqs create-queue --queue-name "$DLQ"
  DLQ_ARN="arn:aws:sqs:${REGION}:${ACCOUNT}:${DLQ}"

  # 2. La file principale, reliée à sa DLQ
  awslocal sqs create-queue --queue-name "$Q" --attributes "{
    \"VisibilityTimeout\": \"30\",
    \"RedrivePolicy\": \"{\\\"deadLetterTargetArn\\\":\\\"${DLQ_ARN}\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"
  }"
done

echo ">>> Règle OrderCreated -> payment, inventory, notification, cart"
awslocal events put-rule --name order-created-rule --event-bus-name "$BUS" \
  --event-pattern '{"source":["ecommerce.order"],"detail-type":["OrderCreated"]}'
awslocal events put-targets --rule order-created-rule --event-bus-name "$BUS" --targets \
  "Id"="payment","Arn"="arn:aws:sqs:${REGION}:${ACCOUNT}:payment-queue" \
  "Id"="inventory","Arn"="arn:aws:sqs:${REGION}:${ACCOUNT}:inventory-queue" \
  "Id"="notification","Arn"="arn:aws:sqs:${REGION}:${ACCOUNT}:notification-queue" \
  "Id"="cart","Arn"="arn:aws:sqs:${REGION}:${ACCOUNT}:cart-queue"

echo ">>> Règle PaymentCompleted/Failed -> order, inventory, notification"
awslocal events put-rule --name payment-result-rule --event-bus-name "$BUS" \
  --event-pattern '{"source":["ecommerce.payment"],"detail-type":["PaymentCompleted","PaymentFailed"]}'
awslocal events put-targets --rule payment-result-rule --event-bus-name "$BUS" --targets \
  "Id"="order","Arn"="arn:aws:sqs:${REGION}:${ACCOUNT}:order-queue" \
  "Id"="inventory","Arn"="arn:aws:sqs:${REGION}:${ACCOUNT}:inventory-queue" \
  "Id"="notification","Arn"="arn:aws:sqs:${REGION}:${ACCOUNT}:notification-queue"

echo ">>> Infrastructure événementielle prête"