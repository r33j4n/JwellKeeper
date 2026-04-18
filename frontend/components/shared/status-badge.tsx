import { cn } from "@/lib/utils/cn";

const styles: Record<string, string> = {
  AVAILABLE: "bg-amber-50 text-amber-800 ring-amber-200",
  SOLD: "bg-slate-100 text-slate-700 ring-slate-200",
  MISSING: "bg-rose-50 text-rose-800 ring-rose-200",
  ARCHIVED: "bg-zinc-100 text-zinc-700 ring-zinc-200",
  OPEN: "bg-amber-50 text-amber-800 ring-amber-200",
  CLOSED: "bg-slate-100 text-slate-700 ring-slate-200",
  SCANNING: "bg-amber-50 text-amber-800 ring-amber-200",
  SEARCHING_UNRESOLVED: "bg-rose-50 text-rose-800 ring-rose-200",
  FINALIZED: "bg-emerald-50 text-emerald-800 ring-emerald-200",
  MANAGER: "bg-amber-50 text-amber-800 ring-amber-200",
  CASHIER: "bg-emerald-50 text-emerald-800 ring-emerald-200",
  STOCK_KEEPER: "bg-sky-50 text-sky-800 ring-sky-200",
  OWNER: "bg-amber-50 text-amber-800 ring-amber-200",
  STAFF: "bg-slate-100 text-slate-700 ring-slate-200",
};

export function StatusBadge({ value, className }: { value: string; className?: string }) {
  return (
    <span className={cn("inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ring-1", styles[value] || styles.SOLD, className)}>
      {value}
    </span>
  );
}
