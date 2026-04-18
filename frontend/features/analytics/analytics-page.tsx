"use client";

import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  AlertTriangle,
  ArrowDownRight,
  ArrowUpRight,
  Boxes,
  CalendarDays,
  Coins,
  Gauge,
  Lightbulb,
  PackageSearch,
  ReceiptText,
  Scale,
  ShoppingBag,
  Target,
} from "lucide-react";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { PageHeader } from "@/components/shared/page-header";
import { SkeletonBlock } from "@/components/shared/states";
import { analyticsApi } from "@/lib/api/queries";
import type { AnalyticsPerformanceRow, AnalyticsRange, AnalyticsSummary } from "@/lib/api/types";
import { formatDate, formatMoney, formatWeight } from "@/lib/utils/format";
import { cn } from "@/lib/utils/cn";

const COLORS = ["#b45309", "#047857", "#be123c", "#334155", "#ca8a04", "#0369a1"];

export function AnalyticsPage() {
  const [range, setRange] = useState<AnalyticsRange>("THIS_MONTH");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const canFetch = range !== "CUSTOM" || (Boolean(from) && Boolean(to));

  const summary = useQuery({
    queryKey: ["analytics", range, from, to],
    queryFn: () => analyticsApi.summary({ range, from: from || undefined, to: to || undefined }),
    enabled: canFetch,
  });

  const data = summary.data;
  const growth = Number(data?.salesGrowthPercent ?? 0);

  const trendData = useMemo(
    () =>
      (data?.salesChart ?? []).map((point) => ({
        date: point.date.slice(5),
        amount: Number(point.totalAmount || 0),
        items: point.itemCount,
      })),
    [data],
  );

  const typeChartData = useMemo(
    () =>
      (data?.typePerformance ?? []).slice(0, 8).map((row) => ({
        name: row.name,
        sold: row.itemsSold,
        available: row.availableCount,
        sales: Number(row.salesAmount || 0),
      })),
    [data],
  );

  const ageChartData = useMemo(
    () => (data?.inventoryAgeBuckets ?? []).map((bucket) => ({ name: bucket.label, value: bucket.itemCount })),
    [data],
  );

  return (
    <div className="space-y-6">
      <PageHeader
        title="Owner analytics"
        description="Decision dashboard for sales momentum, buying focus, aging stock, missing risk, and cash movement."
      />

      <Card className="border-amber-100 bg-gradient-to-br from-white via-white to-amber-50/60">
        <CardContent className="grid gap-3 p-4 lg:grid-cols-[220px_180px_180px_1fr]">
          <Select value={range} onChange={(event) => setRange(event.target.value as AnalyticsRange)}>
            <option value="THIS_WEEK">This week</option>
            <option value="THIS_MONTH">This month</option>
            <option value="LAST_3_MONTHS">Last 3 months</option>
            <option value="ALL_TIME">All time</option>
            <option value="CUSTOM">Custom range</option>
          </Select>
          {range === "CUSTOM" ? (
            <>
              <Input type="date" value={from} onChange={(event) => setFrom(event.target.value)} />
              <Input type="date" value={to} onChange={(event) => setTo(event.target.value)} />
            </>
          ) : (
            <>
              <div className="hidden lg:block" />
              <div className="hidden lg:block" />
            </>
          )}
          <div className="flex items-center gap-2 rounded-md border border-amber-100 bg-white px-3 py-2 text-sm text-slate-600">
            <CalendarDays className="h-4 w-4 text-amber-700" />
            {data ? `${formatDate(data.from)} to ${formatDate(data.to)}` : range === "CUSTOM" ? "Choose both dates" : "Loading period"}
          </div>
        </CardContent>
      </Card>

      {!canFetch ? (
        <Card>
          <CardContent className="p-8 text-sm text-slate-500">Choose a start and end date to view custom analytics.</CardContent>
        </Card>
      ) : summary.isLoading ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <SkeletonBlock /><SkeletonBlock /><SkeletonBlock /><SkeletonBlock />
        </div>
      ) : data ? (
        <>
          <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <MetricCard
              icon={<Coins className="h-5 w-5" />}
              label="Revenue"
              value={formatMoney(data.totalSalesAmount, data.currencyCode)}
              detail={`${formatGrowth(growth)} vs previous period`}
              tone={growth >= 0 ? "good" : "danger"}
            />
            <MetricCard
              icon={<ReceiptText className="h-5 w-5" />}
              label="Average bill value"
              value={formatMoney(data.averageBillValue, data.currencyCode)}
              detail={`${data.billCount} bill${data.billCount === 1 ? "" : "s"} · ${data.itemsPerBill} items/bill`}
            />
            <MetricCard
              icon={<Scale className="h-5 w-5" />}
              label="Sold weight"
              value={formatWeight(data.totalWeightSold)}
              detail={`${data.totalItemsSold} item${data.totalItemsSold === 1 ? "" : "s"} sold`}
            />
            <MetricCard
              icon={<Gauge className="h-5 w-5" />}
              label="Sell-through"
              value={`${data.sellThroughRate}%`}
              detail={`${coverageText(data.stockCoverageDays)} stock coverage`}
              tone={Number(data.sellThroughRate) >= 35 ? "good" : "neutral"}
            />
          </section>

          <section className="grid gap-4 md:grid-cols-3">
            <MetricCard
              icon={<Boxes className="h-5 w-5" />}
              label="Available stock"
              value={data.availableStockCount}
              detail={formatWeight(data.availableStockWeight)}
            />
            <MetricCard
              icon={<AlertTriangle className="h-5 w-5" />}
              label="Missing stock"
              value={data.missingStockCount}
              detail={formatWeight(data.missingStockWeight)}
              tone={data.missingStockCount > 0 ? "danger" : "good"}
            />
            <MetricCard
              icon={<Target className="h-5 w-5" />}
              label="Current demand signal"
              value={data.bestSellingItemType || "-"}
              detail={`Popular karat in stock: ${data.mostPopularKarat || "-"}`}
            />
          </section>

          <section className="grid gap-6 xl:grid-cols-[minmax(0,1.2fr)_minmax(360px,0.8fr)]">
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <h2 className="font-semibold text-slate-900">Sales momentum</h2>
                    <p className="text-sm text-slate-500">Daily revenue and sold item count in {data.currencyCode}.</p>
                  </div>
                  <GrowthBadge value={growth} />
                </div>
              </CardHeader>
              <CardContent>
                <div className="h-80">
                  <ResponsiveContainer>
                    <AreaChart data={trendData} margin={{ left: 0, right: 16, top: 10, bottom: 0 }}>
                      <defs>
                        <linearGradient id="analyticsSales" x1="0" x2="0" y1="0" y2="1">
                          <stop offset="5%" stopColor="#b45309" stopOpacity={0.3} />
                          <stop offset="95%" stopColor="#b45309" stopOpacity={0} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                      <XAxis dataKey="date" tick={{ fontSize: 12 }} />
                      <YAxis tick={{ fontSize: 12 }} />
                      <Tooltip formatter={(value, name) => name === "amount" ? formatMoney(value as number, data.currencyCode) : value} />
                      <Area type="monotone" dataKey="amount" stroke="#b45309" fill="url(#analyticsSales)" strokeWidth={2} />
                    </AreaChart>
                  </ResponsiveContainer>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <h2 className="font-semibold text-slate-900">Owner decision notes</h2>
                <p className="text-sm text-slate-500">Actions generated from movement, stock accuracy, and aging signals.</p>
              </CardHeader>
              <CardContent className="space-y-3">
                {data.insights.length === 0 ? (
                  <p className="rounded-md border border-slate-100 bg-slate-50 p-4 text-sm text-slate-500">
                    More bills, stock movements, and closed audits will make the recommendations sharper.
                  </p>
                ) : (
                  data.insights.map((insight) => <InsightCard key={`${insight.title}-${insight.severity}`} insight={insight} />)
                )}
              </CardContent>
            </Card>
          </section>

          <section className="grid gap-6 xl:grid-cols-2">
            <Card>
              <CardHeader>
                <h2 className="font-semibold text-slate-900">Type performance</h2>
                <p className="text-sm text-slate-500">Use this for buying depth, display focus, and promotion decisions.</p>
              </CardHeader>
              <CardContent>
                <div className="h-72">
                  <ResponsiveContainer>
                    <BarChart data={typeChartData} margin={{ left: 0, right: 16, top: 12, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                      <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                      <YAxis tick={{ fontSize: 12 }} />
                      <Tooltip />
                      <Bar dataKey="sold" fill="#b45309" radius={[4, 4, 0, 0]} />
                      <Bar dataKey="available" fill="#047857" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
                <PerformanceTable rows={data.typePerformance} currency={data.currencyCode} />
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <h2 className="font-semibold text-slate-900">Karat performance</h2>
                <p className="text-sm text-slate-500">Match replenishment to metal purity demand, not only total stock.</p>
              </CardHeader>
              <CardContent>
                <PerformanceTable rows={data.karatPerformance} currency={data.currencyCode} />
              </CardContent>
            </Card>
          </section>

          <section className="grid gap-6 xl:grid-cols-[420px_minmax(0,1fr)]">
            <Card>
              <CardHeader>
                <h2 className="font-semibold text-slate-900">Inventory aging</h2>
                <p className="text-sm text-slate-500">Older available stock ties up capital and display attention.</p>
              </CardHeader>
              <CardContent>
                <div className="h-72">
                  <ResponsiveContainer>
                    <PieChart>
                      <Pie data={ageChartData} dataKey="value" nameKey="name" innerRadius={58} outerRadius={98} paddingAngle={2}>
                        {ageChartData.map((entry, index) => <Cell key={entry.name} fill={COLORS[index % COLORS.length]} />)}
                      </Pie>
                      <Tooltip />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
                <div className="grid gap-2">
                  {data.inventoryAgeBuckets.map((bucket, index) => (
                    <div key={bucket.label} className="flex items-center justify-between rounded-md border border-slate-100 px-3 py-2 text-sm">
                      <span className="flex items-center gap-2">
                        <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: COLORS[index % COLORS.length] }} />
                        {bucket.label}
                      </span>
                      <span className="font-medium text-slate-700">{bucket.itemCount} · {formatWeight(bucket.totalWeight)}</span>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <h2 className="font-semibold text-slate-900">Slow-moving stock watchlist</h2>
                <p className="text-sm text-slate-500">Available items older than 90 days, oldest first.</p>
              </CardHeader>
              <CardContent>
                {data.slowMovingItems.length === 0 ? (
                  <EmptyState icon={<PackageSearch className="h-5 w-5" />} text="No available stock older than 90 days." />
                ) : (
                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                      <thead className="border-b border-slate-100 text-xs uppercase tracking-wide text-slate-500">
                        <tr>
                          <th className="py-2 pr-3">Item</th>
                          <th className="px-3 py-2">Karat</th>
                          <th className="px-3 py-2">Weight</th>
                          <th className="px-3 py-2">Age</th>
                          <th className="px-3 py-2">Added</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100">
                        {data.slowMovingItems.map((item) => (
                          <tr key={item.jewelleryId}>
                            <td className="py-3 pr-3">
                              <p className="font-medium text-slate-900">{item.typeName}</p>
                              <p className="text-xs text-slate-500">{item.designName || "No design name"}</p>
                            </td>
                            <td className="px-3 py-3">{item.karat}</td>
                            <td className="px-3 py-3">{formatWeight(item.weight)}</td>
                            <td className="px-3 py-3 font-semibold text-amber-800">{item.ageDays}d</td>
                            <td className="px-3 py-3 text-slate-500">{formatDate(item.createdAt)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </CardContent>
            </Card>
          </section>

          <section className="grid gap-6 xl:grid-cols-2">
            <Card>
              <CardHeader>
                <h2 className="font-semibold text-slate-900">Currency split</h2>
                <p className="text-sm text-slate-500">Money values are separated so LKR, CAD, EUR, and AUD are not mixed.</p>
              </CardHeader>
              <CardContent className="space-y-2">
                {data.salesByCurrency.length === 0 ? (
                  <EmptyState icon={<Coins className="h-5 w-5" />} text="No sales in this period." />
                ) : (
                  data.salesByCurrency.map((row) => (
                    <div key={row.currencyCode} className="grid gap-2 rounded-md border border-slate-100 p-3 sm:grid-cols-3">
                      <div>
                        <p className="text-xs uppercase tracking-wide text-slate-400">Currency</p>
                        <p className="font-semibold text-slate-900">{row.currencyCode}</p>
                      </div>
                      <div>
                        <p className="text-xs uppercase tracking-wide text-slate-400">Revenue</p>
                        <p className="font-semibold text-slate-900">{formatMoney(row.totalAmount, row.currencyCode)}</p>
                      </div>
                      <div>
                        <p className="text-xs uppercase tracking-wide text-slate-400">AOV</p>
                        <p className="font-semibold text-slate-900">{formatMoney(row.averageBillValue, row.currencyCode)}</p>
                      </div>
                    </div>
                  ))
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <h2 className="font-semibold text-slate-900">How to read this</h2>
              </CardHeader>
              <CardContent className="grid gap-3 text-sm text-slate-600">
                <ReadNote icon={<ShoppingBag className="h-4 w-4" />} text="High sales with low available stock points to reorder depth, especially when the same karat is also moving." />
                <ReadNote icon={<PackageSearch className="h-4 w-4" />} text="High available stock with low sales suggests a display, pricing, or design-fit problem before buying more." />
                <ReadNote icon={<AlertTriangle className="h-4 w-4" />} text="Missing stock makes inventory KPIs less trustworthy; resolve audit exceptions before making purchase decisions." />
                <ReadNote icon={<Lightbulb className="h-4 w-4" />} text={data.methodologyNote} />
              </CardContent>
            </Card>
          </section>
        </>
      ) : null}
    </div>
  );
}

function MetricCard({
  icon,
  label,
  value,
  detail,
  tone = "neutral",
}: {
  icon: React.ReactNode;
  label: string;
  value: React.ReactNode;
  detail?: React.ReactNode;
  tone?: "neutral" | "good" | "danger";
}) {
  const tones = {
    neutral: "bg-amber-50 text-amber-700",
    good: "bg-emerald-50 text-emerald-700",
    danger: "bg-rose-50 text-rose-700",
  };
  return (
    <Card>
      <CardContent className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-slate-500">{label}</p>
          <div className="mt-2 text-2xl font-semibold text-slate-950">{value}</div>
          {detail ? <p className="mt-1 text-xs text-slate-500">{detail}</p> : null}
        </div>
        <div className={cn("rounded-md p-2", tones[tone])}>{icon}</div>
      </CardContent>
    </Card>
  );
}

function InsightCard({ insight }: { insight: AnalyticsSummary["insights"][number] }) {
  const tone = insight.severity === "DANGER"
    ? "border-rose-200 bg-rose-50 text-rose-900"
    : insight.severity === "WARNING"
      ? "border-amber-200 bg-amber-50 text-amber-950"
      : insight.severity === "GOOD"
        ? "border-emerald-200 bg-emerald-50 text-emerald-900"
        : "border-slate-200 bg-slate-50 text-slate-800";
  return (
    <div className={cn("rounded-md border p-3", tone)}>
      <p className="text-xs font-semibold uppercase tracking-wide">{insight.severity}</p>
      <h3 className="mt-1 font-semibold">{insight.title}</h3>
      <p className="mt-1 text-sm opacity-85">{insight.message}</p>
      <p className="mt-2 text-sm font-medium">{insight.recommendedAction}</p>
    </div>
  );
}

function PerformanceTable({ rows, currency }: { rows: AnalyticsPerformanceRow[]; currency: string }) {
  if (rows.length === 0) return <EmptyState icon={<Target className="h-5 w-5" />} text="No sales performance data for this period." />;
  return (
    <div className="mt-4 overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead className="border-b border-slate-100 text-xs uppercase tracking-wide text-slate-500">
          <tr>
            <th className="py-2 pr-3">Name</th>
            <th className="px-3 py-2">Sold</th>
            <th className="px-3 py-2">Available</th>
            <th className="px-3 py-2">Sell-through</th>
            <th className="px-3 py-2">Weight</th>
            <th className="px-3 py-2">Revenue</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {rows.slice(0, 10).map((row) => (
            <tr key={row.name}>
              <td className="py-3 pr-3 font-medium text-slate-900">{row.name}</td>
              <td className="px-3 py-3">{row.itemsSold}</td>
              <td className="px-3 py-3">{row.availableCount}</td>
              <td className="px-3 py-3 font-semibold text-amber-800">{row.sellThroughRate}%</td>
              <td className="px-3 py-3">{formatWeight(row.weightSold)}</td>
              <td className="px-3 py-3">{formatMoney(row.salesAmount, currency)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function GrowthBadge({ value }: { value: number }) {
  const positive = value >= 0;
  return (
    <div className={cn(
      "inline-flex items-center gap-1 rounded-md px-2.5 py-1 text-sm font-semibold",
      positive ? "bg-emerald-50 text-emerald-700" : "bg-rose-50 text-rose-700",
    )}>
      {positive ? <ArrowUpRight className="h-4 w-4" /> : <ArrowDownRight className="h-4 w-4" />}
      {formatGrowth(value)}
    </div>
  );
}

function EmptyState({ icon, text }: { icon: React.ReactNode; text: string }) {
  return (
    <div className="flex items-center gap-2 rounded-md border border-slate-100 bg-slate-50 p-4 text-sm text-slate-500">
      <span className="text-slate-400">{icon}</span>
      {text}
    </div>
  );
}

function ReadNote({ icon, text }: { icon: React.ReactNode; text: string }) {
  return (
    <div className="flex gap-2 rounded-md border border-amber-100 bg-amber-50/50 p-3">
      <span className="mt-0.5 text-amber-700">{icon}</span>
      <p>{text}</p>
    </div>
  );
}

function formatGrowth(value: number) {
  const prefix = value > 0 ? "+" : "";
  return `${prefix}${value.toFixed(2)}%`;
}

function coverageText(value: string | number) {
  const days = Number(value || 0);
  if (days >= 999) return "999+ days";
  return `${days.toFixed(1)} days`;
}
