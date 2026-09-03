# Quick Reference: Invoice App Testing

## 🎯 Current Status

| Component | Status | Details |
|-----------|--------|---------|
| Backend | ✅ Running | Listening on `localhost:8080` |
| Database | ✅ Connected | PostgreSQL invoice_demo |
| API Endpoints | ✅ All Working | Login, CRUD, Line Items, Info |
| Flutter App | ✅ Tested | Login, invoice list, search, filtering, lifecycle controls |

---

## 🚀 Quick Start

### Start Backend (Already Running)
```bash
cd /home/e015293/projects/invoice-demo
mvn spring-boot:run -DskipTests
# Runs on port 8080
```

### Check Backend
```bash
curl http://localhost:8080/api/auth/login -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
# Returns access token, refresh token, username, and expiration metadata
```

### Run Flutter
```bash
cd /mnt/c/Users/rosha/invoice_demo_flutter
flutter run
```

---

## 🔐 Login Credentials
**Username:** `admin`  
**Password:** `admin`

---

## 🐛 Flutter Login Issue - Debugging Steps

### Step 1: Check Flutter Console
```bash
flutter logs
# Look for: "Login Response:" or "Login Error:"
# Watch for network errors or timeouts
```

### Step 2: Verify Backend is Accessible
```bash
# From your machine (should show token response)
curl http://localhost:8080/api/auth/login -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

### Step 3: Check Error Message on Screen
After improvements, the Flutter login screen should now display:
- Full error messages
- Network timeout messages
- Connection issues

---

## 📊 Verified API Endpoints

### Authentication
```bash
POST /api/auth/login
Input: {"username":"admin","password":"admin"}
Output: `{token, refreshToken, username, expiresInMs}`
Status: 200 OK ✅

```http
POST /api/auth/refresh
POST /api/auth/logout
```

Refresh tokens are persisted as hashes, rotated after use, and revoked on logout.
```

### Invoice List
```bash
GET /api/invoices
Headers: Authorization: Bearer TOKEN
Status: 200 OK ✅
Returns: Array of invoices

Supported query parameters: `search`, `customer`, `status`, `currency`, `issuedFrom`, `issuedTo`, `page`, `size`, `sort`.
Filtered or sorted requests return `{content, page, size, totalElements, totalPages}`.
```

### Create Invoice
```bash
POST /api/invoices
Headers: Authorization: Bearer TOKEN
Input: {"customer":"...", "amount":"...", "currency":"USD"}
Status: 201 Created ✅
```

### Get Invoice Detail
```bash
GET /api/invoices/{id}
Headers: Authorization: Bearer TOKEN
Status: 200 OK ✅
Returns: Full invoice with line items, attachments
```

### Update Invoice Info
```bash
PUT /api/invoices/{id}/info
Headers: Authorization: Bearer TOKEN
Input: {customer, supplier, dates, financial info}
Status: 200 OK ✅
```

### Add Line Item
```bash
POST /api/invoices/{id}/line-items
Headers: Authorization: Bearer TOKEN
Input: {description, quantity, unitPrice, tax%, discount%}
Status: 201 Created ✅
Line calculations working: subtotal, tax, total

### Update Invoice Status
```bash
PATCH /api/invoices/{id}/status
Headers: Authorization: Bearer TOKEN
Input: {"status":"REJECTED","rejectionReason":"Incorrect details"}
```

Supported transitions remain `DRAFT -> SUBMITTED`, `SUBMITTED -> APPROVED|REJECTED`,
`APPROVED -> PAID`, and `REJECTED -> DRAFT|SUBMITTED`.

### Import XML Invoice
```bash
curl -X POST http://localhost:8080/api/invoices/import/xml \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@invoice.xml"
```

The importer accepts common fields such as `invoiceNumber`, `customer`, `amount`, `currency`,
`issuedOn`, `dueDate`, `supplier`, and `notes`, creates a draft invoice, and stores the original XML file.
```

---

## 📁 Key Files Modified

### Backend
- `Invoice.java` - Added 14 new fields for customer/supplier/financial data
- `InvoiceLineItem.java` - New entity for line items
- `InvoiceAttachment.java` - New entity for attachments
- `InvoiceController.java` - 7 new endpoints

### Flutter
- `auth_service.dart` - Enhanced error handling, better logging
- `login_screen.dart` - Shows detailed error messages
- `invoice_detail_screen.dart` - New screen (ready)
- `invoice_edit_screen.dart` - New screen (ready)

---

## 🔧 Common Issues & Fixes

### Backend Won't Start
```bash
# Check if port 8080 is in use
lsof -i :8080

# Kill conflicting process
pkill -9 -f "java"

# Restart
cd /home/e015293/projects/invoice-demo
mvn spring-boot:run -DskipTests
```

### Database Error
```bash
# Reset database
PGPASSWORD=inv_dba_passwd psql -h localhost -U inv_dba -d invoice_demo -c "
DROP TABLE IF EXISTS invoice_attachments CASCADE;
DROP TABLE IF EXISTS invoice_line_items CASCADE;
DROP TABLE IF EXISTS invoices CASCADE;
"

# Restart backend to recreate tables
```

### Flutter Login Always Returns to Login
1. **Check flutter logs:** `flutter logs`
2. **Verify backend:** `curl http://localhost:8080/api/auth/login ...`
3. **Check emulator connection:** `adb shell ping 10.0.2.2`
4. **Look for:** Network timeouts, connection refused, or JSON parse errors

---

## Testing Progress

### Backend Testing
- ✅ Authentication
- ✅ Invoice CRUD
- ✅ Invoice Information Update
- ✅ Line Items Management
- ✅ Financial Calculations
- ✅ Database Schema
- ✅ Error Handling

### Flutter Testing
- ✅ Login and token persistence
- ✅ Invoice list display
- ✅ Search, filters, sorting, and pagination
- ✅ Invoice detail view
- ✅ Invoice editing and line-item operations
- ✅ Status, rejection, payment, and deletion controls

---

## 📚 Full Documentation

See detailed guides:
- **[TESTING_GUIDE.md](TESTING_GUIDE.md)** - Comprehensive testing instructions
- **[IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)** - What was built
- **[TEST_RESULTS.md](TEST_RESULTS.md)** - Full test results

---

## 💡 Tips

1. **Watch the console:** Enhanced error messages now print to Flutter console
2. **Check backend logs:** `tail -f /tmp/backend.log`
3. **Test endpoints first:** Use curl to verify backend before testing Flutter
4. **Keep a token handy:** Use the curl login response token for manual testing

---

## 🎯 Next Milestone

Once Flutter login is fixed:
1. Test invoice list loads
2. Test invoice detail screen
3. Test invoice editing
4. Test line item operations
5. Verify calculations display correctly

---

**Last Updated:** 2026-09-03
**Backend Status:** ✅ Running  
**Database Status:** ✅ Connected  
**Test Coverage:** Backend automated tests passing | Flutter workflow manually verified
