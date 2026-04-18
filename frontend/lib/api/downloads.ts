"use client";

import { filenameFromDisposition, downloadBlob } from "@/lib/utils/download";

async function fetchBackendBlob(path: string, fallbackMessage: string) {
  const response = await fetch(`/api/backend${path}`, { method: "GET" });
  if (!response.ok) {
    let message = fallbackMessage;
    const contentType = response.headers.get("content-type");
    if (contentType?.includes("application/json")) {
      const payload = await response.json().catch(() => null);
      message = payload?.message || message;
    }
    throw new Error(message);
  }
  return response;
}

export async function downloadBackendFile(path: string, fallbackFilename: string) {
  const response = await fetchBackendBlob(path, "Download failed");
  const blob = await response.blob();
  const filename = filenameFromDisposition(response.headers.get("content-disposition"), fallbackFilename);
  downloadBlob(blob, filename);
}

export async function viewBackendFile(path: string) {
  const response = await fetchBackendBlob(path, "Preview failed");
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const win = window.open(url, "_blank", "noopener,noreferrer");
  if (!win) {
    URL.revokeObjectURL(url);
    throw new Error("Popup blocked. Allow popups to view PDFs.");
  }
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}
