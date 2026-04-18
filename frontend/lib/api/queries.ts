"use client";

import { apiClient, authClient } from "@/lib/api/client";
import type {
  AnalyticsRange,
  AnalyticsSummary,
  AuditAttemptCloseResponse,
  AuditReport,
  AuditScanResponse,
  AuthResponse,
  Bill,
  CreateBillRequest,
  Jewellery,
  JewelleryImage,
  JewelleryStatus,
  JewelleryType,
  LoginRequest,
  PageResponse,
  QrImageResponse,
  RegisterRequest,
  StaffUser,
  ShopStateResponse,
  StockAdjustment,
  StockAudit,
  TenantProfile,
  TenantSettings,
  UserRole,
} from "@/lib/api/types";

export const authApi = {
  login: (payload: LoginRequest) => authClient.post<AuthResponse>("/login", payload).then((r) => r.data),
  register: (payload: RegisterRequest) => authClient.post<AuthResponse>("/register", payload).then((r) => r.data),
  logout: () => authClient.post("/logout").then((r) => r.data),
  session: () => authClient.get<AuthResponse | null>("/session").then((r) => r.data),
};

export const jewelleryApi = {
  types: () => apiClient.get<JewelleryType[]>("/jewellery/types").then((r) => r.data),
  createType: (name: string) => apiClient.post<JewelleryType>("/jewellery/types", { name }).then((r) => r.data),
  list: (params: { page?: number; size?: number; status?: JewelleryStatus | ""; typeId?: string; karat?: string; q?: string }) =>
    apiClient.get<PageResponse<Jewellery>>("/jewellery", { params }).then((r) => r.data),
  archived: (payload: { ownerPassword: string; typeId?: string; karat?: string; q?: string }, params: { page?: number; size?: number }) =>
    apiClient.post<PageResponse<Jewellery>>("/jewellery/archived/search", payload, { params }).then((r) => r.data),
  get: (id: string) => apiClient.get<Jewellery>(`/jewellery/${id}`).then((r) => r.data),
  create: (payload: { typeId: string; karat: string; designName?: string | null; weight: string; notes?: string | null }) =>
    apiClient.post<Jewellery>("/jewellery", payload).then((r) => r.data),
  update: (id: string, payload: Partial<{ typeId: string; karat: string; designName: string | null; weight: string; notes: string | null }>) =>
    apiClient.put<Jewellery>(`/jewellery/${id}`, payload).then((r) => r.data),
  remove: (id: string) => apiClient.delete(`/jewellery/${id}`).then((r) => r.data),
  qr: (id: string) => apiClient.get<QrImageResponse>(`/jewellery/${id}/qr`).then((r) => r.data),
  resolveQr: (token: string) => apiClient.post<Jewellery>("/jewellery/qr/resolve", { token }).then((r) => r.data),
  markFound: (id: string, ownerPassword: string) =>
    apiClient.post<Jewellery>(`/jewellery/${id}/mark-found`, { ownerPassword }).then((r) => r.data),
  images: (id: string) => apiClient.get<JewelleryImage[]>(`/jewellery/${id}/images`).then((r) => r.data),
  uploadImage: (id: string, file: File, captureSource: "CAMERA" | "UPLOAD" = "UPLOAD") => {
    const formData = new FormData();
    formData.append("file", file);
    return apiClient
      .post<JewelleryImage>(`/jewellery/${id}/images`, formData, {
        params: { captureSource },
        headers: { "Content-Type": "multipart/form-data" },
      })
      .then((r) => r.data);
  },
  primaryImage: (id: string, imageId: string) =>
    apiClient.patch<JewelleryImage>(`/jewellery/${id}/images/${imageId}/primary`).then((r) => r.data),
  deleteImage: (id: string, imageId: string) => apiClient.delete(`/jewellery/${id}/images/${imageId}`).then((r) => r.data),
  adjust: (
    id: string,
    payload: { password: string; reason: string; typeId?: string | null; karat?: string | null; weight?: string | null; archive?: boolean },
  ) => apiClient.post<StockAdjustment>(`/jewellery/${id}/adjustments`, payload).then((r) => r.data),
  adjustments: (id: string, params: { page?: number; size?: number } = {}) =>
    apiClient.get<PageResponse<StockAdjustment>>(`/jewellery/${id}/adjustments`, { params }).then((r) => r.data),
};

export const billingApi = {
  list: (params: { page?: number; size?: number; from?: string; to?: string; q?: string }) =>
    apiClient.get<PageResponse<Bill>>("/bill", { params }).then((r) => r.data),
  get: (id: string) => apiClient.get<Bill>(`/bill/${id}`).then((r) => r.data),
  create: (payload: CreateBillRequest) => apiClient.post<Bill>("/bill", payload).then((r) => r.data),
  whatsapp: (id: string, phoneNumber: string) =>
    apiClient.post<{ status: string; message: string }>(`/bill/${id}/whatsapp`, { phoneNumber }).then((r) => r.data),
  void: (id: string, payload: { password: string; reason: string }) =>
    apiClient.post(`/bills/${id}/void`, payload).then((r) => r.data),
  returnItems: (
    id: string,
    payload: { password: string; reason: string; items: { billItemId: string; refundAmount?: string | null; restock?: boolean }[] },
  ) => apiClient.post(`/bills/${id}/returns`, payload).then((r) => r.data),
  exchange: (id: string, payload: { password: string; reason: string }) =>
    apiClient.post(`/bills/${id}/exchange`, payload).then((r) => r.data),
};

export const auditApi = {
  list: (params: { page?: number; size?: number }) =>
    apiClient.get<PageResponse<StockAudit>>("/audit/report", { params }).then((r) => r.data),
  start: (auditDate?: string, password?: string, repeatReason?: string) =>
    apiClient.post<StockAudit>("/audit/start", { auditDate: auditDate || null, password: password || null, repeatReason: repeatReason || null }).then((r) => r.data),
  scan: (auditId: string, token: string) => apiClient.post<AuditScanResponse>("/audit/scan", { auditId, token }).then((r) => r.data),
  attemptClose: (auditId: string) => apiClient.post<AuditAttemptCloseResponse>(`/audits/${auditId}/attempt-close`).then((r) => r.data),
  forceClose: (auditId: string, payload: { password: string; reason: string }) =>
    apiClient.post<StockAudit>(`/audits/${auditId}/force-close`, payload).then((r) => r.data),
  close: (auditId: string, ownerPassword?: string) =>
    apiClient.post<StockAudit>("/audit/close", { auditId, ownerPassword }).then((r) => r.data),
  report: (auditId: string) => apiClient.get<AuditReport>("/audit/report", { params: { auditId } }).then((r) => r.data),
};

export const shopApi = {
  state: (date?: string) => apiClient.get<ShopStateResponse>("/shop/state", { params: { date } }).then((r) => r.data),
  open: (businessDate?: string) => apiClient.post<ShopStateResponse>("/shop/open", { businessDate: businessDate || null }).then((r) => r.data),
  close: (businessDate?: string) => apiClient.post<ShopStateResponse>("/shop/close", { businessDate: businessDate || null }).then((r) => r.data),
};

export const analyticsApi = {
  summary: (params: { range: AnalyticsRange; from?: string; to?: string }) =>
    apiClient.get<AnalyticsSummary>("/analytics/summary", { params }).then((r) => r.data),
};

export const settingsApi = {
  get: () => apiClient.get<TenantSettings>("/tenant/settings").then((r) => r.data),
  update: (payload: Partial<TenantSettings>) => apiClient.put<TenantSettings>("/tenant/settings", payload).then((r) => r.data),
};

export const tenantProfileApi = {
  get: () => apiClient.get<TenantProfile>("/tenant/profile").then((r) => r.data),
  update: (payload: Partial<TenantProfile>) => apiClient.put<TenantProfile>("/tenant/profile", payload).then((r) => r.data),
  uploadLogo: (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return apiClient
      .post<TenantProfile>("/tenant/profile/logo", formData, { headers: { "Content-Type": "multipart/form-data" } })
      .then((r) => r.data);
  },
  deleteLogo: () => apiClient.delete<TenantProfile>("/tenant/profile/logo").then((r) => r.data),
};

export const staffApi = {
  list: (params: { page?: number; size?: number }) =>
    apiClient.get<PageResponse<StaffUser>>("/users/staff", { params }).then((r) => r.data),
  create: (payload: { name: string; email: string; password: string; role?: UserRole }) =>
    apiClient.post<StaffUser>("/users/staff", payload).then((r) => r.data),
  update: (id: string, payload: Partial<{ name: string; email: string; password: string; active: boolean; role: UserRole }>) =>
    apiClient.put<StaffUser>(`/users/staff/${id}`, payload).then((r) => r.data),
  deactivate: (id: string) => apiClient.delete(`/users/staff/${id}`).then((r) => r.data),
};
