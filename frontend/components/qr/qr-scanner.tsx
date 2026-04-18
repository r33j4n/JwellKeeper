"use client";

import { Html5Qrcode } from "html5-qrcode";
import { forwardRef, useEffect, useImperativeHandle, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";

const scannerElementId = "jk-qr-scanner";

export interface QrScannerHandle {
  stop: () => Promise<void>;
}

export const QrScanner = forwardRef<QrScannerHandle, { onScan: (token: string) => void }>(
function QrScanner({ onScan }, ref) {
  const scannerRef = useRef<Html5Qrcode | null>(null);
  const [cameras, setCameras] = useState<{ id: string; label: string }[]>([]);
  const [cameraId, setCameraId] = useState("");
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

  useImperativeHandle(ref, () => ({ stop }));

  return (
    <div className="grid gap-3">
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
  );
});
