# Quick Start Guide

Get up and running with the Payment Orchestration System in minutes!

## ⚡ Prerequisites

- Java 17 or higher
- PowerShell (for running Maven wrapper)
- Internet connection (for Maven dependency download on first run)

## 🚀 Start the Application

### Step 1: Navigate to Project Directory
```powershell
cd C:\Users\NEELADRI\IdeaProjects\payment\payment
```

### Step 2: Run the Application
```powershell
.\mvnw.cmd spring-boot:run
```

You'll see output like:
```
[INFO] Building  0.0.1-SNAPSHOT
...
[INFO] Started PaymentApplication in X.XXX seconds
```

The application is now running on `http://localhost:8080`

---

## 🧪 Quick Test

### Create a Payment (PowerShell)

```powershell
$body = @{
    idempotencyKey = "quick-test-001"
    customerId = "customer1"
    amount = 100.00
    currency = "USD"
    paymentMethod = "CARD"
    description = "My first payment"
} | ConvertTo-Json

$response = Invoke-WebRequest -Uri "http://localhost:8080/v1/payments" `
    -Method POST `
    -Headers @{"Content-Type"="application/json"} `
    -Body $body

$response.Content | ConvertFrom-Json | ConvertTo-Json
```

Expected Response (201 Created):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "idempotencyKey": "quick-test-001",
  "customerId": "customer1",
  "amount": 100.0,
  "currency": "USD",
  "paymentMethod": "CARD",
  "status": "SUCCESS",
  "provider": "Provider A",
  "description": "My first payment",
  "failureReason": null,
  "createdAt": "2026-05-25T10:30:00",
  "updatedAt": "2026-05-25T10:30:05"
}
```

### Get Payment Status

```powershell
# Replace with your payment ID from above
$paymentId = "550e8400-e29b-41d4-a716-446655440000"

Invoke-WebRequest -Uri "http://localhost:8080/v1/payments/$paymentId" `
    -Method GET | Select-Object -ExpandProperty Content | ConvertFrom-Json
```

---

## 📊 Access H2 Console

Open browser and navigate to: **http://localhost:8080/h2-console**

Login with:
- JDBC URL: `jdbc:h2:mem:paymentdb`
- User: `sa`
- Password: (empty)

Then run:
```sql
SELECT * FROM payments ORDER BY created_at DESC;
```

---

## 📚 What You Got

✅ Payment creation with automatic routing
✅ Idempotency layer (prevents duplicate charges)
✅ Payment status tracking
✅ H2 in-memory database
✅ REST API endpoints
✅ Global error handling
✅ Comprehensive logging
✅ In-memory caching
✅ Input validation

---

## 📖 Full Documentation

- **README.md** - Complete documentation with API details
- **TESTING_GUIDE.md** - Testing examples with PowerShell
- **IMPLEMENTATION_SUMMARY.md** - Technical details of implementation

---

## 🎯 Common Tasks

### Create CARD Payment
```powershell
$body = @{
    idempotencyKey = "card-$(Get-Date -Format 'yyyyMMddHHmmss')"
    customerId = "cust_123"
    amount = 99.99
    currency = "USD"
    paymentMethod = "CARD"
    description = "Card payment"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8080/v1/payments" `
    -Method POST `
    -Headers @{"Content-Type"="application/json"} `
    -Body $body | Select-Object -ExpandProperty Content | ConvertFrom-Json
```

### Create UPI Payment
```powershell
$body = @{
    idempotencyKey = "upi-$(Get-Date -Format 'yyyyMMddHHmmss')"
    customerId = "cust_456"
    amount = 500
    currency = "INR"
    paymentMethod = "UPI"
    description = "UPI payment"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8080/v1/payments" `
    -Method POST `
    -Headers @{"Content-Type"="application/json"} `
    -Body $body | Select-Object -ExpandProperty Content | ConvertFrom-Json
```

### Test Idempotency
```powershell
$key = "idempotent-test"
$body = @{
    idempotencyKey = $key
    customerId = "cust_789"
    amount = 200
    currency = "USD"
    paymentMethod = "CARD"
    description = "Idempotency test"
} | ConvertTo-Json

$response1 = Invoke-WebRequest -Uri "http://localhost:8080/v1/payments" `
    -Method POST `
    -Headers @{"Content-Type"="application/json"} `
    -Body $body

$response2 = Invoke-WebRequest -Uri "http://localhost:8080/v1/payments" `
    -Method POST `
    -Headers @{"Content-Type"="application/json"} `
    -Body $body

Write-Host "Same ID returned both times? $(if (($response1.Content | ConvertFrom-Json).id -eq ($response2.Content | ConvertFrom-Json).id) {'YES ✓' else {'NO ✗'})"
```

---

## 🔧 Troubleshooting

### Application won't start
1. Check Java version: `java -version` (should be 17+)
2. Kill any existing process on port 8080
3. Clear Maven cache: `.\mvnw.cmd clean`

### H2 Console won't load
1. Check URL: `http://localhost:8080/h2-console`
2. Ensure credentials are correct (sa / empty)
3. Restart the application

### API returns 404
1. Ensure endpoint path is correct: `/v1/payments`
2. Check HTTP method (POST vs GET)
3. Verify JSON format is correct

### Payment ID not found
1. Copy the full UUID from payment creation response
2. Exact match required - no trimming
3. Use from most recent payment creation

---

## 📝 Payment Methods Supported

| Method | Route | Provider |
|--------|-------|----------|
| CARD | ➜ | Provider A |
| UPI | ➜ | Provider B |
| WALLET | ➜ | Provider C |
| BANK_TRANSFER | ➜ | Provider D |

---

## ✨ Key Features

🔐 **Idempotency**: Same request = same response (prevents duplicate charges)
🎯 **Routing**: Automatic provider selection based on payment method
🛡️ **Resilience**: Graceful error handling with detailed logging
✔️ **Validation**: Input validation with meaningful error messages
📦 **Type Safety**: Java 17 Records for immutable DTOs
⚡ **Performance**: In-memory caching and database indexing

---

## 🔗 Useful URLs

- API Base: `http://localhost:8080`
- H2 Console: `http://localhost:8080/h2-console`

---

## 📞 Support

For detailed information, see:
- README.md - Full API documentation
- TESTING_GUIDE.md - Testing examples
- IMPLEMENTATION_SUMMARY.md - Technical details

---

**Enjoy! 🎉**

