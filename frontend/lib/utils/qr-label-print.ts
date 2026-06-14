"use client";

import type { Jewellery } from "@/lib/api/types";
import { formatWeight } from "@/lib/utils/format";

export type PrintableQrLabel = {
  jewellery: Jewellery;
  qrImage: string;
};

function escapeHtml(value: string) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function labelHtml(label: PrintableQrLabel, shopName: string) {
  const item = label.jewellery;

  return `
    <section class="qr-label">

      <!--
        ┌─────────────────────────────────────────────────┐
        │  OUTER LABEL  20mm × 30mm                       │
        │                                                 │
        │  ┌──────────┐  ┌──────────────────┐            │
        │  │ SHOP NAME│  │  10mm × 20mm     │            │
        │  │ vertical │  │  inner card      │            │
        │  │ 20mm tall│  │  ┌────────────┐  │            │
        │  │ in 5mm   │  │  │ 9mm × 9mm  │  │            │
        │  │ left zone│  │  │    QR      │  │            │
        │  │          │  │  └────────────┘  │            │
        │  │          │  │  Chain (center)  │            │
        │  │          │  │  22KT  (center)  │            │
        │  │          │  │  8g    (center)  │            │
        │  └──────────┘  └──────────────────┘            │
        └─────────────────────────────────────────────────┘
      -->

      <!-- LEFT: shop name in the 5mm bleed zone -->
      <div class="shop-zone">
        <span class="shop-name">${escapeHtml(shopName)}</span>
      </div>

      <!-- CENTER: 10mm × 20mm white inner card -->
      <div class="inner-card">

        <!-- QR: 9mm × 9mm, top-center, 1mm from top -->
        <img class="qr" src="${label.qrImage}" alt="QR" />

        <!-- Meta: centered, below QR, 1mm bottom margin -->
        <div class="meta">
          <div class="meta-line">${escapeHtml(item.typeName)}</div>
          <div class="meta-line">${escapeHtml(item.karat)}</div>
          <div class="meta-line">${escapeHtml(formatWeight(item.weight))}</div>
        </div>

      </div>

      <!-- RIGHT: 5mm right bleed (empty, keeps card centred) -->
      <div class="right-zone"></div>

    </section>
  `;
}

export function printQrLabels(
  labels: PrintableQrLabel[],
  shopName: string,
  title = "Jewellery QR Labels"
) {
  if (!labels.length) return false;

  const win = window.open("", "_blank", "width=900,height=700");
  if (!win) return false;

  const normalizedShopName = shopName?.trim() || "Jewellery Shop";

  win.document.write(`
    <!doctype html>
    <html>
      <head>
        <title>${escapeHtml(title)}</title>
        <style>
          /* ─── Reset ────────────────────────────────────── */
          * { box-sizing: border-box; margin: 0; padding: 0; }

          body {
            background: #fff;
            font-family: Arial, Helvetica, sans-serif;
            color: #111827;
          }

          /* ─── Outer label: 20mm × 30mm ─────────────────── */
          .qr-label {
            width: 20mm;
            height: 30mm;
            position: relative;
            display: flex;
            flex-direction: row;
            align-items: center;        /* vertically centre inner-card in 30mm */
            justify-content: center;
            break-after: page;
            page-break-after: always;
            break-inside: avoid;
            page-break-inside: avoid;
            overflow: hidden;
          }

          /* ─── Left 5mm bleed: shop name ────────────────── */
          .shop-zone {
            width: 5mm;
            height: 20mm;              /* same height as inner card */
            flex-shrink: 0;
            display: flex;
            align-items: center;
            justify-content: center;
            overflow: hidden;
          }

          .shop-name {
            display: block;
            font-size: 2.4pt;
            font-weight: 700;
            text-transform: uppercase;
            color: #7a4f00;
            letter-spacing: 0.3pt;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            /* rotate so text reads bottom → top */
            writing-mode: vertical-rl;
            transform: rotate(180deg);
            text-align: center;
            max-height: 19mm;
          }

          /* ─── Inner white card: 10mm × 20mm ────────────── */
          /*
           *  Height budget (strict):
           *  1mm top pad + 9mm QR + 0.2mm gap + text zone + 0.3mm bottom pad = 20mm
           *  → text zone = 20 − 1 − 9 − 0.2 − 0.3 = 9.5mm for 3 lines
           *  → ~3.1mm per line at font-size 2.8mm with line-height 1.1 ✓
           */
          .inner-card {
            width: 10mm;
            height: 20mm;
            flex-shrink: 0;
            border: 0.25mm solid #b8860b;
            border-radius: 0.8mm;
            background: #fff;

            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: flex-start;

            padding-top: 1mm;           /* 1mm top clearance for QR */
            padding-bottom: 0.3mm;      /* minimal — use every mm for text */
            gap: 0.2mm;                 /* tiny gap between QR and text block */
            overflow: hidden;
          }

          /* ─── QR: full 9mm × 9mm — no compromise ────────── */
          .qr {
            width: 9mm;
            height: 9mm;
            display: block;
            flex-shrink: 0;
            image-rendering: crisp-edges;
            image-rendering: pixelated;
          }

          /* ─── Meta text: max size in remaining 9.5mm ─────── */
          .meta {
            width: 100%;
            flex: 1;                    /* fills all remaining height */
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: space-evenly; /* spread 3 lines across full zone */
            text-align: center;
            padding: 0 0.3mm;           /* tiny side padding so text doesn't touch border */
          }

          .meta-line {
            font-size: 1.6mm;           /* ≈ 7.9pt — maximum that fits 3 lines in 9.5mm */
            font-weight: 900;           /* boldest valid CSS value */
            line-height: 1;             /* tight — let space-evenly handle spacing */
            color: #111827;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            width: 100%;
            text-align: center;
          }

          /* ─── Right 5mm bleed (keeps card centred) ──────── */
          .right-zone {
            width: 5mm;
            flex-shrink: 0;
          }

          /* ─── Print page setup ──────────────────────────── */
          @page {
            size: 20mm 30mm;
            margin: 0;
          }

          @media print {
            body { margin: 0; padding: 0; background: #fff; }
            .qr-label { margin: 0; }
          }
        </style>
      </head>
      <body>
        <main>
          ${labels.map((label) => labelHtml(label, normalizedShopName)).join("")}
        </main>
        <script>
          window.onload = () => {
            window.focus();
            window.print();
          };
        </script>
      </body>
    </html>
  `);

  win.document.close();
  return true;
}