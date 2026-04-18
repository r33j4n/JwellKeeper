"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Image from "next/image";
import { useParams } from "next/navigation";
import { useState } from "react";
import { Download, Eye, RotateCcw, Send, ShieldAlert } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { PageHeader } from "@/components/shared/page-header";
import { StatusBadge } from "@/components/shared/status-badge";
import { billingApi, jewelleryApi, tenantProfileApi } from "@/lib/api/queries";
import { downloadBackendFile, viewBackendFile } from "@/lib/api/downloads";
import { useAuthStore } from "@/lib/store/auth-store";
import { formatDate, formatDateTime, formatMoney, formatWeight } from "@/lib/utils/format";
import { cn } from "@/lib/utils/cn";
import Decimal from "decimal.js";

// ─── Per-item QR image ────────────────────────────────────────────────────

function ItemQr({ jewelleryId }: { jewelleryId: string }) {
  const qr = useQuery({
    queryKey: ["jewellery-qr", jewelleryId],
    queryFn: () => jewelleryApi.qr(jewelleryId),
    staleTime: Infinity,
  });
  if (!qr.data) return <div className="h-16 w-16 animate-pulse rounded bg-slate-100" />;
  return (
    <Image
      src={`data:${qr.data.contentType};base64,${qr.data.qrCodeBase64}`}
      alt="QR"
      width={64}
      height={64}
      unoptimized
      className="h-16 w-16 rounded border border-slate-200"
    />
  );
}

// ─── Invoice line ─────────────────────────────────────────────────────────

function MoneyLine({ label, value, currency, bold, negative }: { label: string; value: string | null | undefined; currency: string; bold?: boolean; negative?: boolean }) {
  if (!value || new Decimal(value).isZero()) return null;
  return (
    <div className={cn("flex items-center justify-between text-sm", bold && "font-semibold")}>
      <span className={negative ? "text-emerald-700" : "text-slate-600"}>{label}</span>
      <span className={negative ? "text-emerald-700" : "text-slate-900"}>
        {negative ? "−" : ""}{formatMoney(value, currency)}
      </span>
    </div>
  );
}

// ─── Professional invoice view ────────────────────────────────────────────

function InvoiceView({ billId }: { billId: string }) {
  const bill = useQuery({ queryKey: ["bill", billId], queryFn: () => billingApi.get(billId) });
  const tenantProfile = useQuery({ queryKey: ["tenant-profile"], queryFn: tenantProfileApi.get });
  const b = bill.data;
  const profile = tenantProfile.data;

  const shopLines = [profile?.shopContactNumber, profile?.shopEmail, profile?.shopAddress, profile?.taxNumber ? `Tax/Reg: ${profile.taxNumber}` : null]
    .filter((line): line is string => Boolean(line && line.trim().length > 0));

  if (bill.isLoading || !b) return <div className="rounded-xl bg-white p-8 text-sm text-slate-400">Loading invoice…</div>;

  const grandTotal = b.items.reduce((s, i) => s.plus(i.finalPrice), new Decimal(0));
  const totalDiscount = b.items.reduce((s, i) => s.plus(i.discountAmount ?? 0), new Decimal(0));
  const totalTax = b.items.reduce((s, i) => s.plus(i.taxAmount ?? 0), new Decimal(0));
  const totalMaking = b.items.reduce((s, i) => s.plus(i.makingCharge ?? 0), new Decimal(0));
  const totalWeight = b.items.reduce((s, i) => s.plus(i.weight ?? 0), new Decimal(0));

  return (
    <div className="rounded-xl border border-slate-200 bg-white shadow-sm overflow-hidden">
      {/* Header band */}
      <div className="bg-gradient-to-r from-amber-700 to-amber-600 px-8 py-6 text-white">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <div className="mb-1 flex items-start gap-3">
              {profile?.logoAvailable ? (
                <Image
                  src="/api/backend/tenant/profile/logo"
                  alt={profile.shopName || "Shop logo"}
                  width={48}
                  height={48}
                  unoptimized
                  className="h-12 w-12 shrink-0 rounded-md border border-white/25 bg-white object-cover"
                />
              ) : (
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-md border border-white/25 bg-white/20 text-base font-semibold">
                  {(profile?.shopName || "S").trim().charAt(0).toUpperCase()}
                </div>
              )}
              <div className="min-w-0">
                <p className="truncate text-lg font-bold tracking-tight">{profile?.shopName || "Jewellery Shop"}</p>
                {shopLines.length > 0 ? (
                  <div className="mt-1 space-y-0.5 text-[11px] leading-4 text-amber-100">
                    {shopLines.map((line) => (
                      <p key={line} className="truncate">{line}</p>
                    ))}
                  </div>
                ) : (
                  <p className="mt-0.5 text-xs text-amber-200">Update shop details in Settings to show contact information.</p>
                )}
              </div>
            </div>
          </div>
          <div className="text-right">
            <p className="text-2xl font-bold tracking-tight">{b.billNo}</p>
            <p className="text-amber-200 text-xs mt-0.5">TAX INVOICE</p>
            <div className="mt-1.5">
              <StatusBadge value={b.status} />
            </div>
          </div>
        </div>
      </div>

      <div className="px-8 py-6 space-y-6">
        {/* Bill meta + Customer */}
        <div className="grid gap-6 sm:grid-cols-2">
          <div className="space-y-3">
            <p className="text-[10px] font-bold uppercase tracking-widest text-slate-400">Bill to</p>
            {b.customerName ? (
              <>
                <p className="font-semibold text-slate-900">{b.customerName}</p>
                {b.customerPhone && <p className="text-sm text-slate-600">{b.customerPhone}</p>}
                {b.customerAddress && <p className="text-sm text-slate-500 whitespace-pre-line">{b.customerAddress}</p>}
              </>
            ) : (
              <p className="text-sm text-slate-400 italic">Walk-in customer</p>
            )}
          </div>
          <div className="space-y-2 sm:text-right">
            <p className="text-[10px] font-bold uppercase tracking-widest text-slate-400">Invoice details</p>
            <div className="text-sm space-y-1">
              <div className="flex sm:justify-end gap-6">
                <span className="text-slate-500">Date</span>
                <span className="font-medium text-slate-900">{formatDate(b.billDate)}</span>
              </div>
              <div className="flex sm:justify-end gap-6">
                <span className="text-slate-500">Payment</span>
                <span className="font-medium text-slate-900">{b.paymentMethod || "—"}</span>
              </div>
              <div className="flex sm:justify-end gap-6">
                <span className="text-slate-500">Currency</span>
                <span className="font-medium text-slate-900">{b.currencyCode}</span>
              </div>
              <div className="flex sm:justify-end gap-6">
                <span className="text-slate-500">Created</span>
                <span className="font-medium text-slate-900">{formatDateTime(b.createdAt)}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Items table */}
        <div>
          <p className="text-[10px] font-bold uppercase tracking-widest text-slate-400 mb-3">Items</p>
          <div className="overflow-x-auto rounded-lg border border-slate-100">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 text-[10px] uppercase tracking-wider text-slate-500 border-b border-slate-100">
                <tr>
                  <th className="px-4 py-2.5 text-left">#</th>
                  <th className="px-4 py-2.5 text-left">Item</th>
                  <th className="px-4 py-2.5 text-right">Weight</th>
                  <th className="px-4 py-2.5 text-right">Rate/g</th>
                  <th className="px-4 py-2.5 text-right">Making</th>
                  <th className="px-4 py-2.5 text-right">Discount</th>
                  <th className="px-4 py-2.5 text-right">Tax</th>
                  <th className="px-4 py-2.5 text-right">Amount</th>
                  <th className="px-4 py-2.5 text-center">QR</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {b.items.map((item, idx) => (
                  <tr key={item.id} className="hover:bg-slate-50/50">
                    <td className="px-4 py-3 text-slate-400 tabular-nums">{idx + 1}</td>
                    <td className="px-4 py-3">
                      <p className="font-semibold text-slate-900">{item.typeNameSnapshot}</p>
                      {item.designNameSnapshot && (
                        <p className="text-xs text-slate-500">{item.designNameSnapshot}</p>
                      )}
                      <p className="text-xs text-slate-400 font-mono mt-0.5">{item.karatSnapshot}</p>
                      {item.notes && <p className="text-xs text-slate-400 italic mt-0.5">{item.notes}</p>}
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums text-slate-700">{formatWeight(item.weight)}</td>
                    <td className="px-4 py-3 text-right tabular-nums text-slate-500">
                      {item.ratePerGram ? formatMoney(item.ratePerGram, b.currencyCode) : "—"}
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums text-slate-500">
                      {item.makingCharge ? formatMoney(item.makingCharge, b.currencyCode) : "—"}
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums text-emerald-700">
                      {item.discountAmount && !new Decimal(item.discountAmount).isZero()
                        ? `−${formatMoney(item.discountAmount, b.currencyCode)}`
                        : "—"}
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums text-slate-500">
                      {item.taxAmount ? formatMoney(item.taxAmount, b.currencyCode) : "—"}
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums font-semibold text-slate-900">
                      {formatMoney(item.finalPrice, b.currencyCode)}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex justify-center">
                        <ItemQr jewelleryId={item.jewelleryId} />
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot className="border-t-2 border-slate-200 bg-slate-50">
                <tr>
                  <td colSpan={2} className="px-4 py-2.5 text-xs font-semibold text-slate-500">
                    {b.items.length} item{b.items.length !== 1 ? "s" : ""}
                  </td>
                  <td className="px-4 py-2.5 text-right text-xs font-semibold tabular-nums text-slate-700">
                    {formatWeight(totalWeight.toFixed(3))}
                  </td>
                  <td colSpan={6} />
                </tr>
              </tfoot>
            </table>
          </div>
        </div>

        {/* Totals */}
        <div className="flex justify-end">
          <div className="w-full max-w-xs space-y-1.5 rounded-lg border border-slate-100 bg-slate-50 p-4">
            {!totalMaking.isZero() && (
              <MoneyLine label="Making charges" value={totalMaking.toFixed(2)} currency={b.currencyCode} />
            )}
            {!totalDiscount.isZero() && (
              <MoneyLine label="Total discount" value={totalDiscount.toFixed(2)} currency={b.currencyCode} negative />
            )}
            {!totalTax.isZero() && (
              <MoneyLine label="Tax" value={totalTax.toFixed(2)} currency={b.currencyCode} />
            )}
            <div className="flex items-center justify-between border-t border-slate-200 pt-2 mt-2">
              <span className="text-base font-bold text-slate-900">Grand Total</span>
              <span className="text-xl font-bold text-amber-700">{formatMoney(grandTotal.toFixed(2), b.currencyCode)}</span>
            </div>
          </div>
        </div>

        {/* Notes */}
        {b.notes && (
          <div className="rounded-lg bg-amber-50 border border-amber-100 px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-wider text-amber-700 mb-1">Notes</p>
            <p className="text-sm text-slate-700 whitespace-pre-line">{b.notes}</p>
          </div>
        )}

        {/* Footer */}
        <div className="border-t border-slate-100 pt-4 text-center">
          <p className="text-sm font-medium text-slate-600">Thank you for your purchase!</p>
          <p className="text-xs text-slate-400 mt-0.5">Each QR code uniquely identifies the jewellery item. Scan to verify authenticity.</p>
        </div>
      </div>
    </div>
  );
}

// ─── Page shell ───────────────────────────────────────────────────────────

export function BillDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const queryClient = useQueryClient();
  const user = useAuthStore((state) => state.user);
  const [phone, setPhone] = useState("");
  const [correctionPassword, setCorrectionPassword] = useState("");
  const [correctionReason, setCorrectionReason] = useState("");
  const [returnItems, setReturnItems] = useState<Record<string, boolean>>({});
  const [downloading, setDownloading] = useState(false);
  const [viewing, setViewing] = useState(false);

  const bill = useQuery({ queryKey: ["bill", id], queryFn: () => billingApi.get(id) });
  const canCorrect = user?.role === "OWNER" || user?.role === "MANAGER";

  const whatsapp = useMutation({
    mutationFn: () => billingApi.whatsapp(id, phone),
    onSuccess: (result) => toast.success(result.message || "WhatsApp send stub processed"),
  });
  const voidBill = useMutation({
    mutationFn: () => billingApi.void(id, { password: correctionPassword, reason: correctionReason }),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ["bill", id] }); queryClient.invalidateQueries({ queryKey: ["jewellery"] }); toast.success("Bill voided"); },
  });
  const returnBillItems = useMutation({
    mutationFn: () => billingApi.returnItems(id, {
      password: correctionPassword,
      reason: correctionReason,
      items: Object.entries(returnItems).filter(([, c]) => c).map(([billItemId]) => ({ billItemId, restock: true })),
    }),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ["bill", id] }); queryClient.invalidateQueries({ queryKey: ["jewellery"] }); toast.success("Return recorded"); },
  });
  const exchange = useMutation({
    mutationFn: () => billingApi.exchange(id, { password: correctionPassword, reason: correctionReason }),
    onSuccess: () => toast.success("Exchange scaffold recorded"),
  });

  async function downloadPdf() {
    if (!bill.data) return;
    setDownloading(true);
    try { await downloadBackendFile(`/bill/${id}/pdf`, `bill-${bill.data.billNo}.pdf`); }
    catch (e) { toast.error(e instanceof Error ? e.message : "PDF download failed"); }
    finally { setDownloading(false); }
  }

  async function viewPdf() {
    setViewing(true);
    try { await viewBackendFile(`/bill/${id}/pdf`); }
    catch (e) { toast.error(e instanceof Error ? e.message : "PDF preview failed"); }
    finally { setViewing(false); }
  }

  if (bill.isLoading || !bill.data) return <div className="rounded-xl bg-white p-8 text-sm text-slate-400">Loading…</div>;

  return (
    <div className="mx-auto max-w-4xl space-y-5">
      <PageHeader
        title={`Bill ${bill.data.billNo}`}
        description={`${formatDate(bill.data.billDate)} · ${formatMoney(bill.data.totalAmount, bill.data.currencyCode)}`}
        actions={
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" size="sm" disabled={viewing} onClick={viewPdf}>
              <Eye className="h-4 w-4" />{viewing ? "Opening…" : "View PDF"}
            </Button>
            <Button variant="outline" size="sm" disabled={downloading} onClick={downloadPdf}>
              <Download className="h-4 w-4" />{downloading ? "Preparing…" : "Download PDF"}
            </Button>
          </div>
        }
      />

      {/* Professional invoice */}
      <InvoiceView billId={id} />

      {/* WhatsApp */}
      <Card>
        <CardHeader><h2 className="text-sm font-semibold">Send PDF via WhatsApp</h2></CardHeader>
        <CardContent className="flex flex-col gap-3 sm:flex-row">
          <Input placeholder="Phone number with country code" value={phone} onChange={(e) => setPhone(e.target.value)} />
          <Button disabled={!phone || whatsapp.isPending} onClick={() => whatsapp.mutate()}>
            <Send className="h-4 w-4" />{whatsapp.isPending ? "Sending…" : "Send"}
          </Button>
        </CardContent>
      </Card>

      {/* Corrections */}
      {canCorrect && (
        <Card>
          <CardHeader>
            <h2 className="text-sm font-semibold">Corrections</h2>
            <p className="text-xs text-slate-500 mt-0.5">Void, return, and exchange require owner or manager password. The original bill remains immutable.</p>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="grid gap-3 sm:grid-cols-2">
              <Input type="password" placeholder="Owner / manager password" value={correctionPassword} onChange={(e) => setCorrectionPassword(e.target.value)} />
              <Input placeholder="Correction reason" value={correctionReason} onChange={(e) => setCorrectionReason(e.target.value)} />
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-500 mb-2">Return items</p>
              <div className="space-y-1.5">
                {bill.data.items.map((item) => (
                  <label key={item.id} className="flex items-center gap-2.5 rounded-lg border border-slate-200 px-3 py-2 text-sm cursor-pointer hover:bg-slate-50">
                    <input
                      type="checkbox"
                      checked={Boolean(returnItems[item.id])}
                      onChange={(e) => setReturnItems((prev) => ({ ...prev, [item.id]: e.target.checked }))}
                      className="h-4 w-4 accent-amber-600"
                    />
                    <span className="flex-1 text-slate-700">
                      {item.typeNameSnapshot}{item.designNameSnapshot ? ` · ${item.designNameSnapshot}` : ""} · {item.karatSnapshot} · {formatWeight(item.weight)}
                    </span>
                    <span className="font-semibold text-slate-900">{formatMoney(item.finalPrice, bill.data!.currencyCode)}</span>
                  </label>
                ))}
              </div>
            </div>
            <div className="flex flex-wrap gap-2 pt-1">
              <Button
                variant="danger"
                size="sm"
                disabled={bill.data.status === "VOIDED" || !correctionPassword || !correctionReason.trim() || voidBill.isPending}
                onClick={() => voidBill.mutate()}
              >
                <ShieldAlert className="h-4 w-4" />Void bill
              </Button>
              <Button
                variant="secondary"
                size="sm"
                disabled={!correctionPassword || !correctionReason.trim() || !Object.values(returnItems).some(Boolean) || returnBillItems.isPending}
                onClick={() => returnBillItems.mutate()}
              >
                <RotateCcw className="h-4 w-4" />Return selected
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={!correctionPassword || !correctionReason.trim() || exchange.isPending}
                onClick={() => exchange.mutate()}
              >
                Exchange scaffold
              </Button>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
