"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { ArrowLeft, Boxes, Download, Eye, ListChecks, PackagePlus, Scale, ShoppingBag, TriangleAlert } from "lucide-react";
import { toast } from "sonner";
import Decimal from "decimal.js";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { PageHeader } from "@/components/shared/page-header";
import { StatusBadge } from "@/components/shared/status-badge";
import { SkeletonBlock } from "@/components/shared/states";
import { auditApi } from "@/lib/api/queries";
import { downloadBackendFile, viewBackendFile } from "@/lib/api/downloads";
import { formatDate, formatMoney, formatWeight, shortId } from "@/lib/utils/format";
import type { AuditReportStockRow, AuditReportSoldRow } from "@/lib/api/types";
import { cn } from "@/lib/utils/cn";

export function AuditReportPage() {
  const [auditId, setAuditId] = useState("");
  const [downloading, setDownloading] = useState(false);
  const [viewing, setViewing] = useState(false);

  const audits = useQuery({ queryKey: ["audits", "report"], queryFn: () => auditApi.list({ size: 100 }) });
  const closedAudits = useMemo(
    () => audits.data?.content.filter((audit) => audit.status === "CLOSED") ?? [],
    [audits.data],
  );
  const selectedAudit = closedAudits.find((audit) => audit.id === auditId) ?? closedAudits[0];

  const report = useQuery({
    queryKey: ["audit-report", selectedAudit?.id],
    queryFn: () => auditApi.report(selectedAudit!.id),
    enabled: Boolean(selectedAudit),
  });

  const currentRows = report.data?.currentStockItems ?? [];
  const soldRows = report.data?.todaySoldItems ?? [];
  const todayAddedRows = currentRows.filter((row) => row.category === "TODAY_ADDED");
  const alreadyAvailableRows = currentRows.filter((row) => row.category === "ALREADY_AVAILABLE");
  const missingRows = currentRows.filter((row) => row.category === "MISSING_IN_AUDIT");
  const currentStockWeight = currentRows.reduce((sum, row) => sum.plus(row.weight || 0), new Decimal(0)).toFixed(3);

  async function downloadPdf() {
    if (!selectedAudit) return;
    setDownloading(true);
    try {
      await downloadBackendFile(`/audit/${selectedAudit.id}/pdf`, `audit-${selectedAudit.auditDate}-${selectedAudit.runNumber}.pdf`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Audit PDF download failed");
    } finally {
      setDownloading(false);
    }
  }

  async function viewPdf() {
    if (!selectedAudit) return;
    setViewing(true);
    try {
      await viewBackendFile(`/audit/${selectedAudit.id}/pdf`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Audit PDF preview failed");
    } finally {
      setViewing(false);
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Detailed daily audit report"
        description="Daily stock proof with today-added stock, existing stock, sold items, missing items, and per-type tally."
        actions={
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" size="sm" disabled={!selectedAudit || viewing} onClick={viewPdf}>
              <Eye className="h-4 w-4" />
              {viewing ? "Opening..." : "View PDF"}
            </Button>
            <Button variant="outline" size="sm" disabled={!selectedAudit || downloading} onClick={downloadPdf}>
              <Download className="h-4 w-4" />
              {downloading ? "Preparing..." : "Download PDF"}
            </Button>
            <Button asChild variant="outline" size="sm">
              <Link href="/dashboard/audit">
                <ArrowLeft className="h-4 w-4" />
                Back to audit
              </Link>
            </Button>
          </div>
        }
      />

      <Card>
        <CardContent className="flex flex-wrap items-center gap-3 p-4">
          <label className="text-sm font-medium text-slate-600">Select audit</label>
          {audits.isLoading ? (
            <div className="h-10 w-64 animate-pulse rounded-md bg-slate-100" />
          ) : closedAudits.length === 0 ? (
            <p className="text-sm text-slate-500">No closed audits yet.</p>
          ) : (
            <Select value={selectedAudit?.id ?? ""} onChange={(event) => setAuditId(event.target.value)} className="w-72">
              {closedAudits.map((audit) => (
                <option key={audit.id} value={audit.id}>
                  {audit.auditName || `${formatDate(audit.auditDate)} · Audit #${audit.runNumber}`}
                </option>
              ))}
            </Select>
          )}
        </CardContent>
      </Card>

      {selectedAudit && (
        <>
          {report.isLoading ? (
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
              <SkeletonBlock /><SkeletonBlock /><SkeletonBlock /><SkeletonBlock /><SkeletonBlock />
            </div>
          ) : report.data ? (
            <>
              <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
                <SummaryCard icon={<PackagePlus className="h-5 w-5" />} label="Today added" value={todayAddedRows.length} sub="New stock in this audit day" color="gold" />
                <SummaryCard icon={<Boxes className="h-5 w-5" />} label="Already available" value={alreadyAvailableRows.length} sub="Older stock still available" color="slate" />
                <SummaryCard icon={<ShoppingBag className="h-5 w-5" />} label="Sold today" value={soldRows.length} sub={salesTotals(report.data.salesTotalsByCurrency)} color="blue" />
                <SummaryCard icon={<TriangleAlert className="h-5 w-5" />} label="Missing" value={missingRows.length} sub={missingRows.length ? "Needs follow-up" : "All accounted for"} color={missingRows.length ? "rose" : "slate"} />
                <SummaryCard icon={<Scale className="h-5 w-5" />} label="Current stock weight" value={formatWeight(currentStockWeight)} sub={`${currentRows.length} stock row${currentRows.length === 1 ? "" : "s"}`} color="emerald" />
              </section>

              <ReportLegend />

              <TypeTallyTable rows={report.data.typeTallies} />

              <Card>
                <CardHeader>
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                      <h2 className="text-sm font-semibold text-slate-900">Current Stock Details — {formatDate(report.data.auditDate)}</h2>
                      <p className="mt-0.5 text-xs text-slate-500">
                        Today added and already available items are in the same stock table. Missing audit rows stay visible for follow-up.
                      </p>
                    </div>
                    <div className="flex items-center gap-2 text-xs text-slate-500">
                      <ListChecks className="h-3.5 w-3.5" />
                      {todayAddedRows.length} today added · {alreadyAvailableRows.length} already available · {missingRows.length} missing
                    </div>
                  </div>
                </CardHeader>
                <CardContent className="p-0">
                  <CurrentStockTable rows={currentRows} />
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <h2 className="text-sm font-semibold text-slate-900">Today Sold Items</h2>
                  <p className="mt-0.5 text-xs text-slate-500">
                    Items billed on the audit date. Prices are displayed with each bill currency.
                  </p>
                </CardHeader>
                <CardContent className="p-0">
                  <SoldItemsTable rows={soldRows} />
                </CardContent>
              </Card>
            </>
          ) : null}
        </>
      )}
    </div>
  );
}

function ReportLegend() {
  return (
    <Card>
      <CardContent className="grid gap-3 p-4 md:grid-cols-3">
        <LegendItem className="border-l-4 border-amber-600 bg-amber-50" title="Today added" text="New jewellery created on the audit date." />
        <LegendItem className="border-l-4 border-slate-500 bg-slate-50" title="Already available" text="Existing stock still expected in the shop." />
        <LegendItem className="border-l-4 border-blue-700 bg-blue-50" title="Today sold" text="Items billed on the audit date." />
      </CardContent>
    </Card>
  );
}

function LegendItem({ className, title, text }: { className: string; title: string; text: string }) {
  return (
    <div className={cn("rounded-md p-3", className)}>
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-700">{title}</p>
      <p className="mt-1 text-sm text-slate-600">{text}</p>
    </div>
  );
}

function TypeTallyTable({ rows }: { rows: { typeName: string; todayAddedCount: number; alreadyAvailableCount: number; missingCount: number; currentStockCount: number; soldTodayCount: number; currentStockWeight: string }[] }) {
  return (
    <Card>
      <CardHeader>
        <h2 className="text-sm font-semibold text-slate-900">Quick Tally By Jewellery Type</h2>
        <p className="mt-0.5 text-xs text-slate-500">Counts per type for stock balancing, purchase decisions, and daily close verification.</p>
      </CardHeader>
      <CardContent className="p-0">
        {rows.length === 0 ? (
          <EmptyRows text="No tally rows for this audit." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-y border-slate-100 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-4 py-3">Type</th>
                  <th className="px-4 py-3">Today added</th>
                  <th className="px-4 py-3">Already available</th>
                  <th className="px-4 py-3">Missing</th>
                  <th className="px-4 py-3">Current stock</th>
                  <th className="px-4 py-3">Sold today</th>
                  <th className="px-4 py-3">Stock weight</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {rows.map((row) => (
                  <tr key={row.typeName} className="hover:bg-slate-50">
                    <td className="px-4 py-3 font-medium text-slate-900">{row.typeName}</td>
                    <td className="px-4 py-3">{row.todayAddedCount}</td>
                    <td className="px-4 py-3">{row.alreadyAvailableCount}</td>
                    <td className={cn("px-4 py-3", row.missingCount > 0 && "font-semibold text-rose-700")}>{row.missingCount}</td>
                    <td className="px-4 py-3 font-semibold text-slate-900">{row.currentStockCount}</td>
                    <td className="px-4 py-3">{row.soldTodayCount}</td>
                    <td className="px-4 py-3">{formatWeight(row.currentStockWeight)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function CurrentStockTable({ rows }: { rows: AuditReportStockRow[] }) {
  if (rows.length === 0) return <EmptyRows text="No current stock rows captured for this audit." />;
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead className="border-y border-slate-100 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
          <tr>
            <th className="px-4 py-3">#</th>
            <th className="px-4 py-3">Category</th>
            <th className="px-4 py-3">Type</th>
            <th className="px-4 py-3">Design</th>
            <th className="px-4 py-3">Karat</th>
            <th className="px-4 py-3">Weight</th>
            <th className="px-4 py-3">Scanned</th>
            <th className="px-4 py-3">Resolution</th>
            <th className="px-4 py-3">Status</th>
            <th className="px-4 py-3">Added</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {rows.map((row, index) => (
            <tr key={row.jewelleryId} className={cn("border-l-4 hover:brightness-[0.99]", rowShade(row.category))}>
              <td className="px-4 py-3 text-slate-400 tabular-nums">{index + 1}</td>
              <td className="px-4 py-3"><CategoryBadge category={row.category} /></td>
              <td className="px-4 py-3 font-medium text-slate-900">{row.typeName}</td>
              <td className="px-4 py-3 text-slate-500">{row.designName || "-"}</td>
              <td className="px-4 py-3 font-mono text-xs">{row.karat}</td>
              <td className="px-4 py-3 tabular-nums">{formatWeight(row.weight)}</td>
              <td className="px-4 py-3">{row.scanned ? "Yes" : "No"}</td>
              <td className="px-4 py-3 text-xs text-slate-500">{formatResolution(row.resolution)}</td>
              <td className="px-4 py-3">{row.status ? <StatusBadge value={row.status} /> : "-"}</td>
              <td className="px-4 py-3 text-slate-500 whitespace-nowrap">{formatDate(row.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function SoldItemsTable({ rows }: { rows: AuditReportSoldRow[] }) {
  if (rows.length === 0) return <EmptyRows text="No items were sold on this audit date." />;
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead className="border-y border-slate-100 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
          <tr>
            <th className="px-4 py-3">#</th>
            <th className="px-4 py-3">Bill no</th>
            <th className="px-4 py-3">Type</th>
            <th className="px-4 py-3">Design</th>
            <th className="px-4 py-3">Karat</th>
            <th className="px-4 py-3">Weight</th>
            <th className="px-4 py-3">Price</th>
            <th className="px-4 py-3">Jewellery</th>
            <th className="px-4 py-3">Notes</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {rows.map((row, index) => (
            <tr key={`${row.billNo}-${row.jewelleryId}`} className="border-l-4 border-blue-700 bg-blue-50/70 hover:bg-blue-50">
              <td className="px-4 py-3 text-slate-400 tabular-nums">{index + 1}</td>
              <td className="px-4 py-3 font-semibold text-amber-700">{row.billNo}</td>
              <td className="px-4 py-3 font-medium text-slate-900">{row.typeName}</td>
              <td className="px-4 py-3 text-slate-500">{row.designName || "-"}</td>
              <td className="px-4 py-3 font-mono text-xs">{row.karat}</td>
              <td className="px-4 py-3 tabular-nums">{formatWeight(row.weight)}</td>
              <td className="px-4 py-3 font-semibold">{formatMoney(row.finalPrice, row.currencyCode)}</td>
              <td className="px-4 py-3 font-mono text-xs text-slate-500">{shortId(row.jewelleryId)}</td>
              <td className="px-4 py-3 max-w-[180px] truncate text-slate-500">{row.notes || "-"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function CategoryBadge({ category }: { category: string }) {
  const label = categoryLabel(category);
  return (
    <span className={cn("inline-flex rounded-full px-2 py-1 text-[11px] font-semibold", badgeShade(category))}>
      {label}
    </span>
  );
}

function SummaryCard({ icon, label, value, sub, color = "gold" }: { icon: React.ReactNode; label: string; value: React.ReactNode; sub?: string; color?: "gold" | "emerald" | "rose" | "slate" | "blue" }) {
  const colors = {
    gold: "bg-amber-50 text-amber-700",
    emerald: "bg-emerald-50 text-emerald-700",
    rose: "bg-rose-50 text-rose-700",
    slate: "bg-slate-100 text-slate-600",
    blue: "bg-blue-50 text-blue-700",
  };
  return (
    <Card>
      <CardContent className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-slate-500">{label}</p>
          <div className="mt-2 text-2xl font-semibold text-slate-900">{value}</div>
          {sub && <p className="mt-1 text-xs text-slate-400">{sub}</p>}
        </div>
        <div className={cn("rounded-md p-2", colors[color])}>{icon}</div>
      </CardContent>
    </Card>
  );
}

function EmptyRows({ text }: { text: string }) {
  return <div className="p-8 text-center text-sm text-slate-400">{text}</div>;
}

function rowShade(category: string) {
  if (category === "TODAY_ADDED") return "border-amber-600 bg-amber-50/80";
  if (category === "MISSING_IN_AUDIT") return "border-rose-700 bg-rose-50/80";
  return "border-slate-500 bg-slate-50/80";
}

function badgeShade(category: string) {
  if (category === "TODAY_ADDED") return "bg-amber-100 text-amber-900 ring-1 ring-amber-200";
  if (category === "MISSING_IN_AUDIT") return "bg-rose-100 text-rose-900 ring-1 ring-rose-200";
  return "bg-slate-200 text-slate-800 ring-1 ring-slate-300";
}

function categoryLabel(category: string) {
  if (category === "TODAY_ADDED") return "Today added";
  if (category === "MISSING_IN_AUDIT") return "Missing";
  return "Already available";
}

function formatResolution(value?: string | null) {
  return value ? value.replaceAll("_", " ") : "-";
}

function salesTotals(totals: { currencyCode: string; totalAmount: string }[]) {
  if (!totals.length) return formatMoney("0");
  return totals.map((total) => formatMoney(total.totalAmount, total.currencyCode)).join(" / ");
}
