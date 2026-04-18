import { Suspense } from "react";
import { DashboardShell } from "@/components/layout/dashboard-shell";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <Suspense fallback={<div className="p-6">Loading workspace...</div>}>
      <DashboardShell>{children}</DashboardShell>
    </Suspense>
  );
}
