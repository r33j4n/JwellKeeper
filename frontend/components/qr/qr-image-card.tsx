"use client";

/* eslint-disable @next/next/no-img-element */

import { Printer, Download } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { downloadDataUri } from "@/lib/utils/download";
import { printQrLabels } from "@/lib/utils/qr-label-print";
import type { Jewellery } from "@/lib/api/types";
import { shortId } from "@/lib/utils/format";
import { useAuthStore } from "@/lib/store/auth-store";

export function QrImageCard({ jewellery, image }: { jewellery: Jewellery; image?: string }) {
  const shopName = useAuthStore((state) => state.user?.shopName || "JwellKeeper");
  const label = [jewellery.typeName, jewellery.designName, jewellery.karat].filter(Boolean).join("-");
  const filename = `qr-${label}-${shortId(jewellery.id)}.png`.replace(/\s+/g, "-").toLowerCase();

  function printQr() {
    if (!image) return;
    const printed = printQrLabels([{ jewellery, qrImage: image }], shopName, filename);
    if (!printed) {
      toast.error("Popup blocked. Allow popups to print QR codes.");
    }
  }

  return (
    <Card>
      <CardHeader>
        <h2 className="font-semibold">QR Code</h2>
      </CardHeader>
      <CardContent className="grid gap-4">
        <div className="flex justify-center rounded-md border border-slate-200 bg-slate-50 p-4">
          {image ? <img src={image} alt="Jewellery QR code" className="h-64 w-64" /> : <div className="h-64 w-64 animate-pulse bg-slate-200" />}
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" disabled={!image} onClick={() => image && downloadDataUri(image, filename)}>
            <Download className="h-4 w-4" />
            Download QR
          </Button>
          <Button variant="outline" disabled={!image} onClick={printQr}>
            <Printer className="h-4 w-4" />
            Print small label
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
