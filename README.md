# Payment Orchestration System

A Spring Boot 3.3 Payment Orchestration system built with Java 17, featuring idempotency support, intelligent routing, and resilience mechanisms.

## Project Overview

This project implements a robust payment orchestration service that manages the complete lifecycle of payments. It provides:

- **Payment Creation & Processing**: Secure payment handling with automatic status tracking
- **Idempotency Layer**: Prevents duplicate payment processing using cached idempotency keys
- **Intelligent Routing**: Routes payments to appropriate providers based on payment method
- **Resilience & Failover**: Handles provider failures gracefully with error logging
- **Type-Safe DTOs**: Java 17 Records for type-safe request/response handling
- **Global Exception Handling**: Comprehensive error handling with meaningful error responses
- **H2 In-Memory Database**: Easy setup for development and testing

## Technology Stack

- **Framework**: Spring Boot 3.5.14
- **Language**: Java 17 with Records and Switch Expressions
- **Database**: H2 In-Memory Database
- **ORM**: Spring Data JPA with Hibernate
- **Caching**: Spring Boot Cache with ConcurrentMapCacheManager (for Idempotency)
- **Validation**: Jakarta Bean Validation (Hibernate Validator)
- **Build Tool**: Maven
- **Additional Libraries**: Lombok, SLF4J

## Project Structure

```
payment/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/yuno/payment/
│   │   │       ├── controller/
│   │   │       │   └── PaymentController.java
│   │   │       ├── service/
│   │   │       │   └── PaymentService.java
│   │   │       ├── repository/
│   │   │       │   ├── PaymentRepository.java
│   │   │       │   └── PaymentProviderRepository.java
│   │   │       ├── entity/
│   │   │       │   ├── Payment.java
│   │   │       │   ├── PaymentProvider.java
│   │   │       │   ├── PaymentStatus.java
│   │   │       │   └── PaymentMethod.java
│   │   │       ├── dto/
│   │   │       │   ├── CreatePaymentRequest.java
│   │   │       │   └── PaymentResponse.java
│   │   │       ├── exception/
│   │   │       │   ├── GlobalExceptionHandler.java
│   │   │       │   └── ErrorResponse.java
│   │   │       └── PaymentApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── schema.sql
│   │       └── data.sql
│   └── test/
│       └── java/
└── pom.xml
```

## Architecture & Design Patterns

### 1. **Idempotency Layer**
- Uses Spring's `@Cacheable` annotation with ConcurrentMapCacheManager
- Caches payment responses using `idempotencyKey` as the cache key
- Prevents duplicate payments when the same idempotency key is used
- Duration: Cached for the application session (configurable via Spring cache settings)

```java
@Cacheable(value = "idempotencyCache", key = "#request.idempotencyKey")
public PaymentResponse createPayment(CreatePaymentRequest request)
```

### 2. **Routing Engine**
- Uses Java 17 Switch Expressions for clean routing logic
- Routes based on payment method:
  - `CARD` → Provider A (Stripe, Visa, etc.)
  - `UPI` → Provider B (PayTM, Google Pay, etc.)
  - `WALLET`, `BANK_TRANSFER` → Default providers
  
```java
PaymentProvider provider = switch (paymentMethod) {
    case CARD -> providerRepo.findBySupportedMethodAndIsActive(CARD, true).orElse(null);
    case UPI -> providerRepo.findBySupportedMethodAndIsActive(UPI, true).orElse(null);
    // ...
};
```

### 3. **Resilience & Error Handling**
- Try-catch mechanism in `processPaymentWithResilience()`
- Graceful degradation with detailed error logging
- Automatic status updates (FAILED with failure reason)
- Fallback ready for retry queuing or alternative providers

```java
try {
    callPaymentProvider(payment);
    payment.setStatus(PaymentStatus.SUCCESS);
} catch (Exception e) {
    payment.setStatus(PaymentStatus.FAILED);
    payment.setFailureReason(e.getMessage());
    log.error("Payment processing failed", e);
}
```

### 4. **Type Safety with Java 17 Records**
- Immutable DTOs using Records instead of POJOs
- Automatic equals, hashCode, toString methods
- Cleaner and more concise code

```java
public record CreatePaymentRequest(
    @NotBlank String idempotencyKey,
    @NotBlank String customerId,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotBlank String currency,
    @NotNull PaymentMethod paymentMethod,
    String description
) {}
```

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Build the Project

```bash
cd payment
mvn clean install
```

### Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Database Setup

The application automatically initializes the H2 database on startup:

1. **schema.sql**: Creates the `payments` and `payment_providers` tables with indexes
2. **data.sql**: Inserts sample payment provider configurations

### Payment Providers Configuration

| Provider | Supported Method | Status     |
|----------|-----------------|-----------|
| Provider A | CARD          | Active    |
| Provider B | UPI           | Active    |
| Provider C | WALLET        | Active    |
| Provider D | BANK_TRANSFER | Active    |

## H2 Console Access

The H2 console is enabled for easy database inspection.

**URL**: `http://localhost:8080/h2-console`

**Connection Settings**:
- **JDBC URL**: `jdbc:h2:mem:paymentdb`
- **Driver Class**: `org.h2.Driver`
- **User Name**: `sa`
- **Password**: (leave empty)

Click "Connect" to access the console.

## API Endpoints

### 1. Create Payment
**Endpoint**: `POST /v1/payments`

**Request**:
```json
{
  "idempotencyKey": "unique-key-12345",
  "customerId": "cust_001",
  "amount": 100.00,
  "currency": "USD",
  "paymentMethod": "CARD",
  "description": "Purchase product XYZ"
}
```

**Response** (201 Created):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "idempotencyKey": "unique-key-12345",
  "customerId": "cust_001",
  "amount": 100.00,
  "currency": "USD",
  "paymentMethod": "CARD",
  "status": "SUCCESS",
  "provider": "Provider A",
  "description": "Purchase product XYZ",
  "failureReason": null,
  "createdAt": "2026-05-25T10:30:00",
  "updatedAt": "2026-05-25T10:30:05"
}
```

### 2. Get Payment Status
**Endpoint**: `GET /v1/payments/{id}`

**Response** (200 OK):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "idempotencyKey": "unique-key-12345",
  "customerId": "cust_001",
  "amount": 100.00,
  "currency": "USD",
  "paymentMethod": "CARD",
  "status": "SUCCESS",
  "provider": "Provider A",
  "description": "Purchase product XYZ",
  "failureReason": null,
  "createdAt": "2026-05-25T10:30:00",
  "updatedAt": "2026-05-25T10:30:05"
}
```

## Sample curl Commands

### Create a CARD Payment
```bash
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "idempotencyKey": "card-payment-001",
    "customerId": "cust_123",
    "amount": 50.00,
    "currency": "USD",
    "paymentMethod": "CARD",
    "description": "Card payment for order #12345"
  }'
```

### Create a UPI Payment
```bash
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "idempotencyKey": "upi-payment-001",
    "customerId": "cust_456",
    "amount": 75.50,
    "currency": "INR",
    "paymentMethod": "UPI",
    "description": "UPI payment for membership"
  }'
```

### Test Idempotency (Same idempotency key returns cached result)
```bash
# First request - creates payment
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "idempotencyKey": "idempotent-key-001",
    "customerId": "cust_789",
    "amount": 100.00,
    "currency": "USD",
    "paymentMethod": "CARD",
    "description": "Test idempotency"
  }'

# Second request with same idempotency key - returns same payment (cached)
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "idempotencyKey": "idempotent-key-001",
    "customerId": "cust_789",
    "amount": 100.00,
    "currency": "USD",
    "paymentMethod": "CARD",
    "description": "Test idempotency"
  }'
```

### Get Payment Details
```bash
# Replace 550e8400-e29b-41d4-a716-446655440000 with actual payment ID
curl -X GET http://localhost:8080/v1/payments/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json"
```

### Test Validation Errors
```bash
# Missing required field (idempotencyKey)
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust_999",
    "amount": 50.00,
    "currency": "USD",
    "paymentMethod": "CARD"
  }'
```

### Test Invalid Payment Method
```bash
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "idempotencyKey": "invalid-method-001",
    "customerId": "cust_888",
    "amount": 50.00,
    "currency": "USD",
    "paymentMethod": "INVALID_METHOD",
    "description": "This should fail"
  }'
```

## Payment Status Lifecycle

```
       ┌─────────────────────────────────┐
       │         PENDING                  │
       │  (Initial state on creation)     │
       └────────────────┬──────────────────┘
                        │
              Processing starts
                        │
       ┌────────────────▼──────────────────┐
       │       PROCESSING                  │
       │  (Calling payment provider)       │
       └────────────────┬──────────────────┘
                        │
          Provider Call Result
               /                \
              /                  \
       Success                 Failure
        /                          \
       /                            \
    ┌──────────┐              ┌──────────┐
    │ SUCCESS  │              │  FAILED  │
    │(Payment  │              │(With     │
    │processed)│              │failure   │
    └──────────┘              │reason)   │
                              └──────────┘

Additional: CANCELLED (manual cancellation)
```

## Key Features Explained

### 1. Idempotency Implementation
- **Purpose**: Ensure that duplicate requests with the same idempotency key don't create multiple charges
- **Mechanism**: Using Spring `@Cacheable` with the idempotency key as the cache key
- **Database**: Also stored in the `payments` table with `UNIQUE` constraint on `idempotency_key`
- **Example**: If a client retransmits a request with the same idempotency key, the cached response is returned instantly without re-processing

### 2. Routing Logic
The payment service intelligently routes based on payment method:

```
CARD → Provider A (typically credit/debit card processors)
UPI → Provider B (India's Unified Payments Interface)
WALLET → Provider C (Digital wallets, prepaid accounts)
BANK_TRANSFER → Provider D (Direct bank transfers)
```

### 3. Resilience & Error Handling
- **Try-Catch**: All provider calls are wrapped in try-catch
- **Logging**: Detailed logs at each step for debugging
- **Status Tracking**: Payment status reflects processing state
- **Failure Reason**: Error messages are stored for audit trail
- **Future Enhancement**: Status could be queued for retry in production

### 4. Database Schema

**payments table**:
- `id`: UUID (generated)
- `idempotency_key`: Unique key for idempotency
- `customer_id`: Customer reference
- `amount`, `currency`: Payment amount details
- `payment_method`: CARD, UPI, WALLET, BANK_TRANSFER
- `status`: PENDING, PROCESSING, SUCCESS, FAILED, CANCELLED
- `provider`: Name of the payment provider used
- `failure_reason`: Error details if failed

**payment_providers table**:
- `id`: Provider identifier
- `name`: Provider name (Provider A, B, C, D)
- `supported_method`: Payment method they support
- `api_endpoint`: Provider API URL
- `is_active`: Boolean flag for routing decisions

## Configuration

### Caching Configuration
Caching is configured in `application.properties`:
```properties
spring.cache.type=simple
```

This uses `ConcurrentMapCacheManager` which stores cached data in memory. For production, consider:
- Redis for distributed caching
- Ehcache for more control
- Memcached for high-performance caching

### Database Configuration
```properties
spring.datasource.url=jdbc:h2:mem:paymentdb
spring.jpa.hibernate.ddl-auto=none  # Schema managed by schema.sql
spring.sql.init.mode=always         # Always initialize data
```

## Logging

The application uses SLF4J with Logback. Configure log levels in `application.properties`:

```properties
logging.level.com.yuno.payment=DEBUG      # Application logs
logging.level.org.springframework.web=DEBUG # Web framework logs
logging.level.org.hibernate.SQL=DEBUG      # SQL queries (optional)
```

## Testing

### Integration Test Scenario
1. Start the application
2. Create a CARD payment → Should be routed to Provider A
3. Verify payment status is SUCCESS or FAILED
4. Retry with same idempotency key → Should return cached response instantly
5. Create a UPI payment → Should be routed to Provider B
6. Query payment status endpoint to verify state

### Edge Cases
- Duplicate idempotency keys (idempotency test)
- Invalid payment method
- Missing required fields
- Invalid amount (<=0)
- Non-existent payment ID
- Concurrent requests with same idempotency key

## Production Considerations

1. **Database**: Replace H2 with PostgreSQL/MySQL
2. **Caching**: Implement Redis for distributed caching
3. **Provider Integration**: Implement actual HTTP calls to payment providers
4. **Retry Logic**: Add exponential backoff and retry mechanism for failed payments
5. **Async Processing**: Use message queues (Kafka/RabbitMQ) for asynchronous payment processing
6. **Webhook Handlers**: Implement webhooks to handle provider callbacks
7. **Circuit Breaker**: Use Resilience4j or Hystrix for fault tolerance
8. **Monitoring**: Add Micrometer/Prometheus metrics and monitoring
9. **Security**: Implement OAuth2, API Keys, and request signing
10. **Data Encryption**: Encrypt sensitive payment data at rest and in transit

## Troubleshooting

### Issue: Database initialization fails
**Solution**: Ensure `schema.sql` and `data.sql` are in `src/main/resources/` and have correct SQL syntax.

### Issue: H2 console not accessible
**Solution**: Verify `spring.h2.console.enabled=true` in application.properties and access via `/h2-console`.

### Issue: Validation errors on POST
**Solution**: Ensure all required fields are present and match the expected types. Use the sample curls provided above.

### Issue: Payment not found error
**Solution**: Verify the payment ID is correct. Check the H2 console to verify the payment was created.

## Performance Optimization Tips

1. **Database Indexes**: Already added on `idempotency_key`, `customer_id`, `status`, `created_at`
2. **Caching**: Idempotency cache reduces database queries significantly
3. **Connection Pooling**: HikariCP automatically configured
4. **Query Optimization**: Use `readOnly = true` for GET requests
5. **Batch Processing**: For bulk operations, implement batch endpoints

## License

This project is provided as-is for educational and assessment purposes.

## Support

For issues or questions, please refer to the source code comments and Spring Boot documentation.

---

**Last Updated**: May 25, 2026
**Spring Boot Version**: 3.5.14
**Java Version**: 17

