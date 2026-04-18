"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Boxes, CalendarDays, ClipboardCheck, Gem, Plus, Scale, ShoppingBag } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { StatCard } from "@/components/shared/stat-card";
import { SkeletonBlock } from "@/components/shared/states";
import { SalesTrendChart } from "@/components/charts/sales-trend-chart";
import { analyticsApi } from "@/lib/api/queries";
import { formatWeight, todayIsoDate } from "@/lib/utils/format";
import type { AnalyticsRange } from "@/lib/api/types";
import { cn } from "@/lib/utils/cn";

// ─── Range config ─────────────────────────────────────────────────────────────

const PRESETS: { label: string; value: AnalyticsRange }[] = [
  { label: "This week", value: "THIS_WEEK" },
  { label: "This month", value: "THIS_MONTH" },
  { label: "Last 3 months", value: "LAST_3_MONTHS" },
  { label: "All time", value: "ALL_TIME" },
  { label: "Custom", value: "CUSTOM" },
];

function rangeLabel(range: AnalyticsRange, from?: string, to?: string) {
  if (range === "CUSTOM" && from && to) return `${from} → ${to}`;
  return PRESETS.find((p) => p.value === range)?.label ?? "";
}

// ─── Time range bar ───────────────────────────────────────────────────────────

function TimeRangeBar({
  range,
  customFrom,
  customTo,
  onRangeChange,
  onCustomChange,
}: {
  range: AnalyticsRange;
  customFrom: string;
  customTo: string;
  onRangeChange: (r: AnalyticsRange) => void;
  onCustomChange: (from: string, to: string) => void;
}) {
  const [draftFrom, setDraftFrom] = useState(customFrom);
  const [draftTo, setDraftTo] = useState(customTo);

  function applyCustom() {
    if (draftFrom && draftTo && draftFrom <= draftTo) {
      onCustomChange(draftFrom, draftTo);
    }
  }

  return (
    <div className="flex flex-col gap-3">
      {/* Pill group */}
      <div className="flex flex-wrap gap-1.5">
        {PRESETS.map((preset) => (
          <button
            key={preset.value}
            type="button"
            onClick={() => onRangeChange(preset.value)}
            className={cn(
              "inline-flex items-center gap-1.5 rounded-full px-3.5 py-1.5 text-xs font-medium transition-all",
              range === preset.value
                ? "bg-amber-600 text-white shadow-sm"
                : "bg-white text-slate-600 border border-slate-200 hover:border-amber-300 hover:text-amber-700",
            )}
          >
            {preset.value === "CUSTOM" && <CalendarDays className="h-3.5 w-3.5" />}
            {preset.label}
          </button>
        ))}
      </div>

      {/* Custom date inputs */}
      {range === "CUSTOM" && (
        <div className="flex flex-wrap items-end gap-2 rounded-lg border border-amber-200 bg-amber-50 p-3">
          <div className="flex flex-col gap-1">
            <label className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">From</label>
            <Input
              type="date"
              value={draftFrom}
              max={draftTo || todayIsoDate()}
              onChange={(e) => setDraftFrom(e.target.value)}
              className="h-8 w-36 text-xs"
            />
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">To</label>
            <Input
              type="date"
              value={draftTo}
              min={draftFrom}
              max={todayIsoDate()}
              onChange={(e) => setDraftTo(e.target.value)}
              className="h-8 w-36 text-xs"
            />
          </div>
          <Button
            size="sm"
            disabled={!draftFrom || !draftTo || draftFrom > draftTo}
            onClick={applyCustom}
            className="h-8"
          >
            Apply
          </Button>
        </div>
      )}
    </div>
  );
}

// ─── Dashboard page ───────────────────────────────────────────────────────────

export function DashboardPage() {
  const [range, setRange] = useState<AnalyticsRange>("THIS_MONTH");
  const [customFrom, setCustomFrom] = useState("");
  const [customTo, setCustomTo] = useState("");

  const canFetch = range !== "CUSTOM" || (!!customFrom && !!customTo && customFrom <= customTo);

  const summary = useQuery({
    queryKey: ["analytics", "dashboard", range, customFrom, customTo],
    queryFn: () =>
      analyticsApi.summary({
        range,
        from: range === "CUSTOM" ? customFrom : undefined,
        to: range === "CUSTOM" ? customTo : undefined,
      }),
    enabled: canFetch,
  });

  function handleRangeChange(r: AnalyticsRange) {
    setRange(r);
  }

  function handleCustomChange(from: string, to: string) {
    setCustomFrom(from);
    setCustomTo(to);
  }

  const detailLabel = rangeLabel(range, customFrom, customTo);

  return (
    <div className="space-y-6">
      {/* Hero banner */}
      <section className="rounded-xl border border-amber-200 bg-gradient-to-br from-amber-50 to-orange-50 p-5 shadow-sm">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-widest text-amber-600">JewellKeeper</p>
            <h1 className="mt-1.5 text-xl font-semibold text-slate-900">Good to see you. Here&apos;s your stock snapshot.</h1>
            <p className="mt-1 text-sm text-slate-500">Bills, QR labels, and audit — all in one place.</p>
          </div>
          <div className="flex flex-wrap gap-2 shrink-0">
            <Button asChild>
              <Link href="/dashboard/jewellery/create">
                <Plus className="h-4 w-4" />
                Add item
              </Link>
            </Button>
            <Button asChild variant="outline">
              <Link href="/dashboard/audit">
                <ClipboardCheck className="h-4 w-4" />
                Close audit
              </Link>
            </Button>
          </div>
        </div>
      </section>

      {/* Time range selector */}
      <div className="space-y-1.5">
        <p className="text-xs font-semibold uppercase tracking-widest text-slate-400">Time range</p>
        <TimeRangeBar
          range={range}
          customFrom={customFrom}
          customTo={customTo}
          onRangeChange={handleRangeChange}
          onCustomChange={handleCustomChange}
        />
      </div>

      {/* Stat cards */}
      {summary.isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <SkeletonBlock />
          <SkeletonBlock />
          <SkeletonBlock />
          <SkeletonBlock />
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatCard
            label="Available stock"
            value={summary.data?.availableStockCount ?? 0}
            icon={<Boxes className="h-5 w-5" />}
          />
          <StatCard
            label="Items sold"
            value={summary.data?.totalItemsSold ?? 0}
            detail={detailLabel}
            icon={<ShoppingBag className="h-5 w-5" />}
          />
          <StatCard
            label="Weight sold"
            value={formatWeight(summary.data?.totalWeightSold)}
            detail={detailLabel}
            icon={<Scale className="h-5 w-5" />}
          />
          <StatCard
            label="Popular karat"
            value={summary.data?.mostPopularKarat || "—"}
            detail={detailLabel}
            icon={<Gem className="h-5 w-5" />}
          />
        </div>
      )}

      {/* Chart */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-sm font-semibold text-slate-900">Sales trend</h2>
              {summary.data && (
                <p className="mt-0.5 text-xs text-slate-400">
                  {summary.data.from} — {summary.data.to}
                </p>
              )}
            </div>
            {summary.data && (
              <div className="text-right">
                <p className="text-xs text-slate-400">Avg. weight / sale</p>
                <p className="text-sm font-semibold text-slate-700">{formatWeight(summary.data.averageWeightPerSoldItem)}</p>
              </div>
            )}
          </div>
        </CardHeader>
        <CardContent>
          {!canFetch ? (
            <div className="flex h-48 items-center justify-center text-sm text-slate-400">
              Select a valid date range and press Apply to load data.
            </div>
          ) : summary.isLoading ? (
            <div className="h-72 animate-pulse rounded-md bg-slate-100" />
          ) : (
            <SalesTrendChart data={summary.data?.salesChart || []} />
          )}
        </CardContent>
      </Card>
    </div>
  );
}
