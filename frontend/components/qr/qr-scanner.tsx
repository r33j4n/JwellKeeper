"use client";

import { Html5Qrcode } from "html5-qrcode";
import { forwardRef, useEffect, useId, useImperativeHandle, useRef, useState } from "react";
import { QrCode, Video } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";

type ScannerMode = "qr" | "camera";

export interface QrScannerHandle {
  stop: () => Promise<void>;
}

export const QrScanner = forwardRef<QrScannerHandle, { onScan: (token: string) => void; defaultMode?: ScannerMode }>(
function QrScanner({ onScan, defaultMode = "qr" }, ref) {
  const scannerRef = useRef<Html5Qrcode | null>(null);
  const scannerElementId = useId().replace(/:/g, "-");
  const [cameras, setCameras] = useState<{ id: string; label: string }[]>([]);
  const [cameraId, setCameraId] = useState("");
  const [mode, setMode] = useState<ScannerMode>(defaultMode);
  const [manualToken, setManualToken] = useState("");
  const [running, setRunning] = useState(false);

  useEffect(() => {
    Html5Qrcode.getCameras()
      .then((devices) => {
        setCameras(devices);
        setCameraId(devices[0]?.id || "");
      })
      .catch(() => setCameras([]));
    return () => {
      // Chain clear() after stop() resolves to avoid the "cannot clear while running" error
      scannerRef.current?.stop().catch(() => null).finally(() => {
        scannerRef.current?.clear();
        scannerRef.current = null;
      });
    };
  }, []);

  useEffect(() => {
    if (mode !== "camera") {
      void stop();
    }
  }, [mode]);

  async function start() {
    if (!cameraId) return;
    const scanner = new Html5Qrcode(scannerElementId);
    scannerRef.current = scanner;
    await scanner.start(
      cameraId,
      { fps: 8, qrbox: { width: 260, height: 260 } },
      (decodedText) => onScan(decodedText),
      () => undefined,
    );
    setRunning(true);
  }

  async function stop() {
    await scannerRef.current?.stop().catch(() => null);
    scannerRef.current?.clear();
    scannerRef.current = null;
    setRunning(false);
  }

  function submitManualToken() {
    const token = manualToken.trim();
    if (!token) return;
    onScan(token);
    setManualToken("");
  }

  useImperativeHandle(ref, () => ({ stop }));

  return (
    <div className="grid gap-4">
      <div className="grid gap-2 sm:max-w-sm">
        <label className="text-xs font-semibold uppercase tracking-wider text-slate-500">Scanner source</label>
        <Select value={mode} onChange={(event) => setMode(event.target.value as ScannerMode)}>
          <option value="qr">QR code scanner</option>
          <option value="camera">Camera scanner</option>
        </Select>
      </div>
      {mode === "qr" ? (
        <div className="grid gap-3 rounded-xl border border-amber-200 bg-amber-50/60 p-4">
          <div className="flex items-start gap-3">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-amber-100 text-amber-700">
              <QrCode className="h-4 w-4" />
            </div>
            <div>
              <p className="text-sm font-semibold text-slate-900">Primary QR code scanner</p>
              <p className="text-sm text-slate-600">Use a handheld QR reader or paste the token and press Enter.</p>
            </div>
          </div>
          <div className="grid gap-3 sm:grid-cols-[1fr_auto]">
            <Input
              value={manualToken}
              placeholder="Scan or paste QR token"
              onChange={(event) => setManualToken(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  submitManualToken();
                }
              }}
            />
            <Button onClick={submitManualToken} disabled={!manualToken.trim()}>
              Scan QR
            </Button>
          </div>
        </div>
      ) : (
        <div className="grid gap-3">
          <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">
            <div className="flex items-start gap-3">
              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-slate-100 text-slate-700">
                <Video className="h-4 w-4" />
              </div>
              <div>
                <p className="text-sm font-semibold text-slate-900">Camera scanner</p>
                <p className="text-sm text-slate-600">Use this when you want to scan with the device camera.</p>
              </div>
            </div>
          </div>
          <Select value={cameraId} onChange={(event) => setCameraId(event.target.value)} disabled={running}>
            <option value="">Select camera</option>
            {cameras.map((camera, index) => (
              <option key={camera.id} value={camera.id}>
                {camera.label || `Camera ${index + 1}`}
              </option>
            ))}
          </Select>
          <div className="overflow-hidden rounded-md border border-slate-200 bg-slate-950">
            <div id={scannerElementId} className="min-h-72" />
          </div>
          <div className="flex gap-2">
            <Button onClick={start} disabled={!cameraId || running}>
              Start camera
            </Button>
            <Button variant="outline" onClick={stop} disabled={!running}>
              Stop
            </Button>
          </div>
        </div>
      )}
    </div>
  );
});
