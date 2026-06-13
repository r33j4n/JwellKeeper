"use client";

import { FormEvent, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Archive, Lock, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { PageHeader } from "@/components/shared/page-header";
import { StatusBadge } from "@/components/shared/status-badge";
import { EmptyState, SkeletonBlock } from "@/components/shared/states";
import { PaginationControls } from "@/components/tables/pagination-controls";
import { jewelleryApi } from "@/lib/api/queries";
import { formatDate, formatWeight } from "@/lib/utils/format";

export function ArchivedJewelleryPage() {
  const [page, setPage] = useState(0);
  const [passwordInput, setPasswordInput] = useState("");
  const [ownerPassword, setOwnerPassword] = useState("");
  const [unlockVersion, setUnlockVersion] = useState(0);
  const [typeId, setTypeId] = useState("");
  const [karat, setKarat] = useState("");
  const [q, setQ] = useState("");
  const [minWeight, setMinWeight] = useState("");
  const [maxWeight, setMaxWeight] = useState("");
  const size = 10;
  const unlocked = Boolean(ownerPassword);

  const types = useQuery({ queryKey: ["jewellery-types"], queryFn: jewelleryApi.types });
  const archived = useQuery({
    queryKey: ["archived-jewellery", page, typeId, karat, q, unlockVersion, minWeight, maxWeight],
    queryFn: () => jewelleryApi.archived(
      { ownerPassword, typeId: typeId || undefined, karat: karat || undefined, q: q || undefined, minWeight: minWeight || undefined, maxWeight: maxWeight || undefined },
      { page, size },
    ),
    enabled: unlocked,
  });

  function unlock(event: FormEvent) {
    event.preventDefault();
    if (!passwordInput.trim()) return;
    setOwnerPassword(passwordInput);
    setUnlockVersion((value) => value + 1);
    setPage(0);
  }

  return (
    <div className="grid gap-6">
      <PageHeader
        title="Archived jewellery"
        description="Owner-password protected view of jewellery that has been archived or soft deleted."
      />

      {!unlocked ? (
        <Card className="max-w-xl">
          <CardHeader>
            <div className="flex items-center gap-3">
              <div className="rounded-md bg-amber-50 p-2 text-amber-700">
                <Lock className="h-5 w-5" />
              </div>
              <div>
                <h2 className="font-semibold text-slate-900">Owner verification required</h2>
                <p className="text-sm text-slate-500">Archived stock can contain sensitive correction history, so it is hidden until the owner password is provided.</p>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <form className="flex flex-col gap-3 sm:flex-row" onSubmit={unlock}>
              <Input
                type="password"
                placeholder="Owner password"
                value={passwordInput}
                onChange={(event) => setPasswordInput(event.target.value)}
              />
              <Button disabled={!passwordInput.trim()}>
                <Archive className="h-4 w-4" />
                View archived
              </Button>
            </form>
          </CardContent>
        </Card>
      ) : (
        <>
          <Card>
            <CardContent className="grid gap-3 p-4 md:grid-cols-6 items-center">
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
                className="md:col-span-2 lg:col-span-1"
              />
              <Input
                type="number"
                placeholder="Max Weight"
                value={maxWeight}
                onChange={(event) => {
                  setPage(0);
                  setMaxWeight(event.target.value);
                }}
                className="md:col-span-2 lg:col-span-1"
              />
              <Input
                placeholder="Search type, notes"
                value={q}
                onChange={(event) => {
                  setPage(0);
                  setQ(event.target.value);
                }}
                className="md:col-span-4 lg:col-span-1"
              />
              <Button variant="outline" onClick={() => archived.refetch()} disabled={archived.isFetching} className="md:col-span-6 lg:col-span-1">
                <Search className="h-4 w-4" />
                {archived.isFetching ? "Searching..." : "Search"}
              </Button>
            </CardContent>
          </Card>

          <Card className="overflow-hidden">
            {archived.isLoading ? (
              <div className="p-4">
                <SkeletonBlock className="h-72" />
              </div>
            ) : archived.data?.content.length ? (
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
                        <th className="px-4 py-3">Added Date</th>
                        <th className="px-4 py-3">Archived Date</th>
                        <th className="px-4 py-3">Notes</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {archived.data.content.map((item) => (
                        <tr key={item.id} className="bg-slate-50/60 hover:bg-slate-100">
                          <td className="px-4 py-3 font-semibold text-slate-900">{item.typeName}</td>
                          <td className="px-4 py-3">{item.designName || "-"}</td>
                          <td className="px-4 py-3">{item.karat}</td>
                          <td className="px-4 py-3">{formatWeight(item.weight)}</td>
                          <td className="px-4 py-3"><StatusBadge value={item.status} /></td>
                          <td className="px-4 py-3 text-slate-600">{formatDate(item.createdAt)}</td>
                          <td className="px-4 py-3 text-slate-600">{formatDate(item.deletedAt)}</td>
                          <td className="px-4 py-3 max-w-[260px] truncate text-slate-500">{item.notes || "-"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <PaginationControls page={page} totalPages={archived.data.totalPages} onPageChange={setPage} />
              </>
            ) : (
              <div className="p-6">
                <EmptyState title="No archived jewellery found" description="Archived or soft-deleted jewellery will appear here after owner verification." />
              </div>
            )}
          </Card>
        </>
      )}
    </div>
  );
}
