import Link from "next/link";
import { ArrowRight, BadgeCheck, Boxes, ClipboardCheck, Gem, QrCode, ShieldCheck, ShoppingBag, Sparkles } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";

type Feature = {
  title: string;
  description: string;
  icon: LucideIcon;
};

const features: Feature[] = [
  {
    title: "QR Stock Management",
    description: "Track every item with QR-driven stock flows that keep the floor, the drawer, and the system aligned.",
    icon: QrCode,
  },
  {
    title: "Missing Alerts",
    description: "Spot gaps quickly with clear alerts so nothing quietly disappears from your inventory.",
    icon: ShieldCheck,
  },
  {
    title: "Staff-Based Access",
    description: "Give the right people the right access with role-based control for safer day-to-day operations.",
    icon: BadgeCheck,
  },
  {
    title: "Daily Audit",
    description: "Close each day with a fast audit loop that keeps your stock counts trustworthy.",
    icon: ClipboardCheck,
  },
  {
    title: "QR Easy Billing",
    description: "Move from scan to invoice with less friction and fewer manual steps.",
    icon: ShoppingBag,
  },
  {
    title: "Business Insights",
    description: "See the patterns that matter most, from item movement to sales performance.",
    icon: Boxes,
  },
];

const highlights = [
  { value: "6", label: "core workflows" },
  { value: "24/7", label: "inventory visibility" },
  { value: "1", label: "calm dashboard" },
];

const operationalPillars = ["Stock control", "QR billing", "Daily audit", "Role access"];

export default function Home() {
  return (
    <main className="relative min-h-screen overflow-hidden bg-[#f8f3eb] text-slate-950">
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <div className="absolute -left-24 top-0 h-72 w-72 rounded-full bg-amber-200/50 blur-3xl" />
        <div className="absolute right-[-5rem] top-24 h-80 w-80 rounded-full bg-orange-300/30 blur-3xl" />
        <div className="absolute bottom-0 left-1/3 h-72 w-72 rounded-full bg-slate-200/60 blur-3xl" />
      </div>

      <div className="relative mx-auto flex min-h-screen w-full max-w-7xl flex-col px-5 pb-10 sm:px-8 lg:px-10">
        <header className="flex items-center justify-between py-6">
          <Link href="/" className="inline-flex items-center gap-3 rounded-full border border-white/70 bg-white/80 px-4 py-2 shadow-sm backdrop-blur">
            <span className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-amber-500 to-orange-600 text-white shadow-sm">
              <Gem className="h-5 w-5" />
            </span>
            <span className="leading-tight">
              <span className="block text-sm font-semibold tracking-tight text-slate-950">Jewell Keeper</span>
              <span className="block text-xs text-slate-500">Jewellery stock management</span>
            </span>
          </Link>

          <div className="hidden items-center gap-3 sm:flex">
            <Button asChild variant="outline">
              <Link href="/dashboard">Dashboard</Link>
            </Button>
            <Button asChild variant="ghost">
              <Link href="/login">Login</Link>
            </Button>
            <Button asChild>
              <Link href="/register">
                Register
                <ArrowRight className="h-4 w-4" />
              </Link>
            </Button>
          </div>
        </header>

        <section className="grid flex-1 items-center gap-8 pb-8 pt-4 lg:grid-cols-[1.15fr_0.85fr] lg:gap-10">
          <div className="space-y-7">
            <div className="inline-flex items-center gap-2 rounded-full border border-amber-200 bg-white/80 px-4 py-2 text-sm font-medium text-amber-800 shadow-sm backdrop-blur">
              <Sparkles className="h-4 w-4" />
              Built for jewellery shops that want clarity, speed, and control.
            </div>

            <div className="space-y-5">
              <p className="text-sm font-semibold uppercase tracking-[0.3em] text-amber-700">Autowhap&apos;s</p>
              <h1 className="max-w-2xl text-5xl font-semibold tracking-tight text-slate-950 sm:text-6xl lg:text-7xl">
                Jewell Keeper
              </h1>
              <p className="max-w-2xl text-lg leading-8 text-slate-600 sm:text-xl">
                QR stock management, missing alerts, staff access, daily audit, easy billing, and business insights in one elegant workspace.
              </p>
            </div>

            <div className="flex flex-col gap-3 sm:flex-row">
              <Button asChild size="lg" className="shadow-lg shadow-amber-500/20">
                <Link href="/register">
                  Start free registration
                  <ArrowRight className="h-4 w-4" />
                </Link>
              </Button>
              <Button asChild size="lg" variant="outline" className="border-slate-300 bg-white/80">
                <Link href="/dashboard">Open dashboard</Link>
              </Button>
            </div>

            <div className="grid gap-3 sm:grid-cols-3">
              {highlights.map((item) => (
                <Card key={item.label} className="border-white/70 bg-white/80 backdrop-blur">
                  <CardContent className="px-4 py-4">
                    <div className="text-2xl font-semibold tracking-tight text-slate-950">{item.value}</div>
                    <div className="mt-1 text-sm text-slate-500">{item.label}</div>
                  </CardContent>
                </Card>
              ))}
            </div>

            <div className="flex flex-wrap gap-2">
              {operationalPillars.map((pill) => (
                <span key={pill} className="rounded-full border border-slate-200 bg-white/70 px-4 py-2 text-sm text-slate-600 shadow-sm backdrop-blur">
                  {pill}
                </span>
              ))}
            </div>
          </div>

          <div className="grid gap-5">
            <Card className="border-slate-200/80 bg-white/85 shadow-[0_30px_90px_-30px_rgba(120,90,30,0.4)] backdrop-blur">
              <CardContent className="space-y-6 p-6 sm:p-7">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.28em] text-amber-700">At a glance</p>
                    <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">Operations designed for daily use.</h2>
                  </div>
                  <span className="rounded-full bg-amber-100 px-3 py-1 text-xs font-semibold text-amber-800">Live</span>
                </div>

                <div className="grid gap-3 sm:grid-cols-2">
                  {[
                    { label: "Stock sync", value: "QR first" },
                    { label: "Audit ready", value: "Daily" },
                    { label: "Access control", value: "Role based" },
                    { label: "Billing flow", value: "Fast" },
                  ].map((stat) => (
                    <div key={stat.label} className="rounded-2xl border border-amber-100 bg-gradient-to-br from-amber-50 to-white p-4">
                      <div className="text-sm text-slate-500">{stat.label}</div>
                      <div className="mt-1 text-lg font-semibold text-slate-950">{stat.value}</div>
                    </div>
                  ))}
                </div>

                <div className="rounded-3xl border border-slate-200 bg-slate-950 p-5 text-white shadow-inner">
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-[0.28em] text-amber-300">Designed for the floor</p>
                      <p className="mt-2 text-lg font-medium">Quick scans, clear counts, and calmer handoffs.</p>
                    </div>
                    <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white/10 text-amber-300">
                      <QrCode className="h-6 w-6" />
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card className="border-white/70 bg-white/80 backdrop-blur">
              <CardContent className="grid gap-4 p-6">
                <div className="flex items-center gap-3">
                  <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
                    <ShieldCheck className="h-5 w-5" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold uppercase tracking-[0.22em] text-amber-700">Developed by</p>
                    <p className="text-xl font-semibold text-slate-950">Autowhap</p>
                  </div>
                </div>
                <a className="text-sm text-slate-600 transition hover:text-amber-700" href="https://autowhap.com" target="_blank" rel="noreferrer">
                  autowhap.com
                </a>
              </CardContent>
            </Card>
          </div>
        </section>

        <section className="pb-4 pt-2">
          <div className="mb-5 max-w-2xl">
            <p className="text-sm font-semibold uppercase tracking-[0.3em] text-amber-700">Features</p>
            <h2 className="mt-2 text-3xl font-semibold tracking-tight text-slate-950 sm:text-4xl">Track every item with QR-driven stock flows

.</h2>
          </div>

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {features.map((feature) => {
              const Icon = feature.icon;

              return (
                <Card key={feature.title} className="border-white/70 bg-white/85 backdrop-blur transition-transform duration-200 hover:-translate-y-1 hover:shadow-lg">
                  <CardContent className="space-y-4 p-6">
                    <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-amber-100 to-orange-100 text-amber-700">
                      <Icon className="h-5 w-5" />
                    </div>
                    <div>
                      <h3 className="text-lg font-semibold text-slate-950">{feature.title}</h3>
                      <p className="mt-2 text-sm leading-6 text-slate-600">{feature.description}</p>
                    </div>
                  </CardContent>
                </Card>
              );
            })}
          </div>
        </section>
      </div>
    </main>
  );
}
