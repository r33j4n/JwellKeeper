"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { FormField } from "@/components/forms/form-field";
import { authApi } from "@/lib/api/queries";
import { useAuthStore } from "@/lib/store/auth-store";
import { registerSchema } from "@/lib/validation/schemas";
import type { RegisterRequest } from "@/lib/api/types";

export function RegisterForm() {
  const router = useRouter();
  const setUser = useAuthStore((state) => state.setUser);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterRequest>({
    resolver: zodResolver(registerSchema),
    defaultValues: { defaultCurrencyCode: "LKR", billPrefix: "JK" },
  });

  async function onSubmit(values: RegisterRequest) {
    const user = await authApi.register({ ...values, defaultCurrencyCode: values.defaultCurrencyCode.toUpperCase() });
    setUser(user);
    toast.success("Shop registered");
    router.replace("/dashboard");
  }

  return (
    <Card className="w-full">
      <CardContent className="grid gap-6 p-6">
        <div>
          <h1 className="text-2xl font-semibold">Register shop</h1>
          <p className="mt-1 text-sm text-slate-600">Create your tenant and owner account.</p>
        </div>
        <form className="grid gap-4 sm:grid-cols-2" onSubmit={handleSubmit(onSubmit)}>
          <FormField label="Shop name" error={errors.shopName?.message}>
            <Input {...register("shopName")} />
          </FormField>
          <FormField label="Owner name" error={errors.ownerName?.message}>
            <Input {...register("ownerName")} />
          </FormField>
          <FormField label="Email" error={errors.email?.message}>
            <Input type="email" autoComplete="email" {...register("email")} />
          </FormField>
          <FormField label="Password" error={errors.password?.message}>
            <Input type="password" autoComplete="new-password" {...register("password")} />
          </FormField>
          <FormField label="Bill prefix" error={errors.billPrefix?.message}>
            <Input {...register("billPrefix")} />
          </FormField>
          <FormField label="Default currency" error={errors.defaultCurrencyCode?.message}>
            <Input maxLength={3} {...register("defaultCurrencyCode")} />
          </FormField>
          <div className="sm:col-span-2">
            <Button className="w-full" type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Registering..." : "Register and continue"}
            </Button>
          </div>
        </form>
        <p className="text-center text-sm text-slate-600">
          Already registered?{" "}
          <Link className="font-semibold text-amber-700" href="/login">
            Login
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}
