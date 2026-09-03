# Invoice Application Testing Guide

## Backend Testing

### 1. Verify Backend is Running
```bash
# Check if Java process is listening on port 8080
netstat -tlnp | grep 8080
# or
ss -tlnp | grep 8080
```

### 2. Test Login Endpoint Directly
```bash
# Test authentication
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

# Expected response includes `token`, `refreshToken`, `username`, and `expiresInMs`.
```

### 3. Test Invoice List Endpoint (Requires Valid Token)
```bash
# First get a token from login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq -r '.token')

# Then use it to get invoices
curl -X GET http://localhost:8080/api/invoices \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json"
```

### 4a. Test Search, Filters, Sorting, and Pagination
```bash
curl "http://localhost:8080/api/invoices?search=Quadient&status=DRAFT&page=0&size=20&sort=amount,desc" \
  -H "Authorization: Bearer $TOKEN"
```

Expected response: an object containing `content`, `page`, `size`, `totalElements`, and `totalPages`.

### 5. Test Invoice Detail Endpoint
```bash
# Get invoice with ID 1
curl -X GET http://localhost:8080/api/invoices/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json"
```

### 6. Test Create Invoice
```bash
curl -X POST http://localhost:8080/api/invoices \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "customer": "Test Company",
    "amount": "1000.00",
    "currency": "USD"
  }'
```

### 7. Test Update Invoice Information
```bash
curl -X PUT http://localhost:8080/api/invoices/1/info \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "customerAddress": "123 Main St, City",
    "customerContactEmail": "contact@company.com",
    "customerContactPhone": "+1-555-0100",
    "supplier": "Supplier Inc",
    "supplierAddress": "456 Business Ave",
    "supplierContactEmail": "sales@supplier.com",
    "supplierContactPhone": "+1-555-0200",
    "dueDate": "2026-09-15",
    "paymentTerms": "Net 30",
    "discountPercentage": "5.00",
    "taxPercentage": "10.00",
    "notes": "Test invoice"
  }'
```

### 8. Test Add Line Item
```bash
curl -X POST http://localhost:8080/api/invoices/1/line-items \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "description": "Professional Services",
    "quantity": "10",
    "unitPrice": "100.00",
    "taxPercentage": "10.00",
    "discountPercentage": "0"
  }'
```

### 9. Test Update Status
```bash
curl -X PATCH http://localhost:8080/api/invoices/1/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"SUBMITTED"}'
```

### 10. Test Delete Invoice
```bash
curl -X DELETE http://localhost:8080/api/invoices/1 \
  -H "Authorization: Bearer $TOKEN"
```

### 11. Test XML Import
```bash
cat > /tmp/invoice.xml <<'EOF'
<Invoice>
  <invoiceNumber>XML-TEST-001</invoiceNumber>
  <customer>XML Customer</customer>
  <amount>125.50</amount>
  <currency>EUR</currency>
  <issuedOn>2026-09-03</issuedOn>
</Invoice>
EOF

curl -X POST http://localhost:8080/api/invoices/import/xml \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/tmp/invoice.xml"
```

Expected response: `201 Created` with a new `DRAFT` invoice. Also test a missing required field,
a non-XML extension, and an XML containing a `DOCTYPE`; each must be rejected.

## XML Integration Test Case for Next Session

### Preconditions
- Start Spring Boot on port `8080`.
- Confirm PostgreSQL is running.
- Obtain an access token using the `admin` / `admin` credentials.

### Test Data
Create `/tmp/invoice.xml` with:

```xml
<Invoice>
  <invoiceNumber>XML-TEST-001</invoiceNumber>
  <customer>XML Customer</customer>
  <amount>125.50</amount>
  <currency>EUR</currency>
  <issuedOn>2026-09-03</issuedOn>
  <dueDate>2026-10-03</dueDate>
  <supplier>Test Supplier</supplier>
  <notes>Imported from XML</notes>
</Invoice>
```

### Test Steps
1. Login and save the returned `token` as `TOKEN`.
2. Upload the XML file:

```bash
curl -i -X POST http://localhost:8080/api/invoices/import/xml \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/tmp/invoice.xml"
```

3. Confirm the response is `201 Created` and contains a `DRAFT` invoice with customer
   `XML Customer`, amount `125.50`, currency `EUR`, supplier `Test Supplier`, and due date `2026-10-03`.
4. Confirm the imported invoice can be found:

```bash
curl -sS "http://localhost:8080/api/invoices?search=XML-TEST-001" \
  -H "Authorization: Bearer $TOKEN"
```

5. Confirm the original XML is listed in the invoice attachments.
6. Confirm these invalid inputs return `400 Bad Request`:
   - A file whose name does not end in `.xml`.
   - XML missing `customer`, `amount`, or `currency`.
   - XML containing a `DOCTYPE` or external entity.

### Expected Result
The valid XML creates one draft invoice and stores the original file. Invalid or unsafe XML
is rejected without creating an invoice.

---

## Flutter Testing

### 1. Check Emulator Network Connectivity
The Flutter app on Android emulator uses `http://10.0.2.2:8080` to access the host machine's localhost.

To verify:
- Open Dart DevTools: `flutter pub global run devtools`
- Check the app's console output for network errors

### 2. View Flutter Logs
```bash
# In the Flutter project directory
flutter logs
```

### 3. Run Flutter Analyzer
```bash
flutter analyze
```

### 4. Run Flutter Unit Tests
```bash
flutter test
```

### 5. Rebuild Flutter App
```bash
cd /mnt/c/Users/rosha/invoice_demo_flutter
flutter pub get
flutter run
```

### 6. Manual Workflow Verification
- Log in with `admin` / `admin`.
- Search by invoice number or customer.
- Filter by status and change sorting.
- Open an invoice and verify details.
- Test `DRAFT -> SUBMITTED -> APPROVED -> PAID`.
- Test rejection with a required reason and recovery to draft.
- Delete a test invoice and confirm it disappears from the list.
- Log out and log in again; confirm the session is restored after restart.

---

## Frontend Testing Checklist

### Login Screen
- [ ] Enter admin/admin credentials
- [ ] Check console logs (flutter logs) for error messages
- [ ] Verify backend is accessible: `curl http://localhost:8080/api/auth/login`
- [ ] Check if error message displays on screen (we added better error handling)

### Invoice List Screen (After Login)
- [ ] Invoices should load and display
- [ ] Pull-to-refresh should work
- [ ] Tap on invoice should navigate to detail screen
- [ ] Logout button should work

### Invoice Detail Screen
- [ ] All invoice information should display
- [ ] Customer and supplier info should show (if available)
- [ ] Line items should display correctly
- [ ] Edit button should navigate to edit screen

### Invoice Edit Screen
- [ ] Should populate all fields with current data
- [ ] Should allow editing customer/supplier info
- [ ] Should allow adding line items
- [ ] Should allow removing line items
- [ ] Save should persist changes

---

## Common Issues & Solutions

### Issue: "Returns to login page"
**Possible Causes:**
1. **Backend not accessible** - Check `flutter logs` for network errors
2. **Wrong backend URL** - Emulator uses `10.0.2.2` for localhost
3. **JSON parsing error** - Check backend response format
4. **Token not saved** - Check `flutter_secure_storage` permissions

**Solutions:**
```bash
# 1. Verify backend is running and accessible
curl http://localhost:8080/api/auth/login -X POST -H "Content-Type: application/json" -d '{"username":"admin","password":"admin"}'

# 2. Check Flutter logs for detailed error
flutter logs

# 3. Test from emulator (if you can access shell)
adb shell curl http://10.0.2.2:8080/api/auth/login -X POST ...
```

### Issue: "Connection timeout"
**Cause:** Backend not running or firewall blocking
**Solution:**
```bash
# Check if backend is running
ps aux | grep java

# Check if port 8080 is open
netstat -tlnp | grep 8080

# Restart backend
cd /home/e015293/projects/invoice-demo
mvn spring-boot:run -DskipTests
```

### Issue: "Authorization: Bearer token missing"
**Cause:** Token not being saved or retrieved
**Solution:**
```bash
# Run on emulator shell to check stored tokens
adb shell run-as com.example.invoice_demo_flutter sqlite3 /data/data/com.example.invoice_demo_flutter/shared_prefs/FlutterSecureStorage.db
```

---

## Running Complete Test Suite

### Backend Tests
```bash
cd /home/e015293/projects/invoice-demo

# Run unit tests
mvn test

# Run with specific test class
mvn test -Dtest=InvoiceRepositoryTest

# Run with detailed logging
mvn test -X
```

### Flutter Tests
```bash
cd /mnt/c/Users/rosha/invoice_demo_flutter

# Run all tests
flutter test

# Run specific test
flutter test test/invoice_test.dart

# Run with verbose output
flutter test -v
```

---

## Manual Testing Flow

### Step 1: Start Backend
```bash
cd /home/e015293/projects/invoice-demo
mvn spring-boot:run -DskipTests
# Wait for: "Started InvoiceDemoApplication"
```

### Step 2: Start Flutter App
```bash
cd /mnt/c/Users/rosha/invoice_demo_flutter
flutter run
```

### Step 3: Test Login
- Enter: `admin` / `admin`
- Expected: Navigates to invoice list screen
- If fails: Check `flutter logs` for error

### Step 4: Test Invoice List
- Should display invoices (if any exist)
- Pull down to refresh
- Expected: List updates

### Step 5: Create Test Invoice (via curl)
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq -r '.token')

curl -X POST http://localhost:8080/api/invoices \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"customer":"Test Corp","amount":"5000.00","currency":"USD"}'
```

### Step 6: Refresh Flutter App
- Refresh invoice list (pull down)
- New invoice should appear

### Step 7: Test Invoice Detail
- Tap on invoice in list
- Should navigate to detail screen
- Should display all information

### Step 8: Test Invoice Edit
- Tap "Edit Invoice" button
- Update customer information
- Add line item:
  - Description: "Service"
  - Quantity: 5
  - Unit Price: 100
  - Tax: 10%
- Click "Save Changes"
- Should return to detail screen with updated data

### Step 9: Test Line Item Operations
- On detail screen, tap "Edit Invoice"
- Add multiple line items
- Delete a line item
- Verify totals recalculate

---

## Verification Checklist

### ✅ Backend
- [ ] Compiles without errors: `mvn clean compile`
- [ ] Tests pass: `mvn test`
- [ ] Runs successfully: `mvn spring-boot:run`
- [ ] Login endpoint works: `curl http://localhost:8080/api/auth/login ...`
- [ ] Protected endpoints require token

### ✅ Flutter
- [ ] No analyzer warnings: `flutter analyze`
- [ ] Tests pass: `flutter test`
- [ ] App builds: `flutter run`
- [ ] Login works with valid credentials
- [ ] Can view invoice list
- [ ] Can view invoice details
- [ ] Can edit invoice information
- [ ] Can add/remove line items
- [ ] Calculations are correct

---

## Next Steps

After testing, the following features can be added:
1. Attachment file upload/download
2. Invoice PDF export
3. Advanced search and filtering
4. Role-based access control
5. Database migrations (Flyway/Liquibase)
6. Production configuration
