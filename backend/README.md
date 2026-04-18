# JwellKeeper Backend

Production-ready Spring Boot backend for a multi-tenant Jewellery Stock Management System.

## Stack

- Java 21
- Spring Boot 4.0.5
- Spring Security with JWT
- Spring Data JPA / Hibernate
- MySQL 8.4 LTS
- Flyway
- Maven
- Lombok
- MapStruct
- springdoc OpenAPI
- ZXing QR generation
- OpenPDF PDF generation

## Structure

```text
src/main/java/com/jwellkeeper/
  auth/        register and login
  users/       owner-managed staff users
  tenant/      tenant settings and bill numbering config
  jewellery/   jewellery types, inventory, QR tokens
  billing/     bills, dynamic prices, PDFs, WhatsApp stub
  audit/       daily stock check, scans, missing items, PDFs
  analytics/   sales and stock summary metrics
  logs/        persisted business transaction logs
  security/    JWT filter, tenant context, principals
  common/      API responses, exceptions, pagination, shared models
  config/      OpenAPI configuration
```

## Tenant Isolation

- Every tenant-owned table has `tenant_id`.
- JWT claims carry `tenantId`, `userId`, `role`, and `email`.
- Services read tenant context from the authenticated principal only.
- Repositories expose tenant-scoped lookup methods such as `findByIdAndTenantId`.

## Running

```bash
docker compose up -d
mvn test
mvn spring-boot:run
```

If startup fails with `Schema validation: missing table [...]`, your local MySQL schema is stale or partially created. For local development only, reset the database and let Flyway recreate every table:

```bash
docker compose down -v
docker compose up -d
mvn spring-boot:run
```

If you use your own MySQL instead of Docker, drop and recreate the `jwellkeeper` database, then restart the app.

On startup you should see Flyway log lines before Hibernate validation. If you do not, confirm `spring-boot-starter-flyway` is present in `pom.xml`.

Configure MySQL and secrets with environment variables:

```text
DB_URL=jdbc:mysql://localhost:3306/jwellkeeper?createDatabaseIfNotExist=true
DB_USERNAME=root
DB_PASSWORD=root
JWT_SECRET=change-me-to-a-very-long-production-secret-at-least-32-bytes
QR_SIGNING_SECRET=change-me-to-a-separate-qr-signing-secret-32-bytes
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

## Sample API Responses

Registration:

```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": "f5c3f8ec-c88d-4ce8-9ce1-a7c5dfb7a0e0",
    "tenantId": "0a1db344-0c1d-45db-b71b-f785a91e638d",
    "role": "OWNER",
    "email": "owner@example.com",
    "name": "Owner"
  }
}
```

Jewellery creation:

```json
{
  "success": true,
  "message": "Jewellery created successfully",
  "data": {
    "id": "7d318ab0-23c3-4767-8f61-72445cfa72a6",
    "typeId": "4f8cba91-bbf2-4d3c-b6a0-5f464751e4f9",
    "typeName": "Bangle",
    "karat": "22K",
    "weight": 12.5,
    "status": "AVAILABLE",
    "createdAt": "2026-04-17T09:00:00Z",
    "soldAt": null,
    "deletedAt": null,
    "version": 0
  }
}
```

QR image:

```json
{
  "success": true,
  "message": "QR generated",
  "data": {
    "contentType": "image/png",
    "qrCodeBase64": "data:image/png;base64,iVBORw0KGgo..."
  }
}
```

Bill creation:

```json
{
  "success": true,
  "message": "Bill created",
  "data": {
    "id": "695a8f90-bc28-4b8b-886d-c934a16b1476",
    "billNo": "JK-000001",
    "billDate": "2026-04-17",
    "currencyCode": "CAD",
    "totalAmount": 2500.00,
    "customerName": "Customer",
    "customerPhone": "+14165550123",
    "paymentMethod": "CARD",
    "items": [
      {
        "jewelleryId": "7d318ab0-23c3-4767-8f61-72445cfa72a6",
        "typeNameSnapshot": "Bangle",
        "karatSnapshot": "22K",
        "weight": 12.5,
        "finalPrice": 2500.00,
        "ratePerGram": 200.00
      }
    ],
    "pdfUrl": "/api/bill/695a8f90-bc28-4b8b-886d-c934a16b1476/pdf"
  }
}
```

Audit close with missing items:

```json
{
  "success": true,
  "message": "Audit closed",
  "data": {
    "id": "cda3c628-373c-4589-90a9-91a582f11773",
    "auditDate": "2026-04-17",
    "status": "CLOSED",
    "manuallyClosed": true,
    "totalItems": 35,
    "scannedItems": 34,
    "missingItems": 1,
    "pdfUrl": "/api/audit/cda3c628-373c-4589-90a9-91a582f11773/pdf"
  }
}
```

Error:

```json
{
  "success": false,
  "message": "Only available jewellery can be billed: 7d318ab0-23c3-4767-8f61-72445cfa72a6",
  "data": null
}
```
