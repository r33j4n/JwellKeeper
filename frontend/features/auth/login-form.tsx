"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { FormField } from "@/components/forms/form-field";
import { authApi } from "@/lib/api/queries";
import { useAuthStore } from "@/lib/store/auth-store";
import { loginSchema } from "@/lib/validation/schemas";
import type { LoginRequest } from "@/lib/api/types";

export function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const redirectTarget = searchParams.get("next");
  const loginReason = searchParams.get("reason");
  const needsAuthentication = loginReason === "auth-required";
  const setUser = useAuthStore((state) => state.setUser);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginRequest>({ resolver: zodResolver(loginSchema) });

  async function onSubmit(values: LoginRequest) {
    const user = await authApi.login(values);
    setUser(user);
    toast.success("Welcome back");
    router.replace(redirectTarget || "/dashboard");
  }

  return (
    <Card className="w-full">
      <CardContent className="grid gap-6 p-6">
        <div>
          <h1 className="text-2xl font-semibold">Login</h1>
          <p className="mt-1 text-sm text-slate-600">Access your jewellery stock workspace.</p>
        </div>
        {needsAuthentication ? (
          <div className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
            Please sign in to continue to your dashboard.
          </div>
        ) : null}
        <form className="grid gap-4" onSubmit={handleSubmit(onSubmit)}>
          <FormField label="Email" error={errors.email?.message}>
            <Input type="email" autoComplete="email" {...register("email")} />
          </FormField>
          <FormField label="Password" error={errors.password?.message}>
            <Input type="password" autoComplete="current-password" {...register("password")} />
          </FormField>
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? "Signing in..." : "Sign in"}
          </Button>
        </form>
        <p className="text-center text-sm text-slate-600">
          New shop?{" "}
          <Link className="font-semibold text-amber-700" href="/register">
            Register here
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}
