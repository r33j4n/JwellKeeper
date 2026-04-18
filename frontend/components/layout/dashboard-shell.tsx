"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import {
  Archive,
  BarChart3,
  Boxes,
  ClipboardCheck,
  CreditCard,
  LayoutDashboard,
  LogOut,
  Menu,
  QrCode,
  Settings,
  Store,
  Users,
  X,
} from "lucide-react";
import { toast } from "sonner";
import { authApi, shopApi } from "@/lib/api/queries";
import { useAuthStore } from "@/lib/store/auth-store";
import { useUiStore } from "@/lib/store/ui-store";
import { cn } from "@/lib/utils/cn";

const navItems = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { href: "/dashboard/jewellery", label: "Jewellery", icon: Boxes },
  { href: "/dashboard/qr-print", label: "Bulk QR Print", icon: QrCode },
  { href: "/dashboard/billing", label: "Billing", icon: CreditCard },
  { href: "/dashboard/audit", label: "Audit", icon: ClipboardCheck },
  { href: "/dashboard/analytics", label: "Analytics", icon: BarChart3 },
];

const ownerItems = [
  { href: "/dashboard/archived-jewellery", label: "Archived Jewellery", icon: Archive },
  { href: "/dashboard/settings/shop", label: "Shop Details", icon: Store },
  { href: "/dashboard/settings/staff", label: "Staff", icon: Users },
  { href: "/dashboard/settings/billing", label: "Billing Settings", icon: Settings },
];

function NavLink({ href, label, icon: Icon, active, onClick }: { href: string; label: string; icon: React.ElementType; active: boolean; onClick: () => void }) {
  return (
    <Link
      href={href}
      onClick={onClick}
      className={cn(
        "group flex items-center gap-2.5 rounded-md px-2.5 py-1.5 text-sm font-medium transition-all",
        active
          ? "bg-amber-50 text-amber-800"
          : "text-slate-600 hover:bg-slate-50 hover:text-slate-900",
      )}
    >
      <Icon
        className={cn(
          "h-[18px] w-[18px] shrink-0 transition-colors",
          active ? "text-amber-700" : "text-slate-400 group-hover:text-slate-600",
        )}
      />
      <span className="truncate">{label}</span>
      {active && <span className="ml-auto h-1.5 w-1.5 rounded-full bg-amber-500" />}
    </Link>
  );
}

export function DashboardShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const searchParams = useSearchParams();
  const user = useAuthStore((state) => state.user);
  const clearUser = useAuthStore((state) => state.clearUser);
  const sidebarOpen = useUiStore((state) => state.sidebarOpen);
  const setSidebarOpen = useUiStore((state) => state.setSidebarOpen);
  const queryClient = useQueryClient();

  const shopState = useQuery({ queryKey: ["shop-state"], queryFn: () => shopApi.state(), enabled: Boolean(user) });
  const canManageShop = user?.role === "OWNER" || user?.role === "MANAGER";

  const openShop = useMutation({
    mutationFn: () => shopApi.open(),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ["shop-state"] }); toast.success("Shop opened"); },
  });
  const closeShop = useMutation({
    mutationFn: () => shopApi.close(),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ["shop-state"] }); toast.success("Shop closed"); },
  });

  useEffect(() => {
    if (searchParams.get("accessDenied") === "owner") toast.error("Owner access is required for that page.");
    if (searchParams.get("accessDenied") === "billing") toast.error("Billing access is not enabled for stock keeper users.");
  }, [searchParams]);

  async function logout() {
    await authApi.logout().catch(() => null);
    clearUser();
    router.replace("/login");
  }

  const allowedNavItems = navItems.filter((item) => !(item.href === "/dashboard/billing" && user?.role === "STOCK_KEEPER"));
  const items = user?.role === "OWNER" ? allowedNavItems : allowedNavItems;
  const adminItems = user?.role === "OWNER" ? ownerItems : [];
  const sidebarShopName = user?.shopName?.trim() || "Jewellery Shop";

  const isActive = (href: string) => href === "/dashboard" ? pathname === href : pathname.startsWith(href);

  return (
    <div className="min-h-screen bg-[#fffaf0]">
      {/* Sidebar */}
      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-40 flex w-56 flex-col border-r border-slate-100 bg-white shadow-sm transition-transform duration-200 lg:translate-x-0",
          sidebarOpen ? "translate-x-0" : "-translate-x-full",
        )}
      >
        {/* Logo */}
        <div className="flex h-14 items-center justify-between px-4 border-b border-slate-100">
          <Link href="/dashboard" className="flex items-center gap-2">
            <div className="flex h-7 w-7 items-center justify-center rounded-md bg-amber-600 text-white text-xs font-bold">
              {sidebarShopName.charAt(0).toUpperCase()}
            </div>
            <span className="truncate text-sm font-semibold text-slate-900 tracking-tight">{sidebarShopName}</span>
          </Link>
          <button
            className="rounded-md p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 lg:hidden"
            onClick={() => setSidebarOpen(false)}
            aria-label="Close menu"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto px-2 py-3 space-y-0.5">
          {items.map((item) => (
            <NavLink
              key={item.href}
              href={item.href}
              label={item.label}
              icon={item.icon}
              active={isActive(item.href)}
              onClick={() => setSidebarOpen(false)}
            />
          ))}

          {adminItems.length > 0 && (
            <>
              <div className="mx-2 my-2 border-t border-slate-100" />
              <p className="px-2.5 pb-1 text-[10px] font-semibold uppercase tracking-widest text-slate-400">Admin</p>
              {adminItems.map((item) => (
                <NavLink
                  key={item.href}
                  href={item.href}
                  label={item.label}
                  icon={item.icon}
                  active={isActive(item.href)}
                  onClick={() => setSidebarOpen(false)}
                />
              ))}
            </>
          )}
        </nav>

        {/* User + Logout */}
        <div className="border-t border-slate-100 p-3 space-y-2">
          <div className="flex items-center gap-2 px-1">
            <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-amber-100 text-xs font-semibold text-amber-800">
              {user?.name?.[0]?.toUpperCase() ?? "U"}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-xs font-semibold text-slate-800">{user?.name ?? "User"}</p>
              <p className="text-[10px] text-slate-400">{user?.role ?? ""}</p>
            </div>
          </div>
          <button
            onClick={logout}
            className="flex w-full items-center gap-2.5 rounded-md px-2.5 py-1.5 text-sm text-slate-500 hover:bg-rose-50 hover:text-rose-700 transition-colors"
          >
            <LogOut className="h-[18px] w-[18px] shrink-0" />
            Sign out
          </button>
          <p className="px-1 text-[10px] text-slate-400">
            Built by{" "}
            <a className="font-medium text-amber-700 hover:underline" href="https://autowhap.com" target="_blank" rel="noreferrer">
              autowhap.com
            </a>
          </p>
        </div>
      </aside>

      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-30 bg-slate-900/40 backdrop-blur-sm lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Main content */}
      <div className="lg:pl-56">
        <header className="sticky top-0 z-20 flex h-14 items-center justify-between border-b border-slate-100 bg-white/95 px-4 backdrop-blur sm:px-6">
          <button
            className="rounded-md p-1.5 text-slate-500 hover:bg-slate-100 lg:hidden"
            onClick={() => setSidebarOpen(true)}
            aria-label="Open menu"
          >
            <Menu className="h-5 w-5" />
          </button>

          <div className="flex items-center gap-2 ml-auto">
            {shopState.data ? (
              <div className="flex items-center gap-1.5 rounded-full border border-amber-200 bg-amber-50 px-3 py-1 text-xs text-amber-900">
                <span className={cn("h-1.5 w-1.5 rounded-full", shopState.data.status === "OPEN" ? "bg-emerald-500" : "bg-rose-500")} />
                Shop {shopState.data.status === "OPEN" ? "Open" : "Closed"}
                {canManageShop ? (
                  <button
                    className="ml-1 text-amber-700 font-medium hover:underline disabled:opacity-50"
                    disabled={openShop.isPending || closeShop.isPending}
                    onClick={() => shopState.data?.status === "OPEN" ? closeShop.mutate() : openShop.mutate()}
                  >
                    {shopState.data.status === "OPEN" ? "· Close" : "· Open"}
                  </button>
                ) : null}
              </div>
            ) : null}
          </div>
        </header>

        <main className="px-4 py-6 sm:px-6 lg:px-8">{children}</main>
      </div>
    </div>
  );
}
