"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { PageHeader } from "@/components/shared/page-header";
import { StatusBadge } from "@/components/shared/status-badge";
import { EmptyState, SkeletonBlock } from "@/components/shared/states";
import { PaginationControls } from "@/components/tables/pagination-controls";
import { jewelleryApi, shopApi } from "@/lib/api/queries";
import type { JewelleryStatus } from "@/lib/api/types";
import { formatDate, formatWeight } from "@/lib/utils/format";
import { Lock, StoreIcon } from "lucide-react";

export function JewelleryListPage() {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<JewelleryStatus | "">("");
  const [typeId, setTypeId] = useState("");
  const [karat, setKarat] = useState("");
  const [q, setQ] = useState("");
  const [minWeight, setMinWeight] = useState("");
  const [maxWeight, setMaxWeight] = useState("");
  const size = 10;

  const types = useQuery({ queryKey: ["jewellery-types"], queryFn: jewelleryApi.types });
  const jewellery = useQuery({
    queryKey: ["jewellery", page, status, typeId, karat, q, minWeight, maxWeight],
    queryFn: () => jewelleryApi.list({ page, size, status, typeId: typeId || undefined, karat: karat || undefined, q: q || undefined, minWeight: minWeight || undefined, maxWeight: maxWeight || undefined }),
  });
  const shopState = useQuery({ queryKey: ["shop-state"], queryFn: () => shopApi.state() });
  const shopClosed = shopState.data?.status === "CLOSED";

  return (
    <div className="grid gap-6">
      <PageHeader
        title="Jewellery"
        description="Manage stock items, statuses, and QR labels."
        actions={
          shopClosed ? (
            <Button disabled title="Open the shop before adding jewellery">
              <Lock className="h-4 w-4" />
              Add jewellery
            </Button>
          ) : (
            <Button asChild>
              <Link href="/dashboard/jewellery/create">
                <Plus className="h-4 w-4" />
                Add jewellery
              </Link>
            </Button>
          )
        }
      />
      {shopClosed && (
        <div className="flex items-start gap-3 rounded-xl border border-rose-200 bg-rose-50 p-4">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-rose-100">
            <StoreIcon className="h-4 w-4 text-rose-600" />
          </div>
          <div>
            <p className="text-sm font-semibold text-rose-800">Shop is currently closed</p>
            <p className="mt-0.5 text-sm text-rose-700">
              Adding jewellery is blocked while the shop is closed. Go to the{" "}
              <Link href="/dashboard" className="font-semibold underline underline-offset-2">Dashboard</Link>
              {" "}to reopen the shop.
            </p>
          </div>
        </div>
      )}
      <Card className="p-4">
        <div className="grid gap-3 md:grid-cols-6 items-center">
          <Select
            value={status}
            onChange={(event) => {
              setPage(0);
              setStatus(event.target.value as JewelleryStatus | "");
            }}
            className="md:col-span-2 lg:col-span-1"
          >
            <option value="">All statuses</option>
            <option value="AVAILABLE">Available</option>
            <option value="SOLD">Sold</option>
            <option value="MISSING">Missing</option>
          </Select>
          <Select
            value={typeId}
            onChange={(event) => {
              setPage(0);
              setTypeId(event.target.value);
            }}
            className="md:col-span-2 lg:col-span-1"
          >
            <option value="">All types</option>
            {types.data?.map((type) => (
              <option key={type.id} value={type.id}>
                {type.name}
              </option>
            ))}
          </Select>
          <Input
            placeholder="Karat"
            value={karat}
            onChange={(event) => {
              setPage(0);
              setKarat(event.target.value.toUpperCase());
            }}
            className="md:col-span-2 lg:col-span-1"
          />
          <Input
            type="number"
            placeholder="Min Weight"
            value={minWeight}
            onChange={(event) => {
              setPage(0);
              setMinWeight(event.target.value);
            }}
            className="md:col-span-3 lg:col-span-1"
          />
          <Input
            type="number"
            placeholder="Max Weight"
            value={maxWeight}
            onChange={(event) => {
              setPage(0);
              setMaxWeight(event.target.value);
            }}
            className="md:col-span-3 lg:col-span-1"
          />
          <Input
            placeholder="Search bill no, notes..."
            value={q}
            onChange={(event) => {
              setPage(0);
              setQ(event.target.value);
            }}
            className="md:col-span-6 lg:col-span-1"
          />
        </div>
      </Card>
      <Card className="overflow-hidden">
        {jewellery.isLoading ? (
          <div className="p-4">
            <SkeletonBlock className="h-72" />
          </div>
        ) : jewellery.data?.content.length ? (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-100 text-xs uppercase text-slate-500">
                  <tr>
                    <th className="px-4 py-3">Type</th>
                    <th className="px-4 py-3">Sub-type</th>
                    <th className="px-4 py-3">Karat</th>
                    <th className="px-4 py-3">Weight</th>
                    <th className="px-4 py-3">Status</th>
                    <th className="px-4 py-3">Bill no</th>
                    <th className="px-4 py-3">Added Date</th>
                    <th className="px-4 py-3">Sold Date</th>
                    <th className="px-4 py-3">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {jewellery.data.content.map((item) => (
                    <tr key={item.id} className="hover:bg-slate-50">
                      <td className="px-4 py-3">
                        <Link className="font-semibold text-amber-800" href={`/dashboard/jewellery/${item.id}`}>
                          {item.typeName}
                        </Link>
                      </td>
                      <td className="px-4 py-3">{item.designName || "-"}</td>
                      <td className="px-4 py-3">{item.karat}</td>
                      <td className="px-4 py-3">{formatWeight(item.weight)}</td>
                      <td className="px-4 py-3">
                        <StatusBadge value={item.status} />
                      </td>
                      <td className="px-4 py-3 text-slate-600">
                        {item.billNo && item.billId ? (
                          <Link className="font-semibold text-amber-800 hover:underline" href={`/dashboard/billing/${item.billId}`}>
                            {item.billNo}
                          </Link>
                        ) : (
                          item.billNo || "-"
                        )}
                      </td>
                      <td className="px-4 py-3 text-slate-600">{formatDate(item.createdAt)}</td>
                      <td className="px-4 py-3 text-slate-600">{formatDate(item.soldAt)}</td>
                      <td className="px-4 py-3">
                        <Button asChild variant="outline" size="sm">
                          <Link href={`/dashboard/jewellery/${item.id}`}>View</Link>
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <PaginationControls page={page} totalPages={jewellery.data.totalPages} onPageChange={setPage} />
          </>
        ) : (
          <div className="p-6">
            <EmptyState title="No jewellery found" description="Add stock or adjust filters." />
          </div>
        )}
      </Card>
    </div>
  );
}
