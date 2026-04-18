import { Suspense } from "react";
import { LoginForm } from "@/features/auth/login-form";

export default function LoginPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-100 px-4 py-12">
      <Suspense fallback={<div className="rounded-md bg-white p-6">Loading login...</div>}>
        <LoginForm />
      </Suspense>
    </main>
  );
}
