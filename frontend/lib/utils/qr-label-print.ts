"use client";

import type { Jewellery } from "@/lib/api/types";
import { formatWeight } from "@/lib/utils/format";

export type PrintableQrLabel = {
  jewellery: Jewellery;
  qrImage: string;
};

type PrintLayout = "single" | "bulk";

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

      <!-- LEFT: shop name in the 5mm bleed zone -->
      <div class="shop-zone">
        <span class="shop-name">${escapeHtml(shopName)}</span>
      </div>

      <!-- CENTER: 10mm × 20mm white inner card -->
      <div class="inner-card">
        <img class="qr" src="${label.qrImage}" alt="QR" />
        <div class="meta">
          <div class="meta-line">${escapeHtml(item.typeName)}</div>
          <div class="meta-line">${escapeHtml(item.karat)}</div>
          <div class="meta-line">${escapeHtml(formatWeight(item.weight))}</div>
        </div>
      </div>

      <!-- RIGHT: 5mm right bleed -->
      <div class="right-zone"></div>

    </section>
  `;
}

// Bulk label HTML is IDENTICAL to the single label — same classes, same structure.
// The rotation is handled entirely in CSS on .bulk-label.
function bulkLabelHtml(label: PrintableQrLabel, shopName: string) {
  const item = label.jewellery;

  return `
    <section class="bulk-label">

      <div class="shop-zone">
        <span class="shop-name">${escapeHtml(shopName)}</span>
      </div>

      <div class="inner-card">
        <img class="qr" src="${label.qrImage}" alt="QR" />
        <div class="meta">
          <div class="meta-line">${escapeHtml(item.typeName)}</div>
          <div class="meta-line">${escapeHtml(item.karat)}</div>
          <div class="meta-line">${escapeHtml(formatWeight(item.weight))}</div>
        </div>
      </div>

      <div class="right-zone"></div>

    </section>
  `;
}

function bulkSlotHtml(label: PrintableQrLabel | undefined, index: number, shopName: string) {
  return `
    <div class="bulk-slot bulk-slot--${index}">
      ${label ? bulkLabelHtml(label, shopName) : '<div class="bulk-label" aria-hidden="true"></div>'}
    </div>
  `;
}

function chunkLabels(labels: PrintableQrLabel[], size: number) {
  const chunks: PrintableQrLabel[][] = [];
  for (let index = 0; index < labels.length; index += size) {
    chunks.push(labels.slice(index, index + size));
  }
  return chunks;
}

export function printQrLabels(
  labels: PrintableQrLabel[],
  shopName: string,
  title = "Jewellery QR Labels",
  layout: PrintLayout = "single"
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
          * { box-sizing: border-box; margin: 0; padding: 0; }

          body {
            background: #fff;
            font-family: Arial, Helvetica, sans-serif;
            color: #111827;
          }

          /* ─── Shared inner styles (used by both single + bulk) ── */

          .shop-zone {
            width: 5mm;
            height: 20mm;
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
            writing-mode: vertical-rl;
            transform: rotate(180deg);
            text-align: center;
            max-height: 19mm;
          }

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
            padding-top: 1mm;
            padding-bottom: 0.3mm;
            gap: 0.2mm;
            overflow: hidden;
          }

          .qr {
            width: 9mm;
            height: 9mm;
            display: block;
            flex-shrink: 0;
            image-rendering: crisp-edges;
            image-rendering: pixelated;
          }

          .meta {
            width: 100%;
            flex: 1;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: space-evenly;
            text-align: center;
            padding: 0 0.3mm;
          }

          .meta-line {
            font-size: 1.6mm;
            font-weight: 900;
            line-height: 1;
            color: #111827;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            width: 100%;
            text-align: center;
          }

          .right-zone {
            width: 5mm;
            flex-shrink: 0;
          }

          ${layout === "bulk" ? `
          /* ─── Bulk sheet: 90mm × 20mm, three 30×20mm slots ─────── */
          main {
            width: 90mm;
            margin: 0;
            padding: 0;
          }

          .bulk-sheet {
            width: 90mm;
            height: 20mm;
            position: relative;
            break-after: page;
            page-break-after: always;
            break-inside: avoid;
            page-break-inside: avoid;
            overflow: hidden;
          }

          .bulk-slot {
            position: absolute;
            top: 0;
            width: 30mm;
            height: 20mm;
            overflow: hidden;
          }
          .bulk-slot--0 { left: 0mm; }
          .bulk-slot--1 { left: 30mm; }
          .bulk-slot--2 { left: 60mm; }

          /*
           * .bulk-label is the exact single label (20mm × 30mm),
           * positioned and rotated 90° CW to fill the 30mm × 20mm slot.
           *
           * Math:
           *   Label centre = (10mm, 15mm) within itself.
           *   Slot  centre = (15mm, 10mm) within the slot.
           *   So place the label at: left = 15-10 = 5mm, top = 10-15 = -5mm
           *   Rotate around the label's own centre: transform-origin: 10mm 15mm
           *   After 90° CW rotation the 20mm wide × 30mm tall label
           *   occupies exactly 30mm wide × 20mm tall — perfect fit.
           */
          .bulk-label {
            position: absolute;
            left: 5mm;
            top: -5mm;
            width: 20mm;
            height: 30mm;
            display: flex;
            flex-direction: row;
            align-items: center;
            justify-content: center;
            transform: rotate(90deg);
            transform-origin: 10mm 15mm;
            overflow: hidden;
          }
          ` : `
          /* ─── Single label: 20mm × 30mm ─────────────── */
          .qr-label {
            width: 20mm;
            height: 30mm;
            display: flex;
            flex-direction: row;
            align-items: center;
            justify-content: center;
            break-after: page;
            page-break-after: always;
            break-inside: avoid;
            page-break-inside: avoid;
            overflow: hidden;
          }
          `}

          @page {
            size: ${layout === "bulk" ? "90mm 20mm" : "20mm 30mm"};
            margin: 0;
          }

          @media print {
            body { margin: 0; padding: 0; background: #fff; }
            ${layout === "bulk"
              ? "main { width: 90mm; } .bulk-sheet { margin: 0; }"
              : ".qr-label { margin: 0; }"}
          }
        </style>
      </head>
      <body>
        <main>
          ${layout === "bulk"
            ? chunkLabels(labels, 3)
                .map(
                  (pageLabels) => `
                    <section class="bulk-sheet">
                      ${Array.from({ length: 3 }, (_, index) => bulkSlotHtml(pageLabels[index], index, normalizedShopName)).join("")}
                    </section>
                  `,
                )
                .join("")
            : labels.map((label) => labelHtml(label, normalizedShopName)).join("")}
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