export type UserRole = "OWNER" | "MANAGER" | "CASHIER" | "STOCK_KEEPER" | "STAFF";
export type JewelleryStatus = "AVAILABLE" | "SOLD" | "MISSING" | "ARCHIVED";
export type StockAuditStatus = "OPEN" | "CLOSED";
export type StockAuditStage = "SCANNING" | "SEARCHING_UNRESOLVED" | "FINALIZED";
export type AuditItemResolution = "PENDING" | "FOUND_IN_AUDIT" | "MARKED_MISSING_ON_CLOSE" | "EXCLUDED_BY_RULE";
export type ShopStatus = "OPEN" | "CLOSED";
export type BillStatus = "ACTIVE" | "VOIDED" | "PARTIALLY_RETURNED" | "RETURNED";
export type AnalyticsRange = "THIS_WEEK" | "THIS_MONTH" | "LAST_3_MONTHS" | "ALL_TIME" | "CUSTOM";

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface AuthUser {
  userId: string;
  tenantId: string;
  role: UserRole;
  email: string;
  name: string;
  shopName?: string;
}

export interface AuthResponse extends AuthUser {
  token?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  shopName: string;
  ownerName: string;
  email: string;
  password: string;
  billPrefix: string;
  defaultCurrencyCode: string;
}

export interface JewelleryType {
  id: string;
  name: string;
  custom: boolean;
}

export interface Jewellery {
  id: string;
  typeId: string;
  typeName: string;
  karat: string;
  designName: string | null;
  weight: string;
  status: JewelleryStatus;
  notes: string | null;
  billId: string | null;
  billNo: string | null;
  createdAt: string;
  soldAt: string | null;
  deletedAt: string | null;
  version: number;
}

export interface QrImageResponse {
  contentType: string;
  qrCodeBase64: string;
}

export interface BillItemRequest {
  jewelleryId: string;
  finalPrice: string;
  ratePerGram?: string | null;
  makingCharge?: string | null;
  discountAmount?: string | null;
  taxAmount?: string | null;
  notes?: string | null;
}

export interface BillItem {
  id: string;
  jewelleryId: string;
  typeNameSnapshot: string;
  designNameSnapshot: string | null;
  karatSnapshot: string;
  weight: string;
  finalPrice: string;
  ratePerGram: string | null;
  makingCharge: string | null;
  discountAmount: string | null;
  taxAmount: string | null;
  notes: string | null;
}

export interface Bill {
  id: string;
  billNo: string;
  billDate: string;
  status: BillStatus;
  currencyCode: string;
  totalAmount: string;
  customerName: string | null;
  customerPhone: string | null;
  customerAddress: string | null;
  paymentMethod: string | null;
  notes: string | null;
  createdBy: string;
  createdAt: string;
  version: number;
  items: BillItem[];
  pdfUrl: string;
}

export interface CreateBillRequest {
  billDate?: string | null;
  currencyCode?: string | null;
  customerName?: string | null;
  customerPhone?: string | null;
  customerAddress?: string | null;
  paymentMethod?: string | null;
  notes?: string | null;
  items: BillItemRequest[];
}

export interface StockAuditItem {
  id: string;
  jewelleryId: string;
  scanned: boolean;
  scannedAt: string | null;
  scannedBy: string | null;
  resolution: AuditItemResolution;
  resolutionChangedAt: string | null;
}

export interface StockAudit {
  id: string;
  auditDate: string;
  runNumber: number;
  auditName: string;
  status: StockAuditStatus;
  stage: StockAuditStage;
  manuallyClosed: boolean;
  closedBy: string | null;
  closedAt: string | null;
  forceClosed: boolean;
  forceClosedBy: string | null;
  forceClosedAt: string | null;
  forceCloseReason: string | null;
  repeatReason: string | null;
  repeatOfAuditId: string | null;
  expectedCount: number;
  expectedTotalWeight: string;
  totalItems: number;
  scannedItems: number;
  missingItems: number;
  items: StockAuditItem[];
  pdfUrl: string;
}

export interface AuditReport {
  auditId: string;
  auditDate: string;
  runNumber: number;
  auditName: string;
  beforeSalesStock: number;
  afterSalesExpectedStock: number;
  scannedItems: number;
  missingItems: number;
  itemsSoldToday: number;
  salesTotalToday: string;
  currentStockItems: AuditReportStockRow[];
  todaySoldItems: AuditReportSoldRow[];
  typeTallies: AuditReportTypeTally[];
  salesTotalsByCurrency: { currencyCode: string; totalAmount: string; itemCount: number }[];
  pdfUrl: string;
}

export interface AuditReportStockRow {
  jewelleryId: string;
  typeName: string;
  designName: string | null;
  karat: string;
  weight: string;
  status: JewelleryStatus | null;
  scanned: boolean;
  resolution: AuditItemResolution;
  createdAt: string | null;
  category: "TODAY_ADDED" | "ALREADY_AVAILABLE" | "MISSING_IN_AUDIT" | string;
}

export interface AuditReportSoldRow {
  jewelleryId: string;
  billNo: string;
  billDate: string;
  typeName: string;
  designName: string | null;
  karat: string;
  weight: string;
  finalPrice: string;
  currencyCode: string;
  notes: string | null;
}

export interface AuditReportTypeTally {
  typeName: string;
  todayAddedCount: number;
  alreadyAvailableCount: number;
  missingCount: number;
  currentStockCount: number;
  soldTodayCount: number;
  currentStockWeight: string;
}

export interface AnalyticsSummary {
  from: string;
  to: string;
  currencyCode: string;
  bestSellingItemType: string | null;
  totalSalesAmount: string;
  previousPeriodSalesAmount: string;
  salesGrowthPercent: string;
  billCount: number;
  averageBillValue: string;
  totalWeightSold: string;
  totalItemsSold: number;
  availableStockCount: number;
  availableStockWeight: string;
  missingStockCount: number;
  missingStockWeight: string;
  mostPopularKarat: string | null;
  averageWeightPerSoldItem: string;
  itemsPerBill: string;
  sellThroughRate: string;
  stockCoverageDays: string;
  salesChart: { date: string; totalAmount: string; itemCount: number }[];
  salesByCurrency: { currencyCode: string; totalAmount: string; billCount: number; averageBillValue: string }[];
  typePerformance: AnalyticsPerformanceRow[];
  karatPerformance: AnalyticsPerformanceRow[];
  inventoryAgeBuckets: { label: string; itemCount: number; totalWeight: string }[];
  slowMovingItems: {
    jewelleryId: string;
    typeName: string;
    designName: string | null;
    karat: string;
    weight: string;
    createdAt: string;
    ageDays: number;
  }[];
  insights: { severity: "GOOD" | "WARNING" | "DANGER" | "ACTION" | "INFO" | string; title: string; message: string; recommendedAction: string }[];
  methodologyNote: string;
}

export interface AnalyticsPerformanceRow {
  name: string;
  itemsSold: number;
  weightSold: string;
  salesAmount: string;
  availableCount: number;
  sellThroughRate: string;
}

export interface TenantSettings {
  defaultCurrencyCode: string;
  billPrefix: string;
  billNumberFormat: string;
  nextBillSequence: number;
  sequenceResetPolicy: string;
}

export interface TenantProfile {
  id: string;
  shopName: string;
  shopAddress: string | null;
  shopContactNumber: string | null;
  shopEmail: string | null;
  taxNumber: string | null;
  receiptFooterNote: string | null;
  logoAvailable: boolean;
  logoUrl: string | null;
}

export interface StaffUser {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  active: boolean;
  createdAt: string;
}

export interface AuditScanResponse {
  audit: StockAudit;
  item: StockAuditItem;
  alreadyScanned: boolean;
  message: string;
}

export interface AuditAttemptCloseResponse {
  audit: StockAudit;
  canCloseCleanly: boolean;
  unresolvedItems: StockAuditItem[];
}

export interface ShopStateResponse {
  id: string | null;
  businessDate: string;
  status: ShopStatus;
  openedBy: string | null;
  openedAt: string | null;
  closedBy: string | null;
  closedAt: string | null;
  version: number;
}

export interface JewelleryImage {
  id: string;
  jewelleryId: string;
  captureSource: "CAMERA" | "UPLOAD";
  mimeType: string;
  fileSizeBytes: number;
  width: number | null;
  height: number | null;
  checksumSha256: string;
  sortOrder: number;
  primary: boolean;
  url: string;
  createdAt: string;
}

export interface StockAdjustment {
  id: string;
  jewelleryId: string;
  beforeTypeId: string | null;
  afterTypeId: string | null;
  beforeKarat: string | null;
  afterKarat: string | null;
  beforeWeight: string | null;
  afterWeight: string | null;
  beforeDesignName: string | null;
  afterDesignName: string | null;
  beforeNotes: string | null;
  afterNotes: string | null;
  beforeStatus: JewelleryStatus | null;
  afterStatus: JewelleryStatus | null;
  reason: string;
  createdBy: string;
  createdAt: string;
}
