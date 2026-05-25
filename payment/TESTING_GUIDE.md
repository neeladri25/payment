# Payment API Testing Guide for PowerShell

This guide provides PowerShell-compatible commands to test the Payment Orchestration API.

## Starting the Application

```powershell
cd "C:\Users\NEELADRI\IdeaProjects\payment\payment"
.\mvnw.cmd spring-boot:run
```

The application will start on `http://localhost:8080`

---

## Testing Endpoints with PowerShell

### 1. Create a CARD Payment

```powershell
$body = @{
    idempotencyKey = "card-001-$(Get-Date -Format 'yyyyMMddHHmmss')"
    customerId = "cust_123"
    amount = 50.00
    currency = "USD"
    paymentMethod = "CARD"
    description = "Test CARD payment"
} | ConvertTo-Json

$response = Invoke-WebRequest -Uri "http://localhost:8080/v1/payments" `
    -Method POST `
    -Headers @{"Content-Type"="application/json"} `
    -Body $body

$response.Content | ConvertFrom-Json | ConvertTo-Json
```

### 2. Create a UPI Payment

```powershell
$body = @{
    idempotencyKey = "upi-001-$(Get-Date -Format 'yyyyMMddHHmmss')"
    customerId = "cust_456"
    amount = 75.50
    currency = "INR"
    paymentMethod = "UPI"
    description = "Test UPI payment"
} | ConvertTo-Json

$response = Invoke-WebRequest -Uri "http://localhost:8080/v1/payments" `
    -Method POST `
    -Headers @{"Content-Type"="application/json"} `
    -Body $body

$response.Content | ConvertFrom-Json | ConvertTo-Json
```

### 3. Test Idempotency (Create Same Payment Twice)

```powershell
# Use the same idempotency key
$idempotencyKey = "idempotent-test-001"

$body = @{
    idempotencyKey = $idempotencyKey
    customerId = "cust_789"
    amount = 100.00
    currency = "USD"
    paymentMethod = "CARD"
    description = "Test idempotency"
} | ConvertTo-Json

# First request
Write-Host "First Request (creates payment):" -ForegroundColor Green
$response1 = Invoke-WebRequest -Uri "http://localhost:8080/v1/payments" `
    -Method POST `
    -Headers @{"Content-Type"="application/json"} `
    -Body $body

$payment1 = $response1.Content | ConvertFrom-Json
Write-Host $payment1.id
$paymentId = $payment1.id

# Second request with same idempotency key (returns cached)
Write-Host "`nSecond Request (returns cached result instantly):" -ForegroundColor Green
$response2 = Invoke-WebRequest -Uri "http://localhost:8080/v1/payments" `
    -Method POST `
    -Headers @{"Content-Type"="application/json"} `
    -Body $body

$payment2 = $response2.Content | ConvertFrom-Json
Write-Host $payment2.id
Write-Host "Same ID returned? $(if ($payment1.id -eq $payment2.id) {'YES ✓'} else {'NO ✗'})" -ForegroundColor Cyan
```

### 4. Get Payment Status

```powershell
# Replace with actual payment ID from previous response
$paymentId = "550e8400-e29b-41d4-a716-446655440000"

$response = Invoke-WebRequest -Uri "http://localhost:8080/v1/payments/$paymentId" `
    -Method GET `
    -Headers @{"Content-Type"="application/json"}

$response.Content | ConvertFrom-Json | ConvertTo-Json
```

### 5. Test Validation - Missing Required Field

```powershell
# This should fail - missing idempotencyKey
$body = @{
    customerId = "cust_999"
    amount = 50.00
    currency = "USD"
    paymentMethod = "CARD"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/v1/payments" `
        -Method POST `
        -Headers @{"Content-Type"="application/json"} `
        -Body $body
} catch {
    Write-Host "Expected Error Occurred:" -ForegroundColor Red
    $_.Exception.Response.Content.ToString() | ConvertFrom-Json | ConvertTo-Json
}
```

### 6. Test Invalid Payment Amount

```powershell
# This should fail - amount <= 0
$body = @{
    idempotencyKey = "invalid-amount-001"
    customerId = "cust_888"
    amount = -50.00
    currency = "USD"
    paymentMethod = "CARD"
    description = "Invalid amount"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/v1/payments" `
        -Method POST `
        -Headers @{"Content-Type"="application/json"} `
        -Body $body
} catch {
    Write-Host "Expected Error Occurred:" -ForegroundColor Red
    $_.Exception.Response.Content.ToString() | ConvertFrom-Json | ConvertTo-Json
}
```

### 7. Test Non-Existent Payment

```powershell
# This should fail - payment not found
$response = Invoke-WebRequest -Uri "http://localhost:8080/v1/payments/non-existent-id" `
    -Method GET `
    -Headers @{"Content-Type"="application/json"} `
    -ErrorAction SilentlyContinue

if ($response.StatusCode -ne 200) {
    Write-Host "Expected Error: Payment not found" -ForegroundColor Red
    $response.Content | ConvertFrom-Json | ConvertTo-Json
}
```

---

## Create a Reusable Testing Function

Here's a PowerShell function for easier testing:

```powershell
function New-PaymentRequest {
    param(
        [string]$IdempotencyKey = "payment-$(Get-Random)-$(Get-Date -Format 'yyyyMMddHHmmss')",
        [string]$CustomerId = "cust_default",
        [decimal]$Amount = 100.00,
        [string]$Currency = "USD",
        [string]$PaymentMethod = "CARD",
        [string]$Description = "Test payment"
    )

    $body = @{
        idempotencyKey = $IdempotencyKey
        customerId = $CustomerId
        amount = $Amount
        currency = $Currency
        paymentMethod = $PaymentMethod
        description = $Description
    } | ConvertTo-Json

    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/v1/payments" `
            -Method POST `
            -Headers @{"Content-Type"="application/json"} `
            -Body $body

        return $response.Content | ConvertFrom-Json
    } catch {
        Write-Host "Error creating payment:" -ForegroundColor Red
        return $_.Exception.Response.Content.ToString() | ConvertFrom-Json
    }
}

function Get-PaymentStatus {
    param([string]$PaymentId)

    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/v1/payments/$PaymentId" `
            -Method GET `
            -Headers @{"Content-Type"="application/json"}

        return $response.Content | ConvertFrom-Json
    } catch {
        Write-Host "Error fetching payment:" -ForegroundColor Red
        return $_.Exception.Response.Content.ToString() | ConvertFrom-Json
    }
}
```

### Usage:

```powershell
# Create CARD payment
$payment = New-PaymentRequest -PaymentMethod "CARD" -Amount 100.00
Write-Host "Payment ID: $($payment.id)" -ForegroundColor Green
Write-Host "Status: $($payment.status)" -ForegroundColor Cyan

# Create UPI payment
$payment2 = New-PaymentRequest -PaymentMethod "UPI" -Currency "INR" -Amount 500.00
Write-Host "UPI Payment ID: $($payment2.id)" -ForegroundColor Green

# Get status
$status = Get-PaymentStatus -PaymentId $payment.id
Write-Host "Current Status: $($status.status)" -ForegroundColor Cyan
```

---

## H2 Console Access

### Via Browser
Navigate to: `http://localhost:8080/h2-console`

**Login Credentials:**
- JDBC URL: `jdbc:h2:mem:paymentdb`
- User Name: `sa`
- Password: (leave empty)

### Query Payment Data

Once logged in, run these SQL queries:

```sql
-- View all payments
SELECT * FROM payments ORDER BY created_at DESC;

-- View payment providers
SELECT * FROM payment_providers;

-- View payments by status
SELECT * FROM payments WHERE status = 'SUCCESS';

-- View failed payments
SELECT * FROM payments WHERE status = 'FAILED';

-- View payments by method
SELECT * FROM payments WHERE payment_method = 'CARD';
```

---

## Monitoring Request/Response

### Inspect Full Response

```powershell
$response = Invoke-WebRequest -Uri "http://localhost:8080/v1/payments" `
    -Method POST `
    -Headers @{"Content-Type"="application/json"} `
    -Body $jsonBody

# View response details
Write-Host "Status Code: $($response.StatusCode)"
Write-Host "Content Type: $($response.Headers['Content-Type'])"
Write-Host "Response Time: $(Get-Date)"
Write-Host "Response Size: $($response.Content.Length) bytes"
Write-Host ""
Write-Host "Response Body:" -ForegroundColor Green
$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### Timing Requests (Idempotency Performance Test)

```powershell
$idempotencyKey = "timing-test-$(Get-Date -Format 'yyyyMMddHHmmssfff')"
$body = @{
    idempotencyKey = $idempotencyKey
    customerId = "timing_test"
    amount = 50
    currency = "USD"
    paymentMethod = "CARD"
    description = "Timing test"
} | ConvertTo-Json

# First request (processing)
$time1 = Measure-Command {
    $response1 = Invoke-WebRequest -Uri "http://localhost:8080/v1/payments" `
        -Method POST `
        -Headers @{"Content-Type"="application/json"} `
        -Body $body
}

# Second request (cached)
$time2 = Measure-Command {
    $response2 = Invoke-WebRequest -Uri "http://localhost:8080/v1/payments" `
        -Method POST `
        -Headers @{"Content-Type"="application/json"} `
        -Body $body
}

Write-Host "First request (processing): $($time1.TotalMilliseconds)ms" -ForegroundColor Green
Write-Host "Second request (cached): $($time2.TotalMilliseconds)ms" -ForegroundColor Cyan
Write-Host "Performance improvement: $(($time1.TotalMilliseconds / $time2.TotalMilliseconds).ToString('0.0'))x faster"
```

---

## Troubleshooting

### Connection Refused
- Ensure the application is running: `.\mvnw.cmd spring-boot:run`
- Check if port 8080 is available
- Wait a few seconds for the application to fully start

### JSON Parsing Errors
- Ensure JSON is properly formatted
- Use `ConvertTo-Json` to format objects correctly
- Check that all required fields are present

### H2 Console Connection Issues
- Verify the JDBC URL matches: `jdbc:h2:mem:paymentdb`
- Ensure username is `sa` (not blank)
- Check that h2-console is enabled in application.properties

---

## Production Testing Considerations

For production environments, consider using:
- **Postman** - GUI-based API testing
- **Thunder Client** - VS Code extension
- **REST Client** - VS Code extension
- **Swagger UI** - Auto-generated from OpenAPI annotations
- **LoadRunner** - Performance and load testing
- **JMeter** - Load testing tool

---

## Additional Resources

- **README.md** - Complete API documentation
- **application.properties** - Configuration reference
- **Spring Boot Docs** - https://spring.io/projects/spring-boot
- **H2 Database** - http://www.h2database.com/

---

**Happy Testing! 🚀**

