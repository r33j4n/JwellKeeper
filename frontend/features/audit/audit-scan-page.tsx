"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useRef, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { PageHeader } from "@/components/shared/page-header";
import { StatusBadge } from "@/components/shared/status-badge";
import { QrScanner, type QrScannerHandle } from "@/components/qr/qr-scanner";
import { auditApi } from "@/lib/api/queries";
import { playSound } from "@/lib/utils/sound";

export function AuditScanPage() {
  const queryClient = useQueryClient();
  const scannerRef = useRef<QrScannerHandle>(null);
  const audits = useQuery({ queryKey: ["audits", "scanner"], queryFn: () => auditApi.list({ size: 50 }) });
  const openAudits = useMemo(() => audits.data?.content.filter((audit) => audit.status === "OPEN") || [], [audits.data]);
  const [auditId, setAuditId] = useState("");
  const selectedAudit = openAudits.find((audit) => audit.id === auditId) || openAudits[0];

  async function stopCameraThen(action: () => void) {
    await scannerRef.current?.stop();
    action();
  }

  const scan = useMutation({
    mutationFn: (token: string) => auditApi.scan(selectedAudit.id, token),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ["audits"] });
      if (result.alreadyScanned) {
        toast.info("Already scanned");
        playSound("warning");
      } else {
        toast.success("Item scanned");
        playSound("success");
      }
    },
    onError: (error) => {
      playSound("error");
      const message = error instanceof Error ? error.message : "Item not found in audit";
      toast.error(message);
    },
  });

  const attemptClose = useMutation({
    mutationFn: () => auditApi.attemptClose(selectedAudit.id),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ["audits"] });
      if (result.canCloseCleanly) {
        toast.success("Audit closed cleanly");
      } else {
        toast.warning(`${result.unresolvedItems.length} items remain unresolved`);
      }
    },
  });
  const forceClose = useMutation({
    mutationFn: () => auditApi.forceClose(selectedAudit.id, { password: ownerPassword, reason: forceReason }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["audits"] });
      toast.success("Audit force closed");
    },
  });

  const [ownerPassword, setOwnerPassword] = useState("");
  const [forceReason, setForceReason] = useState("");

  return (
    <div className="grid gap-6">
      <PageHeader title="Scan audit QR" description="Use the QR code scanner first, then switch to the camera scanner if needed." />
      <Card>
        <CardContent className="grid gap-4 p-4 md:grid-cols-5">
          <Select value={selectedAudit?.id || ""} onChange={(event) => setAuditId(event.target.value)}>
            {openAudits.map((audit) => (
              <option key={audit.id} value={audit.id}>
                {audit.auditName || `${audit.auditDate} Audit #${audit.runNumber}`}
              </option>
            ))}
          </Select>
          {selectedAudit ? (
            <>
              <StatusBadge value={selectedAudit.status} />
              <div className="text-sm font-semibold text-amber-800">{selectedAudit.auditName || `Audit #${selectedAudit.runNumber}`}</div>
              <div className="text-sm text-slate-600">Scanned: {selectedAudit.scannedItems}</div>
              <div className="text-sm text-slate-600">Remaining: {selectedAudit.missingItems}</div>
            </>
          ) : (
            <div className="text-sm text-slate-600 md:col-span-4">No open audit found. Start an audit first.</div>
          )}
        </CardContent>
      </Card>
      {selectedAudit ? (
        <div className="grid gap-6 xl:grid-cols-[1fr_360px]">
          <Card>
            <CardHeader>
              <h2 className="font-semibold">Scan items</h2>
            </CardHeader>
            <CardContent>
              <QrScanner ref={scannerRef} onScan={(token) => scan.mutate(token)} />
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <h2 className="font-semibold">Close actions</h2>
            </CardHeader>
            <CardContent className="grid gap-3">
              {selectedAudit.missingItems > 0 ? (
                <>
                  <Input
                    type="password"
                    placeholder="Owner/manager password required if unresolved"
                    value={ownerPassword}
                    onChange={(event) => setOwnerPassword(event.target.value)}
                  />
                  <Input placeholder="Force close reason" value={forceReason} onChange={(event) => setForceReason(event.target.value)} />
                </>
              ) : null}
              <Button variant="secondary" disabled={attemptClose.isPending} onClick={() => stopCameraThen(() => attemptClose.mutate())}>
                Attempt close
              </Button>
              {selectedAudit.missingItems > 0 ? (
                <Button variant="danger" disabled={forceClose.isPending || !ownerPassword || !forceReason.trim()} onClick={() => stopCameraThen(() => forceClose.mutate())}>
                  Force close unresolved
                </Button>
              ) : null}
            </CardContent>
          </Card>
        </div>
      ) : null}
    </div>
  );
}
