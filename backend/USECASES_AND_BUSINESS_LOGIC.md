# JwellKeeper Use Cases And Business Logic

This document describes the current intended business use cases, workflow logic, edge cases, and real-world validation points for the JwellKeeper Jewellery Stock Management System.

The goal is to make the domain rules easy to review before the application becomes stricter in production. Some sections describe the current implemented behavior. Other sections explicitly call out logic that should be validated with shop owners, accountants, and staff before finalizing.

## 1. System Purpose

JwellKeeper is a multi-tenant jewellery stock, billing, QR tracking, daily audit, and analytics system.

Each tenant represents one jewellery shop/business. Users under one tenant must never access another tenant's data.

Primary goals:

- Track each jewellery item individually.
- Generate and print QR codes for each jewellery item.
- Bill jewellery with dynamic prices at sale time.
- Mark sold jewellery as unavailable.
- Run daily stock audits by scanning QR codes.
- Detect missing jewellery.
- Allow controlled manual closure of audits with password confirmation.
- Persist business logs for traceability and debugging.
- Provide sales and stock analytics.

## 2. Core Domain Concepts

### 2.1 Tenant

A tenant is a shop/business account.

Current logic:

- A tenant is created during owner registration.
- Every tenant-owned table includes `tenant_id`.
- Tenant context is extracted from JWT/session, not from request body.
- All business queries are scoped by tenant.

Important validation:

- Decide whether one physical business with multiple branches should be one tenant with branches, or separate tenants.
- If branches are needed later, add `branch_id` instead of overloading tenant.

### 2.2 User

Users belong to a tenant.

Roles:

- `OWNER`
- `STAFF`

Current role logic:

- OWNER can perform all operations.
- STAFF can manage operational stock, bills, audits, reports, and analytics.
- OWNER-only operations include staff management, tenant billing settings, restoring missing jewellery, and manual sensitive actions.

Validation questions:

- Should STAFF be allowed to create jewellery?
- Should STAFF be allowed to edit jewellery after it is added?
- Should STAFF be allowed to delete jewellery?
- Should STAFF be allowed to start repeat audits?
- Should repeat audit password be the logged-in user's password or only OWNER password?

Current implementation note:

- Starting a second or later audit for the same date validates the logged-in user's password.
- Closing an audit with missing items validates OWNER role/password.

### 2.3 Jewellery Type

Jewellery Type is the high-level category.

Examples:

- Bangle
- Necklace
- Ring
- Chain
- Bracelet
- Earrings

Current logic:

- Types are tenant-specific.
- Tenant can create custom types.
- Types are used for filtering and analytics.

Validation questions:

- Should there be global default types plus tenant custom types?
- Should type names be case-insensitive unique per tenant?
- Should deleted/unused types be archived instead of removed?

### 2.4 Jewellery Item

Jewellery Item is an individually trackable stock unit.

Current fields:

- `id`
- `tenant_id`
- `type_id`
- `karat`
- `design_name`
- `weight`
- `status`
- `qr_payload_token`
- `notes`
- `created_at`
- `sold_at`
- `deleted_at`
- `version`

Current statuses:

- `AVAILABLE`
- `SOLD`
- `MISSING`

Lifecycle deletion:

- Soft delete uses `deleted_at`.
- There is no `DELETED` business status.

Business meaning:

- `AVAILABLE`: item is physically expected in shop stock and can be sold.
- `SOLD`: item has been billed and should not be available for future billing.
- `MISSING`: item was expected during audit but not scanned and was manually closed as missing.
- `deleted_at`: item is removed from active stock listings, usually due to data correction or administrative removal.

Important validation:

- `MISSING` does not necessarily mean stolen. It means "not scanned during audit and accepted as unresolved."
- If missing items are later found, OWNER can restore them to `AVAILABLE`.
- A found item should create a business log entry.
- Consider whether restored missing items should require a note/reason.

## 3. Authentication Use Cases

### UC-AUTH-001: Register Owner And Tenant

Actor:

- New shop owner

Input:

- Shop name
- Owner name
- Email
- Password
- Bill prefix
- Default currency code

Flow:

1. Owner submits registration.
2. System creates tenant.
3. System creates tenant settings.
4. System creates owner user with BCrypt password hash.
5. System returns authenticated session/JWT.
6. Business log is persisted.

Rules:

- Email must be unique.
- Password is never stored as plain text.
- Tenant settings are created during registration.
- Default currency must be ISO-4217 style, for example `LKR`, `CAD`, `EUR`, `AUD`.

Validation questions:

- Should the system verify email before allowing production usage?
- Should shop name be unique?
- Should password policy require symbols/numbers?

### UC-AUTH-002: Login

Actor:

- OWNER or STAFF

Flow:

1. User enters email and password.
2. System validates credentials.
3. System verifies user is active.
4. System returns JWT/session metadata.
5. Business log is persisted.

Rules:

- JWT contains user id, tenant id, role, and email.
- Tenant id from JWT is authoritative.
- Inactive users cannot login.

Validation questions:

- Should failed login attempts be logged?
- Should too many failed attempts lock the account?

### UC-AUTH-003: Logout

Actor:

- Logged-in user

Flow:

1. User clicks logout.
2. Frontend clears HTTP-only auth cookies through BFF route.
3. User is redirected to login.

Validation questions:

- Should JWTs be revocable server-side?
- Should logout create a business log?

## 4. Staff Management Use Cases

### UC-USER-001: Create Staff User

Actor:

- OWNER

Flow:

1. OWNER opens staff settings.
2. OWNER enters staff name, email, and password.
3. System creates active STAFF user under the same tenant.
4. Business log is persisted.

Rules:

- STAFF belongs to current tenant.
- STAFF cannot be created for another tenant.

Validation questions:

- Should STAFF be forced to change password on first login?
- Should staff permissions be more granular than OWNER/STAFF?

### UC-USER-002: Deactivate Staff

Actor:

- OWNER

Flow:

1. OWNER deactivates staff.
2. Staff can no longer login.
3. Existing historical records remain linked to that staff user id.
4. Business log is persisted.

Validation questions:

- Should staff be reactivatable?
- Should staff deletion be blocked if they created bills?

## 5. Tenant Settings Use Cases

### UC-SETTINGS-001: Update Billing Settings

Actor:

- OWNER

Settings:

- Default currency code
- Bill prefix
- Bill number format
- Next bill sequence
- Sequence reset policy

Current logic:

- Bill numbers use a tenant-level sequence.
- Sequence continues across days.
- Today's bill number continues from yesterday's number.
- Bill number does not reset daily.

Example:

- `TJ-000001`
- `TJ-000002`
- `TJ-000003`

Validation questions:

- Should bill sequence ever reset yearly/monthly?
- Should bill number edits be blocked after first bill?
- Should changing `nextBillSequence` require password confirmation?
- Should each tenant have multiple bill series?

## 6. Jewellery Stock Use Cases

### UC-JEW-001: Create Jewellery Type

Actor:

- OWNER or STAFF, depending on policy

Flow:

1. User enters type name.
2. System creates tenant-specific type.
3. Type becomes available in add-jewellery form.
4. Business log may be persisted.

Validation questions:

- Should only OWNER create types?
- Should STAFF request a type instead of creating it directly?

### UC-JEW-002: Add New Jewellery Item

Actor:

- OWNER or STAFF, depending on policy

Input:

- Type
- Sub-type / Design Name
- Karat
- Weight
- Notes

Current frontend note:

- Added date is not entered manually.
- `created_at` is generated internally.

Flow:

1. User selects type.
2. User enters design name, karat, weight, and optional notes.
3. System creates jewellery item as `AVAILABLE`.
4. System creates stable signed QR payload token.
5. System stores QR token, not QR image.
6. QR image can be generated on demand.
7. Business log is persisted.

Rules:

- Weight supports 3 decimal places.
- Karat must look like `24K`, `22K`, `18K`, etc.
- QR image is not stored in MySQL.
- QR code can be printed/downloaded.

Validation questions:

- Should item code/SKU be human readable in addition to UUID?
- Should item photos be added?
- Should weight be gross weight, net gold weight, or both?
- Should stone weight be separate?
- Should wastage/making details be stored on stock item or only bill item?

### UC-JEW-003: View Jewellery List

Actor:

- OWNER or STAFF

Current filters/search:

- Status
- Type
- Karat
- Search text

Search currently covers:

- Type name
- Sub-type / design name
- Notes
- Karat
- Related bill number for sold items

Displayed columns:

- Type
- Sub-type
- Karat
- Weight
- Status
- Bill no
- Added date
- Sold date
- Actions

Rules:

- Only current tenant data appears.
- Soft-deleted jewellery is hidden.
- Pagination is applied.

Validation questions:

- Should there be barcode/QR token search?
- Should missing items be visually separated from available stock?
- Should sold items be hidden by default?

### UC-JEW-004: View Jewellery Details

Actor:

- OWNER or STAFF

Flow:

1. User opens item detail page.
2. System shows item fields and status.
3. System regenerates QR image from stored token.
4. User can download or print QR.

Rules:

- QR token maps to jewellery id and tenant id.
- QR resolution verifies tenant.

Validation questions:

- Should QR code show item details directly or only signed token?
- Current safer approach is token-only QR; resolving token returns current details.
- If QR includes full details, the code may expose private inventory details to anyone who scans it.

### UC-JEW-005: Edit Jewellery

Actor:

- OWNER or STAFF, depending on policy

Current rules:

- SOLD jewellery cannot be edited.
- Jewellery updates use row locking/versioning.
- Updates are tenant-scoped.

Validation questions:

- Should AVAILABLE item edits require reason?
- Should weight edits be logged with before/after values?
- Should MISSING item edits be blocked until found?

### UC-JEW-006: Soft Delete Jewellery

Actor:

- OWNER or STAFF, depending on policy

Current rules:

- SOLD jewellery cannot be deleted.
- Delete sets `deleted_at`.
- Business status remains separate from deletion.

Validation questions:

- Should STAFF be allowed to delete stock?
- Should deletion require OWNER password?
- Should deletion require notes/reason?
- Should deleted items still appear in audit history and old bills? They should.

### UC-JEW-007: Mark Missing Jewellery Found

Actor:

- OWNER

Flow:

1. OWNER opens a MISSING jewellery item.
2. OWNER enters password.
3. System validates password.
4. Item status changes from `MISSING` to `AVAILABLE`.
5. Business log is persisted.

Rules:

- Only MISSING items can be restored.
- SOLD items cannot be restored through this flow.

Validation questions:

- Should found item create an adjustment report?
- Should owner enter where/how it was found?
- Should restored item be included in the next audit automatically? Yes, because it becomes AVAILABLE.

## 7. QR Code Use Cases

### UC-QR-001: Generate QR For Jewellery

Actor:

- OWNER or STAFF

Flow:

1. Jewellery is created.
2. System creates signed QR payload token.
3. Token is stored in DB.
4. PNG/Base64 image is generated on demand.
5. User prints/downloads QR label.

QR content meaning:

- Encoded signed token contains enough information to resolve:
  - `jewelleryId`
  - `tenantId`

Rules:

- QR image itself is not stored.
- QR token is stable for that jewellery item.
- Tenant mismatch is rejected.

Validation questions:

- Should QR labels include readable text under the code?
- Should QR tokens expire? For physical jewellery labels, normally no.
- Should QR token rotate when item is sold/deleted? Usually no; history should still resolve.

### UC-QR-002: Resolve QR

Actor:

- OWNER or STAFF

Flow:

1. User scans QR.
2. System verifies signature.
3. System checks tenant id in token equals current user tenant id.
4. System returns current jewellery details.

Possible errors:

- Invalid QR
- Tampered QR
- Tenant mismatch
- Jewellery deleted/not found
- Jewellery not part of active audit

## 8. Billing Use Cases

### UC-BILL-001: Create Bill

Actor:

- OWNER or STAFF

Input:

- Bill date
- Currency code
- Customer name
- Customer phone
- Customer address
- Payment method
- Notes
- One or more jewellery items
- Dynamic final price per item
- Optional price breakdown fields:
  - Rate per gram
  - Making charge
  - Discount amount
  - Tax amount
  - Item notes

Flow:

1. User opens create bill.
2. System lists AVAILABLE jewellery only.
3. User selects item(s).
4. User enters final price per item.
5. Frontend calculates total using decimal arithmetic.
6. User submits bill.
7. Backend locks each jewellery row.
8. Backend verifies each item is still AVAILABLE.
9. Backend assigns next tenant bill number.
10. Backend snapshots item type, design name, karat, and weight into bill item.
11. Backend stores final price and optional breakdown in bill item.
12. Backend marks jewellery as SOLD and sets sold time.
13. Backend saves bill and business log.

Rules:

- Price is not stored on Jewellery.
- Price is stored only on BillItem.
- Bill number is unique per tenant.
- Bill sequence continues across days.
- Same jewellery item cannot be billed twice in one bill.
- Already SOLD/MISSING/deleted jewellery cannot be billed.
- Multi-currency is supported by storing `currencyCode` per bill.

Important validation:

- Current bill total is sum of `finalPrice`.
- Rate/making/discount/tax are informational unless business decides they should calculate final price.
- Need decide whether backend should verify:
  - finalPrice = ratePerGram * weight + makingCharge + tax - discount
  - or allow finalPrice as the authoritative negotiated amount.

Recommended real-world rule:

- Keep `finalPrice` authoritative for v1.
- Treat breakdown fields as optional explanation only.
- Add a future setting if strict calculation is required.

### UC-BILL-002: View Bill

Actor:

- OWNER or STAFF

Flow:

1. User opens bill detail.
2. System shows bill metadata and items.
3. User can view PDF.
4. User can download PDF.
5. User can call WhatsApp send stub.

Rules:

- Bill item snapshots are shown, not current Jewellery values.
- Historical bill must remain stable even if item type name changes later.

Validation questions:

- Should bill be editable after creation?
- Should bills be cancellable?
- If a bill is cancelled, should jewellery become AVAILABLE again?
- Should cancellation require OWNER password?

### UC-BILL-003: Search Bills

Actor:

- OWNER or STAFF

Current search:

- Customer name
- Customer phone
- Notes
- Partial bill number

Current filters:

- Date from
- Date to

Rules:

- Search is tenant-scoped.
- Pagination is applied.

Validation questions:

- Should search include item type/design?
- Should search include currency/payment method?
- Should there be daily sales report separate from bill search?

### UC-BILL-004: Download/View Bill PDF

Actor:

- OWNER or STAFF

Flow:

1. User clicks View PDF or Download PDF.
2. Backend generates PDF from bill data.
3. Business log records PDF generation.

PDF includes:

- Bill number
- Bill date
- Currency
- Customer details
- Item table
- Type
- Sub-type/design
- Karat
- Weight
- Price
- Notes
- Total
- Footer credit

Validation questions:

- Should PDF include tenant shop name/address?
- Should PDF include tax registration number?
- Should PDF include staff/created-by name?
- Should PDF include QR/reference number?

### UC-BILL-005: WhatsApp Send Stub

Actor:

- OWNER or STAFF

Current logic:

- Endpoint logs the attempt.
- It does not send a real WhatsApp message yet.

Validation questions:

- Which WhatsApp provider will be used?
- Should customer consent be stored?
- Should WhatsApp delivery status be tracked?

## 9. Daily Stock Audit Use Cases

Daily stock audit is the most important workflow to validate carefully.

### 9.1 Audit Meaning

An audit is a snapshot of expected AVAILABLE stock at the moment the audit starts.

Current logic:

- When audit starts, the system snapshots all currently AVAILABLE jewellery into `stock_audit_items`.
- During scan, each expected item is marked scanned.
- At close, unscanned items are considered missing for that audit.

Important real-world point:

- The timing of audit start matters.
- If staff starts audit before all sales are done, later sales may affect expectations.
- If audit is meant for shop closing, it should be started after billing is complete for the day.

Recommended operational rule:

- Start audit only when the shop is ready to count closing stock.
- If sales happen after audit starts, the business must decide whether:
  - block sales during open audit,
  - allow sales and update audit snapshot,
  - or require restarting/re-running audit.

### UC-AUDIT-001: Start First Audit Of The Day

Actor:

- OWNER or STAFF

Input:

- Audit date, defaults to today

Flow:

1. User selects date.
2. User clicks Start audit.
3. System calculates next run number for the date.
4. If no previous audit exists for the date, run number is 1.
5. System creates audit as `OPEN`.
6. System snapshots all AVAILABLE stock.
7. Business log is persisted with audit name.

Audit name:

- `2026-04-17 Audit #1`

Rules:

- First audit of a date does not require password.
- Audit is tenant-scoped.
- Audit snapshot uses current AVAILABLE jewellery.

Validation questions:

- Should STAFF be allowed to start audit?
- Should audit start be blocked if another audit is already OPEN?
- Current implementation allows multiple audits, but if one audit is still OPEN, business may want to block new audit or warn strongly.

### UC-AUDIT-002: Start Second/Third Audit Of Same Day

Actor:

- OWNER or STAFF, depending on policy

Current logic:

- If one or more audits already exist for the same date, the next audit gets a higher run number.
- Starting repeat audit requires password.
- Password currently validates the logged-in user.

Example:

- First audit: `2026-04-17 Audit #1`
- Second audit: `2026-04-17 Audit #2`
- Third audit: `2026-04-17 Audit #3`

Frontend behavior:

- The Start button changes to `Start Audit #2`, `Start Audit #3`, etc.
- A warning is displayed explaining that another audit already exists for that date.
- A Radix modal asks for password confirmation.
- On success, toast displays the audit name.

Business log metadata includes:

- `auditDate`
- `runNumber`
- `auditName`
- `items`
- `repeatAudit`

Validation questions:

- Should repeat audit require OWNER password only?
- Should repeat audit require a reason/note?
- Should repeat audit be allowed if previous audit is OPEN?
- Should repeat audit compare against previous audit?
- Should repeat audit resolve previous missing items automatically? Usually no.

Recommended stricter production rule:

- If same-day repeat audit is started:
  - require OWNER password,
  - require reason,
  - keep all audits separate,
  - do not auto-resolve previous missing items.

### UC-AUDIT-003: Scan Audit Item

Actor:

- OWNER or STAFF

Flow:

1. User opens Scan QR page.
2. User selects open audit.
3. User scans QR or enters token manually.
4. Backend validates QR.
5. Backend verifies item is part of selected audit.
6. Backend marks item scanned.
7. Backend sets resolution `FOUND`.
8. Business log is persisted.

Rules:

- Closed audit cannot be scanned.
- QR from another tenant is rejected.
- Jewellery not in audit snapshot is rejected.

Validation questions:

- Should scanning a SOLD item be allowed if it was in audit snapshot?
- Ideally SOLD after audit start should be handled by a clear rule.
- Should duplicate scan show "already scanned" instead of success?
- Should scan page show details of scanned item?

### UC-AUDIT-004: Close Audit With No Missing Items

Actor:

- OWNER or STAFF

Flow:

1. User clicks close.
2. System sees all items scanned.
3. Audit status becomes CLOSED.
4. `closed_by` and `closed_at` are set.
5. Business log is persisted.

Rules:

- No password required when nothing is missing.

Validation questions:

- Should STAFF be allowed to close clean audit?
- Should close require confirmation even when clean?

### UC-AUDIT-005: Close Audit With Missing Items

Actor:

- OWNER

Flow:

1. User clicks close.
2. System detects unscanned items.
3. Frontend asks for owner password.
4. Backend verifies OWNER role and password.
5. Audit is marked CLOSED.
6. Audit is marked manually closed.
7. Unscanned audit items get resolution `MANUALLY_CLOSED_MISSING`.
8. Corresponding AVAILABLE jewellery become `MISSING`.
9. Business log is persisted.

Rules:

- Missing closure requires OWNER.
- Missing closure requires password.
- Missing closure does not delete jewellery.
- MISSING jewellery cannot be billed.

Validation questions:

- Should missing closure require a reason/comment?
- Should missing jewellery trigger alert notifications?
- Should missing item status change immediately or only after manager approval?

### UC-AUDIT-006: Audit Report PDF

Actor:

- OWNER or STAFF

Current report includes:

- Audit name
- Audit date
- Run number
- Status
- Before sales stock
- After sales expected stock
- Scanned count
- Missing count
- Items sold today
- Sales total today
- Manual closure status
- Missing item table
- Footer credit

Current calculation:

- `afterSalesExpectedStock` = size of audit snapshot.
- `itemsSoldToday` = bill item count for that date.
- `beforeSalesStock` = afterSalesExpectedStock + itemsSoldToday.

Important validation:

- This formula assumes audit snapshot was taken after daily sales.
- If audit starts before all sales, report can be misleading.

Recommended improvement:

- Store audit snapshot moment and calculate:
  - stock at day start,
  - sold before audit start,
  - sold after audit start,
  - expected at audit start,
  - expected at close.

### UC-AUDIT-007: Multiple Audits Same Day Reporting

Actor:

- OWNER or STAFF

Current logic:

- Each audit has separate id and run number.
- Reports are generated per audit.
- Business logs identify each audit by id, date, and run number.

Validation questions:

- Should daily report combine all audits of the day?
- Should dashboard show only latest audit for the day?
- Should same-day Audit #2 replace Audit #1 operationally, or remain separate?

Recommended rule:

- Keep all audits as immutable historical events.
- Treat the latest CLOSED audit as current operational truth for that day.
- Keep earlier audits for investigation.

## 10. Missing Item Use Cases

### UC-MISSING-001: Item Becomes Missing

Trigger:

- Audit is closed with unscanned items and owner password.

Flow:

1. Audit close detects unscanned items.
2. Missing audit item resolution is set.
3. Jewellery status changes to `MISSING`.
4. Logs are persisted.

Business meaning:

- Item was expected but not found/scanned during closing audit.

Validation questions:

- Should all unscanned items become MISSING immediately?
- Should there be a pending state like `UNRESOLVED` before marking MISSING?
- Should manager review happen before status changes?

### UC-MISSING-002: Missing Item Found Later

Actor:

- OWNER

Flow:

1. OWNER opens missing item.
2. OWNER enters password.
3. Item is restored to AVAILABLE.
4. Business log is persisted.

Validation questions:

- Should found item be linked back to the audit where it was missing?
- Should found event include notes?
- Should found item require physical re-scan?

## 11. Analytics Use Cases

### UC-ANALYTICS-001: Summary

Actor:

- OWNER or STAFF

Filters:

- This week
- This month
- Last 3 months
- All time
- Custom date range

Metrics:

- Best selling item type
- Total weight sold
- Total items sold
- Available stock count
- Most popular current-stock karat
- Average weight per sold item
- Sales chart data

Rules:

- Analytics are tenant-scoped.
- Sold metrics are based on bill items and bill dates.
- Available stock count is based on jewellery status.

Validation questions:

- Should cancelled/returned bills affect analytics? Currently no cancellation flow exists.
- Should analytics include currency conversion? Currently no.
- If multi-currency bills exist, total sales across currencies should not be summed without conversion.

Important multi-currency warning:

- Counts and weights are safe across currencies.
- Money totals across currencies are not safe unless grouped by currency or converted using exchange rates.

## 12. Business Logs Use Cases

### UC-LOG-001: Persist Business Event

Current log fields:

- Tenant id
- Actor user id
- Action
- Entity type
- Entity id
- Result
- Message
- Metadata JSON
- IP address
- User agent
- Created at

Logged actions include:

- Login/register
- Staff changes
- Jewellery create/update/delete/found
- Bill creation
- Bill PDF generation
- WhatsApp stub
- Audit start/scan/close
- Audit PDF generation

Repeat audit log metadata:

- `auditDate`
- `runNumber`
- `auditName`
- `repeatAudit`
- `items`

Security rules:

- Do not log passwords.
- Do not log JWT tokens.
- Do not log sensitive raw request bodies.

Validation questions:

- Should logs be visible in admin UI?
- How long should logs be retained?
- Should logs be immutable?
- Should log metadata include before/after values for stock edits?

Naming note:

- The database table is `business_logs`.
- Instead of creating separate tables such as `logs2`, repeat audit identity is stored through `runNumber`, `auditName`, and audit id.
- This is safer because all audit logs stay queryable in one table.

## 13. PDF Use Cases

### UC-PDF-001: Generate Bill PDF

Actor:

- OWNER or STAFF

Rules:

- PDF is generated from persisted bill data.
- PDF generation is logged.
- Download and view use same backend PDF endpoint.

Validation questions:

- Should PDF be stored after generation for exact historical copy?
- Current implementation regenerates PDF dynamically.
- If templates change later, old bills may render differently unless stored.

Recommended production rule:

- For legal invoices, store generated PDF or store template version.

### UC-PDF-002: Generate Audit PDF

Actor:

- OWNER or STAFF

Rules:

- PDF is generated from audit and report data.
- Includes audit run number.
- PDF generation is logged.

Validation questions:

- Should audit report include full scanned item list, not only missing items?
- Should it include staff name?
- Should it include signatures?

## 14. Search Use Cases

### UC-SEARCH-001: Jewellery Search

Current searchable fields:

- Type name
- Design name
- Notes
- Karat
- Related bill number

Validation questions:

- Should search include UUID/short code?
- Should search include customer name for sold items?
- Should search include QR token? Usually no.

### UC-SEARCH-002: Billing Search

Current searchable fields:

- Bill number
- Customer name
- Customer phone
- Notes

Validation questions:

- Should search include jewellery type/design sold in bill?
- Should search include payment method?
- Should search include amount range?

## 15. Concurrency And Data Integrity Logic

### 15.1 Double Selling Prevention

Current logic:

- Bill creation locks each selected jewellery row.
- Backend verifies item is AVAILABLE at billing time.
- If another bill sold it first, billing fails.

Validation questions:

- Should UI auto-refresh available item list after a billing conflict?
- Should staff see who sold the item?

### 15.2 Optimistic Locking

Important entities have version fields:

- Jewellery
- Bill
- StockAudit
- StockAuditItem

Purpose:

- Detect conflicting edits or state transitions.

Validation questions:

- Should frontend send version for update operations?
- Current backend relies mostly on locking for critical flows.

### 15.3 Tenant Isolation

Current logic:

- Tenant id is pulled from JWT/session.
- Repository/service queries include tenant id.
- QR resolution checks tenant id from token.

Validation questions:

- Add integration tests proving cross-tenant reads/writes fail.

## 16. Currency And Precision Logic

Current frontend rules:

- Weight stored as string in form.
- Weight allows up to 3 decimals.
- Money allows up to 2 decimals in form.
- Decimal.js is used for frontend billing totals.

Current backend rules:

- Money stored as BigDecimal.
- Currency stored per bill.
- No hard-coded LKR.

Important validation:

- Backend DB fields currently allow 4 decimal places for money.
- Frontend restricts entry to 2 decimal places.
- This is acceptable if all supported currencies use 2 decimals.
- Some currencies have 0 or 3 minor units; decide if ISO minor-unit precision matters.

Multi-currency warning:

- Do not sum money across currencies unless grouped by currency or converted.

## 17. Status Transition Rules

### Jewellery Status Transitions

Allowed:

- AVAILABLE -> SOLD during billing.
- AVAILABLE -> MISSING during audit close with missing items.
- MISSING -> AVAILABLE through owner mark-found.

Blocked or not supported:

- SOLD -> AVAILABLE, unless a future bill cancellation/return flow exists.
- SOLD -> MISSING.
- MISSING -> SOLD.
- Any status -> DELETED status, because deletion is `deleted_at`.

Validation questions:

- Should returns exist?
- Should exchanges exist?
- Should repair/temporary-out statuses exist?
- Should items sent to workshop be separate from MISSING?

### Audit Status Transitions

Allowed:

- OPEN -> CLOSED.

Blocked:

- CLOSED -> OPEN.

Validation questions:

- Should audit reopening be allowed by OWNER?
- If reopening exists, logs must preserve original close and reopen events.

## 18. Real-World Logic Risks To Validate

These are the highest-risk business logic areas.

### 18.1 Audit Timing Risk

Question:

- Is audit started before or after all sales are completed?

Why it matters:

- Current before/after sales report assumes audit snapshot is after sales.

Recommended decision:

- Define "Start Audit" as closing-time action after billing is complete.
- If not possible, add logic for sales during an open audit.

### 18.2 Repeat Audit Meaning

Question:

- What does Audit #2 mean?

Possible meanings:

- Correction/recount after Audit #1.
- Separate shift audit.
- Second physical check due to missing items.

Recommended decision:

- Treat Audit #2 as a separate historical event.
- Do not overwrite Audit #1.
- Latest closed audit can be used as operational latest status.

### 18.3 Missing Item Status

Question:

- Should unscanned items immediately become MISSING?

Current behavior:

- Yes, after OWNER manual close.

Alternative:

- Add `UNRESOLVED` audit resolution and require final manager approval before marking jewellery MISSING.

### 18.4 Sales During Audit

Question:

- Can staff create bills while audit is OPEN?

Current behavior:

- Billing is not blocked by open audit.

Risk:

- Audit expected stock may become inaccurate.

Possible policies:

- Block billing during open audit.
- Allow billing and auto-mark audit item as sold/not required.
- Warn staff and require audit restart.

Recommended for v1:

- Operationally instruct staff to start audit after final sale.
- Later add backend guard or setting.

### 18.5 Bill Cancellation Or Returns

Question:

- Can a bill be cancelled?

Current behavior:

- No cancellation flow.

Risk:

- Mistaken bills leave jewellery SOLD forever unless manually corrected in DB.

Recommended future use cases:

- Cancel bill
- Return item
- Exchange item
- Refund
- Reopen stock item with full audit log

### 18.6 PDF Legal Validity

Question:

- Is generated PDF a legal invoice?

Current behavior:

- PDF is dynamically generated.

Risk:

- If template changes, old PDF output changes.

Recommended future rule:

- Store generated invoice PDF or template version.

### 18.7 Multi-Currency Analytics

Question:

- Should analytics total money across currencies?

Current behavior:

- Bill currency is stored, but high-level analytics may need careful currency grouping.

Recommended rule:

- Sales count and weight can be combined.
- Money totals should be grouped by currency.

## 19. Recommended Additional Use Cases For Future

### Stock Adjustment

Use when:

- Weight correction
- Wrong type entered
- QR damaged and reprinted
- Manual stock correction

Recommended fields:

- Reason
- Before values
- After values
- Approved by

### Bill Cancellation

Use when:

- Wrong bill created
- Customer cancels before leaving
- Payment failed

Recommended logic:

- OWNER-only.
- Password required.
- Jewellery status returns to AVAILABLE only if item physically returned.
- Cancellation log required.

### Return Or Exchange

Use when:

- Customer returns sold item.
- Customer exchanges item.

Recommended logic:

- Separate from bill cancellation.
- Preserve original bill.
- Create return document.

### Damage Or Repair Status

Possible statuses:

- REPAIR
- DAMAGED
- RESERVED
- TRANSFERRED

Validation:

- Add only if real shop process needs them.

### Stock Opening Balance

Use when:

- Initial import of stock.
- Daily opening stock report.

Recommended logic:

- Store opening stock count/weight per day.

## 20. Current API Logic Summary

### Auth

- `POST /api/auth/register`
- `POST /api/auth/login`

### Jewellery

- `POST /api/jewellery/types`
- `GET /api/jewellery/types`
- `POST /api/jewellery`
- `GET /api/jewellery`
- `GET /api/jewellery/{id}`
- `PUT /api/jewellery/{id}`
- `DELETE /api/jewellery/{id}`
- `GET /api/jewellery/{id}/qr`
- `POST /api/jewellery/qr/resolve`
- `POST /api/jewellery/{id}/mark-found`

### Billing

- `POST /api/bill`
- `GET /api/bill`
- `GET /api/bill/{id}`
- `GET /api/bill/{id}/pdf`
- `POST /api/bill/{id}/whatsapp`

### Audit

- `POST /api/audit/start`
- `POST /api/audit/scan`
- `POST /api/audit/close`
- `GET /api/audit/report`
- `GET /api/audit/{id}/pdf`

### Analytics

- `GET /api/analytics/summary`

### Staff

- `GET /api/users/staff`
- `POST /api/users/staff`
- `PUT /api/users/staff/{id}`
- `DELETE /api/users/staff/{id}`

### Tenant Settings

- `GET /api/tenant/settings`
- `PUT /api/tenant/settings`

## 21. Review Checklist

Use this checklist to validate the app with real users.

- Confirm whether staff can add/edit/delete jewellery.
- Confirm whether repeat audit requires OWNER password or current user password.
- Confirm whether multiple OPEN audits should be allowed.
- Confirm whether sales are allowed during OPEN audit.
- Confirm whether missing items become MISSING immediately or after approval.
- Confirm whether mark-found requires note/reason.
- Confirm whether bill cancellation is required for v1.
- Confirm whether return/exchange flow is required.
- Confirm bill number format and reset policy.
- Confirm whether PDFs need legal invoice fields.
- Confirm whether generated PDFs must be stored.
- Confirm whether analytics should group money totals by currency.
- Confirm whether stock reports need weight totals by karat/type.
- Confirm whether QR should expose details or only secure token.
- Confirm whether audit report should list all scanned items, not only missing.
- Confirm whether daily report should combine multiple audits or show latest audit only.

## 22. Recommended Immediate Fixes Before Production

These are recommended after business validation:

- Add reason field for repeat audit start.
- Decide if repeat audit must be OWNER-only.
- Add backend rule for billing during OPEN audit.
- Add duplicate scan response such as "Already scanned".
- Add bill cancellation or correction workflow.
- Add audit report full item list.
- Group analytics sales totals by currency.
- Add UI page for business logs.
- Add integration tests for tenant isolation and audit edge cases.

