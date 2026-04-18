"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Lock, Plus, StoreIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { PageHeader } from "@/components/shared/page-header";
import { EmptyState, SkeletonBlock } from "@/components/shared/states";
import { PaginationControls } from "@/components/tables/pagination-controls";
import { billingApi, shopApi } from "@/lib/api/queries";
import { formatDate, formatMoney } from "@/lib/utils/format";

export function BillingListPage() {
  const [page, setPage] = useState(0);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [q, setQ] = useState("");
  const bills = useQuery({
    queryKey: ["bills", page, from, to, q],
    queryFn: () => billingApi.list({ page, size: 10, from: from || undefined, to: to || undefined, q: q || undefined }),
  });
  const shopState = useQuery({ queryKey: ["shop-state"], queryFn: () => shopApi.state() });
  const shopClosed = shopState.data?.status === "CLOSED";

  const createButton = shopClosed ? (
    <Button disabled title="Open the shop before creating bills">
      <Lock className="h-4 w-4" />
      Create bill
    </Button>
  ) : (
    <Button asChild>
      <Link href="/dashboard/billing/create">
        <Plus className="h-4 w-4" />
        Create bill
      </Link>
    </Button>
  );

  return (
    <div className="grid gap-6">
      <PageHeader
        title="Billing"
        description="Create bills with dynamic jewellery prices."
        actions={createButton}
      />
      {shopClosed && (
        <div className="flex items-start gap-3 rounded-xl border border-rose-200 bg-rose-50 p-4">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-rose-100">
            <StoreIcon className="h-4 w-4 text-rose-600" />
          </div>
          <div>
            <p className="text-sm font-semibold text-rose-800">Shop is currently closed</p>
            <p className="mt-0.5 text-sm text-rose-700">
              Billing is disabled while the shop is closed. Go to the{" "}
              <Link href="/dashboard" className="font-semibold underline underline-offset-2">
                Dashboard
              </Link>{" "}
              to reopen the shop, then return here to create bills.
            </p>
          </div>
        </div>
      )}
      <Card className="grid gap-3 p-4 sm:grid-cols-2 md:grid-cols-3">
        <Input
          placeholder="Search customer name or bill no"
          value={q}
          onChange={(event) => {
            setPage(0);
            setQ(event.target.value);
          }}
        />
        <Input
          type="date"
          value={from}
          onChange={(event) => {
            setPage(0);
            setFrom(event.target.value);
          }}
        />
        <Input
          type="date"
          value={to}
          onChange={(event) => {
            setPage(0);
            setTo(event.target.value);
          }}
        />
      </Card>
      <Card className="overflow-hidden">
        {bills.isLoading ? (
          <div className="p-4">
            <SkeletonBlock className="h-72" />
          </div>
        ) : bills.data?.content.length ? (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-100 text-xs uppercase text-slate-500">
                  <tr>
                    <th className="px-4 py-3">Bill no</th>
                    <th className="px-4 py-3">Date</th>
                    <th className="px-4 py-3">Customer</th>
                    <th className="px-4 py-3">Total</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {bills.data.content.map((bill) => (
                    <tr key={bill.id} className="hover:bg-slate-50">
                      <td className="px-4 py-3">
                        <Link className="font-semibold text-amber-800" href={`/dashboard/billing/${bill.id}`}>
                          {bill.billNo}
                        </Link>
                      </td>
                      <td className="px-4 py-3">{formatDate(bill.billDate)}</td>
                      <td className="px-4 py-3">{bill.customerName || "-"}</td>
                      <td className="px-4 py-3 font-semibold">{formatMoney(bill.totalAmount, bill.currencyCode)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <PaginationControls page={page} totalPages={bills.data.totalPages} onPageChange={setPage} />
          </>
        ) : (
          <div className="p-6">
            <EmptyState title="No bills found" description="Create a bill after selling jewellery." />
          </div>
        )}
      </Card>
    </div>
  );
}
