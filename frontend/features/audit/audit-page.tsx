"use client";

import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { AlertTriangle, CheckCircle2, Download, Eye, Play } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { PageHeader } from "@/components/shared/page-header";
import { PasswordConfirmDialog } from "@/components/shared/password-confirm-dialog";
import { StatusBadge } from "@/components/shared/status-badge";
import { PaginationControls } from "@/components/tables/pagination-controls";
import { auditApi, shopApi } from "@/lib/api/queries";
import { downloadBackendFile, viewBackendFile } from "@/lib/api/downloads";
import { formatDate, todayIsoDate } from "@/lib/utils/format";

export function AuditPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [auditDate, setAuditDate] = useState(todayIsoDate());
  const [ownerPasswords, setOwnerPasswords] = useState<Record<string, string>>({});
  const [forceReasons, setForceReasons] = useState<Record<string, string>>({});
  const [repeatReason, setRepeatReason] = useState("");
  const audits = useQuery({ queryKey: ["audits", page], queryFn: () => auditApi.list({ page, size: 10 }) });
  const shopState = useQuery({ queryKey: ["shop-state", auditDate], queryFn: () => shopApi.state(auditDate) });
  const auditsForDate = audits.data?.content.filter((audit) => audit.auditDate === auditDate) || [];
  const nextRunNumber = auditsForDate.reduce((max, audit) => Math.max(max, audit.runNumber), 0) + 1;
  const isRepeatAudit = nextRunNumber > 1;
  const canStartAudit = shopState.data?.status === "CLOSED";
  const start = useMutation({
    mutationFn: ({ password, reason }: { password?: string; reason?: string }) => auditApi.start(auditDate, password, reason),
    onSuccess: (audit) => {
      queryClient.invalidateQueries({ queryKey: ["audits"] });
      toast.success(`${audit.auditName} started`);
    },
  });
  const attemptClose = useMutation({
    mutationFn: (auditId: string) => auditApi.attemptClose(auditId),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ["audits"] });
      if (result.canCloseCleanly) {
        toast.success("Audit closed cleanly");
      } else {
        toast.warning(`${result.unresolvedItems.length} items are still unresolved. Continue scanning or force close with a reason.`);
      }
    },
  });
  const forceClose = useMutation({
    mutationFn: ({ auditId, password, reason }: { auditId: string; password: string; reason: string }) => auditApi.forceClose(auditId, { password, reason }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["audits"] });
      toast.success("Audit force closed");
    },
  });

  function setOwnerPassword(auditId: string, value: string) {
    setOwnerPasswords((current) => ({ ...current, [auditId]: value }));
  }

  function setForceReason(auditId: string, value: string) {
    setForceReasons((current) => ({ ...current, [auditId]: value }));
  }

  return (
    <div className="grid gap-6">
      <PageHeader
        title="Daily stock audit"
        description="Start, scan, close, and download daily audit reports."
        actions={
          <>
            <Button asChild variant="outline">
              <Link href="/dashboard/audit/scan">Scan QR</Link>
            </Button>
            <Button asChild variant="outline">
              <Link href="/dashboard/audit/report">Reports</Link>
            </Button>
          </>
        }
      />
      <Card className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center">
        <Input type="date" value={auditDate} onChange={(event) => setAuditDate(event.target.value)} />
        <div className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
          Shop is {shopState.data?.status || "checking"}. Audits can start only after shop close.
        </div>
        {isRepeatAudit ? (
          <>
            <Input
              placeholder="Repeat audit reason"
              value={repeatReason}
              onChange={(event) => setRepeatReason(event.target.value)}
            />
            <PasswordConfirmDialog
              trigger={
                <Button disabled={start.isPending || !canStartAudit || !repeatReason.trim()}>
                  <Play className="h-4 w-4" />
                  {start.isPending ? "Starting..." : `Start Audit #${nextRunNumber}`}
                </Button>
              }
              title={`Start Audit #${nextRunNumber}?`}
              description={`There is already ${auditsForDate.length === 1 ? "1 audit" : `${auditsForDate.length} audits`} for ${auditDate}. Enter your password to confirm this extra audit. The run number, reason, and audit ID will be saved in business logs.`}
              passwordLabel="Your password"
              confirmLabel={`Start Audit #${nextRunNumber}`}
              isPending={start.isPending}
              onConfirm={(password) => start.mutate({ password, reason: repeatReason })}
            />
          </>
        ) : (
          <Button disabled={start.isPending || !canStartAudit} onClick={() => start.mutate({})}>
            <Play className="h-4 w-4" />
            {start.isPending ? "Starting..." : "Start audit"}
          </Button>
        )}
        {isRepeatAudit ? (
          <div className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
            Starting another audit for this date will be recorded as Audit #{nextRunNumber}.
          </div>
        ) : null}
      </Card>
      <Card className="overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-100 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Date</th>
                <th className="px-4 py-3">Audit</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Scanned</th>
                <th className="px-4 py-3">Missing</th>
                <th className="px-4 py-3">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {audits.data?.content.map((audit) => (
                <tr key={audit.id}>
                  <td className="px-4 py-3">{formatDate(audit.auditDate)}</td>
                  <td className="px-4 py-3 font-semibold text-amber-800">{audit.auditName || `Audit #${audit.runNumber}`}</td>
                  <td className="px-4 py-3">
                    <StatusBadge value={audit.status} />
                  </td>
                  <td className="px-4 py-3">
                    {audit.scannedItems}/{audit.totalItems}
                  </td>
                  <td className="px-4 py-3">{audit.missingItems}</td>
                  <td className="px-4 py-3">
                    <div className="flex min-w-80 flex-wrap items-center gap-2">
                      {audit.status === "OPEN" && audit.missingItems > 0 ? (
                        <>
                          <Input
                            type="password"
                            className="h-8 w-44"
                            placeholder="Owner/manager password"
                            value={ownerPasswords[audit.id] || ""}
                            onChange={(event) => setOwnerPassword(audit.id, event.target.value)}
                          />
                          <Input
                            className="h-8 w-56"
                            placeholder="Force close reason"
                            value={forceReasons[audit.id] || ""}
                            onChange={(event) => setForceReason(audit.id, event.target.value)}
                          />
                        </>
                      ) : null}
                      {audit.status === "OPEN" ? (
                        <Button
                          variant="secondary"
                          size="sm"
                          disabled={attemptClose.isPending}
                          onClick={() => attemptClose.mutate(audit.id)}
                        >
                          <CheckCircle2 className="h-4 w-4" />
                          Attempt close
                        </Button>
                      ) : null}
                      {audit.status === "OPEN" && audit.missingItems > 0 ? (
                        <Button
                          variant="danger"
                          size="sm"
                          disabled={forceClose.isPending || !ownerPasswords[audit.id] || !forceReasons[audit.id]?.trim()}
                          onClick={() => forceClose.mutate({ auditId: audit.id, password: ownerPasswords[audit.id], reason: forceReasons[audit.id] })}
                        >
                          <AlertTriangle className="h-4 w-4" />
                          Force close
                        </Button>
                      ) : null}
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() =>
                          viewBackendFile(`/audit/${audit.id}/pdf`).catch((error) =>
                            toast.error(error instanceof Error ? error.message : "PDF preview failed"),
                          )
                        }
                      >
                        <Eye className="h-4 w-4" />
                        View
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() =>
                          downloadBackendFile(`/audit/${audit.id}/pdf`, `audit-${audit.auditDate}.pdf`).catch((error) =>
                            toast.error(error instanceof Error ? error.message : "PDF download failed"),
                          )
                        }
                      >
                        <Download className="h-4 w-4" />
                        PDF
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {audits.data ? <PaginationControls page={page} totalPages={audits.data.totalPages} onPageChange={setPage} /> : null}
      </Card>
    </div>
  );
}
