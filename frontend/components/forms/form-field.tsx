import { ReactNode } from "react";
import { cn } from "@/lib/utils/cn";

export function FormField({
  label,
  error,
  children,
  className,
}: {
  label: string;
  error?: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <label className={cn("grid gap-1.5 text-sm font-medium text-slate-700", className)}>
      <span>{label}</span>
      {children}
      {error ? <span className="text-xs font-medium text-rose-700">{error}</span> : null}
    </label>
  );
}
