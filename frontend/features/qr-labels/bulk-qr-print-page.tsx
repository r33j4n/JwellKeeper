"use client";

import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Printer, Search } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { PageHeader } from "@/components/shared/page-header";
import { StatusBadge } from "@/components/shared/status-badge";
import { EmptyState, SkeletonBlock } from "@/components/shared/states";
import { jewelleryApi, tenantProfileApi } from "@/lib/api/queries";
import type { Jewellery, JewelleryStatus } from "@/lib/api/types";
import { useAuthStore } from "@/lib/store/auth-store";
import { formatDate, formatWeight } from "@/lib/utils/format";
import { printQrLabels } from "@/lib/utils/qr-label-print";

export function BulkQrPrintPage() {
  const authShopName = useAuthStore((state) => state.user?.shopName || "");
  const [status, setStatus] = useState<JewelleryStatus | "">("AVAILABLE");
  const [karat, setKarat] = useState("");
  const [q, setQ] = useState("");
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [printing, setPrinting] = useState(false);

  const jewellery = useQuery({
    queryKey: ["bulk-qr-jewellery", status, karat, q],
    queryFn: () => jewelleryApi.list({ status, karat: karat || undefined, q: q || undefined, size: 200 }),
  });
  const tenantProfile = useQuery({ queryKey: ["tenant-profile"], queryFn: tenantProfileApi.get });
  const shopName = tenantProfile.data?.shopName?.trim() || authShopName.trim() || "Jewellery Shop";

  const rows = useMemo(() => jewellery.data?.content || [], [jewellery.data?.content]);
  const selectedRows = useMemo(
    () => rows.filter((item) => selectedIds.includes(item.id)),
    [rows, selectedIds],
  );
  const allSelected = rows.length > 0 && rows.every((item) => selectedIds.includes(item.id));

  function toggle(id: string, checked: boolean) {
    setSelectedIds((current) => (checked ? Array.from(new Set([...current, id])) : current.filter((itemId) => itemId !== id)));
  }

  function setAll(checked: boolean) {
    setSelectedIds(checked ? rows.map((item) => item.id) : []);
  }

  async function printSelected() {
    if (!selectedRows.length) {
      toast.error("Select at least one jewellery item");
      return;
    }
    setPrinting(true);
    try {
      const labels = await Promise.all(
        selectedRows.map(async (jewelleryItem: Jewellery) => {
          const qr = await jewelleryApi.qr(jewelleryItem.id);
          return { jewellery: jewelleryItem, qrImage: qr.qrCodeBase64 };
        }),
      );
      const printed = printQrLabels(labels, shopName, "Bulk jewellery QR labels", "bulk");
      if (!printed) {
        toast.error("Popup blocked. Allow popups to print QR labels.");
      }
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Could not prepare QR labels");
    } finally {
      setPrinting(false);
    }
  }

  return (
    <div className="grid gap-6">
      <PageHeader
        title="Bulk QR Print"
        description="Select jewellery items and print compact QR labels that fit under the physical jewellery."
        actions={
          <Button disabled={!selectedRows.length || printing} onClick={printSelected}>
            <Printer className="h-4 w-4" />
            {printing ? "Preparing..." : `Print ${selectedRows.length || ""} label${selectedRows.length === 1 ? "" : "s"}`}
          </Button>
        }
      />

      <Card className="p-4">
        <div className="grid gap-3 md:grid-cols-[180px_180px_1fr]">
          <Select value={status} onChange={(event) => { setStatus(event.target.value as JewelleryStatus | ""); setSelectedIds([]); }}>
            <option value="">All statuses</option>
            <option value="AVAILABLE">Available</option>
            <option value="MISSING">Missing</option>
            <option value="SOLD">Sold</option>
          </Select>
          <Input
            placeholder="Karat, e.g. 22K"
            value={karat}
            onChange={(event) => {
              setKarat(event.target.value.toUpperCase());
              setSelectedIds([]);
            }}
          />
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <Input
              className="pl-9"
              placeholder="Search type, design, notes, bill no"
              value={q}
              onChange={(event) => {
                setQ(event.target.value);
                setSelectedIds([]);
              }}
            />
          </div>
        </div>
      </Card>

      <Card className="overflow-hidden">
        {jewellery.isLoading ? (
          <div className="p-4">
            <SkeletonBlock className="h-72" />
          </div>
        ) : rows.length ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-100 text-xs uppercase text-slate-500">
                <tr>
                  <th className="px-4 py-3">
                    <input type="checkbox" checked={allSelected} onChange={(event) => setAll(event.target.checked)} aria-label="Select all" />
                  </th>
                  <th className="px-4 py-3">Type</th>
                  <th className="px-4 py-3">Sub-type / Design</th>
                  <th className="px-4 py-3">Karat</th>
                  <th className="px-4 py-3">Weight</th>
                  <th className="px-4 py-3">Status / Added Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {rows.map((item) => (
                  <tr key={item.id} className="hover:bg-amber-50/40">
                    <td className="px-4 py-3">
                      <input
                        type="checkbox"
                        checked={selectedIds.includes(item.id)}
                        onChange={(event) => toggle(item.id, event.target.checked)}
                        aria-label={`Select ${item.typeName}`}
                      />
                    </td>
                    <td className="px-4 py-3 font-semibold text-amber-800">{item.typeName}</td>
                    <td className="px-4 py-3">{item.designName || "-"}</td>
                    <td className="px-4 py-3">{item.karat}</td>
                    <td className="px-4 py-3">{formatWeight(item.weight)}</td>
                    <td className="px-4 py-3">
                      <div className="space-y-1">
                        <StatusBadge value={item.status} />
                        <p className="text-xs text-slate-500">Added: {formatDate(item.createdAt)}</p>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="p-6">
            <EmptyState title="No jewellery found" description="Adjust filters or add stock first." />
          </div>
        )}
      </Card>
    </div>
  );
}
