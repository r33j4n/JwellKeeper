"use client";

import Link from "next/link";
import Decimal from "decimal.js";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useMemo, useRef, useState } from "react";
import { toast } from "sonner";
import { ClipboardList, Minus, QrCode, Search, StoreIcon, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Select } from "@/components/ui/select";
import { PageHeader } from "@/components/shared/page-header";
import { QrScanner, type QrScannerHandle } from "@/components/qr/qr-scanner";
import { auditApi, billingApi, jewelleryApi, settingsApi, shopApi } from "@/lib/api/queries";
import { formatMoney, formatWeight, todayIsoDate } from "@/lib/utils/format";
import type { Jewellery } from "@/lib/api/types";
import { cn } from "@/lib/utils/cn";
import { playSound } from "@/lib/utils/sound";

type PriceRow = {
  finalPrice: string;
  ratePerGram: string;
  makingCharge: string;
  discountAmount: string;
  taxAmount: string;
  notes: string;
};

function emptyPriceRow(): PriceRow {
  return { finalPrice: "", ratePerGram: "", makingCharge: "", discountAmount: "", taxAmount: "", notes: "" };
}

function validMoney(v: string) {
  return v === "" || /^\d+(\.\d{1,2})?$/.test(v);
}

// ─── Per-item price editor ─────────────────────────────────────────────────

function CartItemCard({
  item,
  row,
  currency,
  onUpdate,
  onRemove,
}: {
  item: Jewellery;
  row: PriceRow;
  currency: string;
  onUpdate: (patch: Partial<PriceRow>) => void;
  onRemove: () => void;
}) {
  const subtotal = new Decimal(row.finalPrice || 0);
  return (
    <div className="rounded-xl border border-amber-200 bg-amber-50/40 p-4 space-y-3">
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="font-semibold text-slate-900">{item.typeName}</p>
          <p className="text-xs text-slate-500">
            {item.designName ? `${item.designName} · ` : ""}
            {item.karat} · {formatWeight(item.weight)}
          </p>
        </div>
        <button
          type="button"
          onClick={onRemove}
          className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-slate-400 hover:bg-rose-100 hover:text-rose-600 transition-colors"
        >
          <X className="h-3.5 w-3.5" />
        </button>
      </div>
      <div className="grid gap-2 sm:grid-cols-3">
        <div className="space-y-1">
          <label className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Final price *</label>
          <Input
            inputMode="decimal"
            placeholder="0.00"
            value={row.finalPrice}
            onChange={(e) => validMoney(e.target.value) && onUpdate({ finalPrice: e.target.value })}
            className={cn("h-8 text-sm", !row.finalPrice && "border-amber-400")}
          />
        </div>
        <div className="space-y-1">
          <label className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Rate / gram</label>
          <Input
            inputMode="decimal"
            placeholder="0.00"
            value={row.ratePerGram}
            onChange={(e) => validMoney(e.target.value) && onUpdate({ ratePerGram: e.target.value })}
            className="h-8 text-sm"
          />
        </div>
        <div className="space-y-1">
          <label className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Making charge</label>
          <Input
            inputMode="decimal"
            placeholder="0.00"
            value={row.makingCharge}
            onChange={(e) => validMoney(e.target.value) && onUpdate({ makingCharge: e.target.value })}
            className="h-8 text-sm"
          />
        </div>
        <div className="space-y-1">
          <label className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Discount</label>
          <Input
            inputMode="decimal"
            placeholder="0.00"
            value={row.discountAmount}
            onChange={(e) => validMoney(e.target.value) && onUpdate({ discountAmount: e.target.value })}
            className="h-8 text-sm"
          />
        </div>
        <div className="space-y-1">
          <label className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Tax</label>
          <Input
            inputMode="decimal"
            placeholder="0.00"
            value={row.taxAmount}
            onChange={(e) => validMoney(e.target.value) && onUpdate({ taxAmount: e.target.value })}
            className="h-8 text-sm"
          />
        </div>
        <div className="space-y-1">
          <label className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Item notes</label>
          <Input
            placeholder="Optional"
            value={row.notes}
            onChange={(e) => onUpdate({ notes: e.target.value })}
            className="h-8 text-sm"
          />
        </div>
      </div>
      {row.finalPrice && (
        <div className="text-right text-sm font-semibold text-amber-800">
          {formatMoney(subtotal.toFixed(2), currency)}
        </div>
      )}
    </div>
  );
}

// ─── Main page ─────────────────────────────────────────────────────────────

export function BillCreatePage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const scannerRef = useRef<QrScannerHandle>(null);

  // Cart: jewelleryId → { jewellery, pricing }
  const [cart, setCart] = useState<Record<string, { jewellery: Jewellery; row: PriceRow }>>({});
  const [scannerOpen, setScannerOpen] = useState(false);
  const [browsing, setBrowsing] = useState(false);
  const [searchQ, setSearchQ] = useState("");

  // Bill header fields
  const [billDate, setBillDate] = useState(todayIsoDate());
  const [currencyCode, setCurrencyCode] = useState("");
  const [customerName, setCustomerName] = useState("");
  const [customerPhone, setCustomerPhone] = useState("");
  const [customerAddress, setCustomerAddress] = useState("");
  const [paymentMethod, setPaymentMethod] = useState("");
  const [notes, setNotes] = useState("");

  const settings = useQuery({ queryKey: ["tenant-settings"], queryFn: settingsApi.get });
  const shopState = useQuery({ queryKey: ["shop-state", billDate], queryFn: () => shopApi.state(billDate) });
  const audits = useQuery({ queryKey: ["audits", "billing-block"], queryFn: () => auditApi.list({ size: 50 }) });
  const available = useQuery({
    queryKey: ["jewellery", "available-bill"],
    queryFn: () => jewelleryApi.list({ status: "AVAILABLE", size: 200 }),
    enabled: browsing,
  });

  const hasOpenAudit = Boolean(audits.data?.content.some((a) => a.status === "OPEN"));
  const shopClosed = shopState.data?.status === "CLOSED";
  const billingBlocked = shopClosed || hasOpenAudit;
  const currency = currencyCode || settings.data?.defaultCurrencyCode || "LKR";

  const cartItems = Object.values(cart);
  const selectedIds = new Set(Object.keys(cart));

  const total = useMemo(
    () => cartItems.reduce((sum, { row }) => sum.plus(row.finalPrice || 0), new Decimal(0)).toFixed(2),
    [cartItems],
  );

  const resolveQr = useMutation({
    mutationFn: (token: string) => jewelleryApi.resolveQr(token),
    onSuccess: (jewellery) => {
      if (jewellery.status !== "AVAILABLE") {
        toast.error(`${jewellery.typeName} is ${jewellery.status.toLowerCase()} — cannot bill`);
        playSound("error");
        return;
      }
      if (selectedIds.has(jewellery.id)) {
        toast.info("Already added to bill");
        playSound("warning");
        return;
      }
      setCart((prev) => ({ ...prev, [jewellery.id]: { jewellery, row: emptyPriceRow() } }));
      toast.success(`${jewellery.typeName} added to bill`);
      playSound("success");
    },
    onError: () => {
      playSound("error");
      toast.error("QR not recognised — try again");
    },
  });

  const create = useMutation({
    mutationFn: () =>
      billingApi.create({
        billDate,
        currencyCode: currency,
        customerName: customerName || null,
        customerPhone: customerPhone || null,
        customerAddress: customerAddress || null,
        paymentMethod: paymentMethod || null,
        notes: notes || null,
        items: cartItems
          .filter(({ row }) => row.finalPrice.trim())
          .map(({ jewellery, row }) => ({
            jewelleryId: jewellery.id,
            finalPrice: new Decimal(row.finalPrice).toFixed(2),
            ratePerGram: row.ratePerGram || null,
            makingCharge: row.makingCharge || null,
            discountAmount: row.discountAmount || null,
            taxAmount: row.taxAmount || null,
            notes: row.notes || null,
          })),
      }),
    onSuccess: (bill) => {
      queryClient.invalidateQueries({ queryKey: ["bills"] });
      queryClient.invalidateQueries({ queryKey: ["jewellery"] });
      toast.success("Bill created");
      router.replace(`/dashboard/billing/${bill.id}`);
    },
  });

  function addFromBrowse(jewellery: Jewellery) {
    if (selectedIds.has(jewellery.id)) { toast.info("Already added"); return; }
    setCart((prev) => ({ ...prev, [jewellery.id]: { jewellery, row: emptyPriceRow() } }));
  }

  function removeFromCart(id: string) {
    setCart((prev) => { const next = { ...prev }; delete next[id]; return next; });
  }

  function updateRow(id: string, patch: Partial<PriceRow>) {
    setCart((prev) => ({ ...prev, [id]: { ...prev[id], row: { ...prev[id].row, ...patch } } }));
  }

  const browseable = useMemo(
    () => (available.data?.content ?? []).filter(
      (j) => !selectedIds.has(j.id) &&
        (searchQ === "" || j.typeName.toLowerCase().includes(searchQ.toLowerCase()) || (j.designName ?? "").toLowerCase().includes(searchQ.toLowerCase())),
    ),
    [available.data, selectedIds, searchQ],
  );

  const canSubmit = cartItems.some(({ row }) => row.finalPrice.trim()) && !billingBlocked && !create.isPending;

  return (
    <div className="mx-auto max-w-5xl space-y-5">
      <PageHeader title="Create bill" description="Scan jewellery with the QR reader first, then switch to the camera scanner if needed." />

      {/* Blocked banners */}
      {shopClosed && (
        <div className="flex items-start gap-3 rounded-xl border border-rose-200 bg-rose-50 p-4">
          <StoreIcon className="mt-0.5 h-4 w-4 shrink-0 text-rose-600" />
          <div>
            <p className="text-sm font-semibold text-rose-800">Shop is closed — billing blocked</p>
            <p className="text-sm text-rose-700 mt-0.5">
              <Link href="/dashboard" className="font-semibold underline underline-offset-2">Reopen the shop</Link> before creating bills.
            </p>
          </div>
        </div>
      )}
      {hasOpenAudit && !shopClosed && (
        <div className="flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4">
          <ClipboardList className="mt-0.5 h-4 w-4 shrink-0 text-amber-700" />
          <div>
            <p className="text-sm font-semibold text-amber-800">Audit in progress — billing blocked</p>
            <p className="text-sm text-amber-700 mt-0.5">
              <Link href="/dashboard/audit/scan" className="font-semibold underline underline-offset-2">Close the audit</Link> first.
            </p>
          </div>
        </div>
      )}

      <div className="grid gap-5 xl:grid-cols-[1fr_340px]">
        <div className="space-y-5">
          {/* QR Scanner panel */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <QrCode className="h-4 w-4 text-amber-700" />
                  <h2 className="text-sm font-semibold text-slate-900">Scan jewellery QR</h2>
                </div>
                <Button
                  type="button"
                  size="sm"
                  variant={scannerOpen ? "outline" : "primary"}
                  onClick={() => { setScannerOpen((v) => !v); if (scannerOpen) scannerRef.current?.stop(); }}
                >
                  {scannerOpen ? "Close scanner" : "Open scanner"}
                </Button>
              </div>
            </CardHeader>
            {scannerOpen && (
              <CardContent>
                <QrScanner
                  ref={scannerRef}
                  onScan={(token) => {
                    if (!resolveQr.isPending) resolveQr.mutate(token);
                  }}
                />
                {resolveQr.isPending && (
                  <p className="mt-2 text-center text-xs text-amber-700">Resolving…</p>
                )}
              </CardContent>
            )}
          </Card>

          {/* Cart items */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <h2 className="text-sm font-semibold text-slate-900">
                  Selected items
                  {cartItems.length > 0 && (
                    <span className="ml-2 rounded-full bg-amber-100 px-2 py-0.5 text-xs font-semibold text-amber-700">
                      {cartItems.length}
                    </span>
                  )}
                </h2>
              </div>
            </CardHeader>
            <CardContent className="space-y-3">
              {cartItems.length === 0 ? (
                <div className="rounded-lg border-2 border-dashed border-slate-200 py-8 text-center">
                  <QrCode className="mx-auto mb-2 h-8 w-8 text-slate-300" />
                  <p className="text-sm text-slate-400">Scan a QR or browse to add jewellery</p>
                </div>
              ) : (
                cartItems.map(({ jewellery, row }) => (
                  <CartItemCard
                    key={jewellery.id}
                    item={jewellery}
                    row={row}
                    currency={currency}
                    onUpdate={(patch) => updateRow(jewellery.id, patch)}
                    onRemove={() => removeFromCart(jewellery.id)}
                  />
                ))
              )}
            </CardContent>
          </Card>

          {/* Browse available */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <Search className="h-4 w-4 text-slate-400" />
                  <h2 className="text-sm font-semibold text-slate-900">Browse available stock</h2>
                </div>
                <Button type="button" size="sm" variant="outline" onClick={() => setBrowsing((v) => !v)}>
                  {browsing ? "Hide" : "Show"}
                </Button>
              </div>
              {browsing && (
                <Input
                  placeholder="Search by type or design…"
                  value={searchQ}
                  onChange={(e) => setSearchQ(e.target.value)}
                  className="mt-2 h-8 text-sm"
                />
              )}
            </CardHeader>
            {browsing && (
              <CardContent className="p-0">
                {available.isLoading ? (
                  <div className="p-4 text-sm text-slate-400">Loading…</div>
                ) : browseable.length === 0 ? (
                  <div className="p-4 text-sm text-slate-400">No available items{searchQ ? " matching search" : ""}.</div>
                ) : (
                  <div className="max-h-72 overflow-y-auto divide-y divide-slate-100">
                    {browseable.map((j) => (
                      <div
                        key={j.id}
                        className="flex items-center justify-between gap-3 px-4 py-2.5 hover:bg-amber-50 cursor-pointer"
                        onClick={() => addFromBrowse(j)}
                      >
                        <div>
                          <p className="text-sm font-medium text-slate-900">{j.typeName}</p>
                          <p className="text-xs text-slate-500">
                            {j.designName ? `${j.designName} · ` : ""}{j.karat} · {formatWeight(j.weight)}
                          </p>
                        </div>
                        <span className="text-xs text-amber-700 font-medium">+ Add</span>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            )}
          </Card>
        </div>

        {/* Sidebar — customer + summary */}
        <div className="space-y-5">
          <Card>
            <CardHeader>
              <h2 className="text-sm font-semibold text-slate-900">Bill details</h2>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Date</label>
                <Input type="date" value={billDate} onChange={(e) => setBillDate(e.target.value)} />
              </div>
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Currency</label>
                <Input maxLength={3} value={currency} onChange={(e) => setCurrencyCode(e.target.value.toUpperCase())} />
              </div>
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Customer name</label>
                <Input placeholder="Optional" value={customerName} onChange={(e) => setCustomerName(e.target.value)} />
              </div>
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Phone</label>
                <Input placeholder="Optional" value={customerPhone} onChange={(e) => setCustomerPhone(e.target.value)} />
              </div>
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Payment method</label>
                <Select value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value)}>
                  <option value="">Select…</option>
                  <option value="CASH">Cash</option>
                  <option value="CARD">Card</option>
                  <option value="BANK_TRANSFER">Bank transfer</option>
                  <option value="CHEQUE">Cheque</option>
                  <option value="UPI">UPI</option>
                  <option value="OTHER">Other</option>
                </Select>
              </div>
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Address</label>
                <Textarea placeholder="Optional" rows={2} value={customerAddress} onChange={(e) => setCustomerAddress(e.target.value)} />
              </div>
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Notes</label>
                <Textarea placeholder="Optional" rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} />
              </div>
            </CardContent>
          </Card>

          {/* Total & submit */}
          <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-slate-600">Items selected</span>
              <span className="text-sm font-semibold text-slate-900">{cartItems.filter(({ row }) => row.finalPrice).length}</span>
            </div>
            <div className="flex items-center justify-between border-t border-amber-200 pt-3">
              <span className="text-base font-semibold text-slate-900">Total</span>
              <span className="text-xl font-bold text-amber-800">{formatMoney(total, currency)}</span>
            </div>
            <Button
              className="w-full"
              size="lg"
              disabled={!canSubmit}
              onClick={() => create.mutate()}
            >
              {create.isPending ? "Creating bill…" : "Create bill"}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
