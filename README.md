# Please Refer the following Blog
[About Jewell Keeper](https://thareejan.blog/projects/jewellkeeper?from=home&open=projects)



# JwellKeeper - Jewellery Stock Management

JwellKeeper helps jewellery shops track individual pieces from intake through sale and daily stock reconciliation. Shop owners and staff can register an item, print its QR label, bill it at the price agreed at checkout, and scan the remaining stock at closing time. Each shop has its own inventory, users, settings, and records.

## Why Jewellery Shops Need It

| Shop need | How JwellKeeper addresses it |
| --- | --- |
| Identify similar-looking pieces with different weights, karats, or designs | Each piece has its own inventory record and scannable QR label. |
| Handle selling prices that change between transactions | The bill line records the agreed price and item details at checkout. The inventory record tracks the piece itself. |
| Reconcile the display and vault at closing time | The audit captures the expected stock, tracks scans, and keeps unscanned pieces visible for follow-up. |
| Trace who changed stock and why | Stock movements record changes such as sale, adjustment, or archive. Business logs record selected user actions. |
| Decide what to replenish or review | Sales by type and karat, stock aging, and missing-item counts give owners a basis for purchasing and follow-up. |

## Overall System Architecture

![Architecture diagram showing the shop browser, Next.js dashboard and API gateway, Spring Boot API, MySQL, and file storage](/jwellkeeper-architecture.svg)

1. **Browser.** Owners and staff use the Next.js dashboard to manage stock, record sales, scan labels with a camera or enter codes manually, and open or download PDFs.
2. **Next.js backend for frontend (BFF).** Login and registration route handlers store the backend JWT and encrypted session metadata in HTTP-only cookies. For protected API requests, the Next.js server reads the JWT and forwards it to Spring Boot as a bearer token. The proxy carries JSON, image uploads, and document responses.
3. **Spring Boot API.** Spring Security validates the JWT and establishes the tenant context. Controllers handle REST requests, services apply business rules, and Spring Data JPA repositories query tenant-owned records using that context.
4. **Storage and generated files.** MySQL stores shop records, inventory, bills, audit runs, stock movements, business logs, and image metadata. Product photos and shop logos are stored on the backend filesystem. ZXing renders QR PNGs from stored codes, and OpenPDF generates bill and audit PDFs on demand.

## Features

| Area | What users can do |
| --- | --- |
| Inventory | Register jewellery by type, sub-type or design, karat, and weight. Add notes and photos, search records, and filter by type, karat, status, or weight. |
| QR labelling | Preview and download QR codes. Print a compact tag for one item or select multiple pieces for a bulk print run. Labels include the shop name and key item details. |
| Billing | Scan or select available jewellery, enter sale prices and optional price breakdowns, and issue bills in the selected currency. Search by bill number, customer, or date range. View or download the bill as a printable PDF. |
| Daily stock audit | Scan items from the expected stock list, review outstanding pieces, and close the audit. View and export a report with current stock, same-day additions, same-day sales, missing items, and a tally by jewellery type. |
| Owner settings | Manage staff and roles, update shop details and the logo for documents, and configure the default currency and bill numbering. |
| Sales analytics | Compare date ranges and review bill values, items and weight sold, sales by type and karat, available stock, aging stock, and missing-stock alerts. Monetary totals are displayed by currency. |

## Advanced Features

- **Tenant isolation and role-based access.** Authenticated tenant identity scopes shop data. Owner, manager, cashier, and stock keeper roles cover different responsibilities. Archived jewellery requires the owner's password, and sensitive corrections require additional password verification.
- **Transaction-safe sales.** Billing locks selected jewellery records and updates the bill and stock status in one transaction to prevent the same piece from being sold twice. A dedicated per-shop sequence continues across business days.
- **Compact QR compatibility.** New labels encode short, opaque item codes resolved within the authenticated shop. Older signed QR labels are still supported, and PNGs can be regenerated from their stored codes.
- **Controlled daily closing.** The shop must be closed before an audit starts, and new billing is blocked while an audit is open. Repeated runs receive distinct numbers and require owner or manager approval with a reason. Unscanned items stay unresolved until scanned or marked missing during an authorized force-close.
- **Traceable stock corrections.** Stock-critical adjustments capture before-and-after values and a reason. Voids and returns are recorded separately from the original bill line items, while the stock movement ledger records changes to inventory.
- **Decision support.** Owners can compare sales with current availability to identify possible replenishment candidates. The stock-aging view and watchlist help identify pieces worth reviewing before buying more of the same type.

## Stack and Technologies Used

| Layer | Technologies | Role in the project |
| --- | --- | --- |
| Web application | Next.js 15.5 App Router, React 19, TypeScript, Tailwind CSS 3, Radix UI | Dashboard, forms, and server-side API routing |
| Client data and validation | TanStack Query, Axios, Zustand, React Hook Form, Zod | API calls, state, and input validation |
| Analytics and precision | Recharts, decimal.js, Java BigDecimal | Data visualisation and decimal arithmetic for prices and weights |
| API | Java 21, Spring Boot 4.0.5, Spring MVC, Maven, Lombok, MapStruct | REST endpoints, business logic, build tooling, and DTO mapping |
| Authentication | Spring Security, JJWT, BCrypt, jose | API authentication, password verification, and encrypted web session metadata |
| Data | MySQL 8.4, Spring Data JPA, Hibernate, Flyway | Relational storage, tenant-scoped queries, and schema migrations |
| QR and PDF | ZXing, html5-qrcode, OpenPDF | QR image generation, camera scanning, and server-generated documents |
| Local deployment and API tooling | Docker Compose, Spring Boot Actuator, springdoc OpenAPI / Swagger UI | Local service orchestration, health checks, and API documentation |

## Current Scope

WhatsApp has a send stub, and exchanges have a record-keeping scaffold. Bills and audit reports are generated on request. Audit runs retain the expected item list and scan results, while detailed reports also read from linked stock and billing records.

[Project repository](https://github.com/r33j4n/JwellKeeper)
