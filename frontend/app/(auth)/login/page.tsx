import { Suspense } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { LoginForm } from "@/features/auth/login-form";

export default function LoginPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-100 px-4 py-12">
      <div className="w-full max-w-md space-y-6">
        <div className="flex items-center justify-between">
          <p className="text-sm font-medium text-slate-700">Login to your account</p>
          <Button asChild variant="outline" size="sm">
            <Link href="/">Home</Link>
          </Button>
        </div>
        <Suspense fallback={<div className="rounded-md bg-white p-6">Loading login...</div>}>
          <LoginForm />
        </Suspense>
        <footer className="text-center text-xs text-slate-500">
          Jewell Keeper 2026 Developed by <a className="font-semibold text-amber-700 hover:underline" href="https://autowhap.com" target="_blank" rel="noreferrer">Autowhap.com</a>
        </footer>
      </div>
    </main>
  );
}
