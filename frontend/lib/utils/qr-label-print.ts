"use client";

import type { Jewellery } from "@/lib/api/types";
import { formatWeight, shortId } from "@/lib/utils/format";

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
      <div class="shop">${escapeHtml(shopName)}</div>
      <img class="qr" src="${label.qrImage}" alt="QR" />
      <div class="meta">
        <div class="line strong">${escapeHtml(item.typeName)}</div>
        ${item.designName ? `<div class="line">${escapeHtml(item.designName)}</div>` : ""}
        <div class="line">${escapeHtml(item.karat)} · ${escapeHtml(formatWeight(item.weight))}</div>
        <div class="tiny">#${escapeHtml(shortId(item.id))}</div>
      </div>
    </section>
  `;
}

export function printQrLabels(labels: PrintableQrLabel[], shopName: string, title = "Jewellery QR labels") {
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
          * { box-sizing: border-box; }
          body {
            margin: 0;
            padding: 8mm;
            font-family: Arial, Helvetica, sans-serif;
            color: #111827;
            background: #fff;
          }
          .sheet {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(36mm, 1fr));
            gap: 4mm;
            align-items: start;
          }
          .qr-label {
            width: 36mm;
            min-height: 46mm;
            break-inside: avoid;
            page-break-inside: avoid;
            border: 1px solid #d6a326;
            border-radius: 2mm;
            padding: 2mm;
            text-align: center;
            overflow: hidden;
          }
          .shop {
            font-size: 6pt;
            line-height: 1.1;
            font-weight: 700;
            color: #8a5a00;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            margin-bottom: 1mm;
          }
          .qr {
            width: 22mm;
            height: 22mm;
            display: block;
            margin: 0 auto 1mm;
            image-rendering: crisp-edges;
          }
          .meta {
            font-size: 6.3pt;
            line-height: 1.13;
            color: #111827;
          }
          .line {
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }
          .strong { font-weight: 700; }
          .tiny {
            margin-top: 0.8mm;
            font-size: 5pt;
            color: #6b7280;
          }
          @page { size: A4; margin: 8mm; }
          @media print {
            body { padding: 0; }
            .sheet { gap: 3mm; }
          }
        </style>
      </head>
      <body>
        <main class="sheet">
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
