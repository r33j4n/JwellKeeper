import Decimal from "decimal.js";

export function formatDate(value?: string | null) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("en", { dateStyle: "medium" }).format(new Date(value));
}

export function formatDateTime(value?: string | null) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("en", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

export function formatWeight(value?: string | number | null) {
  if (value === undefined || value === null || value === "") return "0.000 g";
  return `${new Decimal(value).toFixed(3)} g`;
}

export function formatMoney(value?: string | number | null, currency = "LKR") {
  const amount = value === undefined || value === null || value === "" ? 0 : Number(new Decimal(value).toFixed(2));
  return new Intl.NumberFormat("en", {
    style: "currency",
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
}

export function shortId(id?: string | null) {
  return id ? id.slice(0, 8) : "-";
}

export function todayIsoDate() {
  return new Date().toISOString().slice(0, 10);
}
