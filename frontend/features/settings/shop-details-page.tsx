"use client";

import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BadgeCheck, ImageIcon, Mail, MapPin, Phone, ReceiptText, ShieldCheck, Trash2, Upload } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { PageHeader } from "@/components/shared/page-header";
import { SkeletonBlock } from "@/components/shared/states";
import { tenantProfileApi } from "@/lib/api/queries";
import type { TenantProfile } from "@/lib/api/types";

type FormState = Pick<
  TenantProfile,
  "shopName" | "shopAddress" | "shopContactNumber" | "shopEmail" | "taxNumber" | "receiptFooterNote"
>;

const emptyForm: FormState = {
  shopName: "",
  shopAddress: "",
  shopContactNumber: "",
  shopEmail: "",
  taxNumber: "",
  receiptFooterNote: "",
};

export function ShopDetailsPage() {
  const queryClient = useQueryClient();
  const profile = useQuery({ queryKey: ["tenant-profile"], queryFn: tenantProfileApi.get });
  const [form, setForm] = useState<FormState>(emptyForm);
  const [logoVersion, setLogoVersion] = useState(0);

  useEffect(() => {
    if (!profile.data) return;
    setForm({
      shopName: profile.data.shopName ?? "",
      shopAddress: profile.data.shopAddress ?? "",
      shopContactNumber: profile.data.shopContactNumber ?? "",
      shopEmail: profile.data.shopEmail ?? "",
      taxNumber: profile.data.taxNumber ?? "",
      receiptFooterNote: profile.data.receiptFooterNote ?? "",
    });
  }, [profile.data]);

  const logoSrc = useMemo(() => {
    if (!profile.data?.logoAvailable) return null;
    return `/api/backend/tenant/profile/logo?v=${profile.data.id}-${logoVersion}`;
  }, [profile.data, logoVersion]);

  const update = useMutation({
    mutationFn: () => tenantProfileApi.update(normalizeForm(form)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tenant-profile"] });
      toast.success("Shop details updated");
    },
  });

  const uploadLogo = useMutation({
    mutationFn: tenantProfileApi.uploadLogo,
    onSuccess: () => {
      setLogoVersion((value) => value + 1);
      queryClient.invalidateQueries({ queryKey: ["tenant-profile"] });
      toast.success("Shop logo updated");
    },
  });

  const deleteLogo = useMutation({
    mutationFn: tenantProfileApi.deleteLogo,
    onSuccess: () => {
      setLogoVersion((value) => value + 1);
      queryClient.invalidateQueries({ queryKey: ["tenant-profile"] });
      toast.success("Shop logo removed");
    },
  });

  function updateField<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function handleLogoFile(file?: File) {
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) {
      toast.error("Logo must be 2 MB or smaller.");
      return;
    }
    uploadLogo.mutate(file);
  }

  return (
    <div className="grid gap-6">
      <PageHeader
        title="Shop details"
        description="Owner-only shop profile used on bills, audit reports, QR print sheets, and generated documents."
      />

      {profile.isLoading ? (
        <SkeletonBlock className="h-96" />
      ) : (
        <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
          <Card>
            <CardHeader>
              <h2 className="font-semibold text-slate-900">Customer-facing business details</h2>
              <p className="text-sm text-slate-500">
                Use the official shop name and one public contact number. Tax or registration details appear only when provided.
              </p>
            </CardHeader>
            <CardContent className="grid gap-4 md:grid-cols-2">
              <Field label="Shop name" required>
                <Input value={form.shopName} maxLength={160} onChange={(event) => updateField("shopName", event.target.value)} />
              </Field>
              <Field label="Contact number">
                <Input value={form.shopContactNumber ?? ""} maxLength={40} onChange={(event) => updateField("shopContactNumber", event.target.value)} />
              </Field>
              <Field label="Shop email">
                <Input
                  type="email"
                  value={form.shopEmail ?? ""}
                  maxLength={120}
                  placeholder="accounts@example.com"
                  onChange={(event) => updateField("shopEmail", event.target.value)}
                />
              </Field>
              <Field label="Tax / registration number">
                <Input value={form.taxNumber ?? ""} maxLength={80} onChange={(event) => updateField("taxNumber", event.target.value)} />
              </Field>
              <Field label="Shop address" className="md:col-span-2">
                <Textarea
                  value={form.shopAddress ?? ""}
                  maxLength={500}
                  placeholder="Street, city, province/state, country"
                  onChange={(event) => updateField("shopAddress", event.target.value)}
                />
              </Field>
              <Field label="Receipt footer note" className="md:col-span-2">
                <Textarea
                  value={form.receiptFooterNote ?? ""}
                  maxLength={500}
                  placeholder="Thank you for shopping with us. Goods once sold are subject to shop return policy."
                  onChange={(event) => updateField("receiptFooterNote", event.target.value)}
                />
              </Field>
              <div className="flex flex-wrap items-center gap-3 md:col-span-2">
                <Button disabled={update.isPending || !form.shopName.trim()} onClick={() => update.mutate()}>
                  {update.isPending ? "Saving..." : "Save shop details"}
                </Button>
                {!form.shopName.trim() && <p className="text-xs text-rose-600">Shop name is required.</p>}
              </div>
            </CardContent>
          </Card>

          <div className="grid gap-6">
            <Card>
              <CardHeader>
                <h2 className="font-semibold text-slate-900">Shop logo</h2>
                <p className="text-sm text-slate-500">PNG, JPG, or WebP. Keep it square or landscape for clean PDF headers.</p>
              </CardHeader>
              <CardContent className="grid gap-4">
                <div className="flex aspect-[3/2] items-center justify-center overflow-hidden rounded-md border border-dashed border-amber-200 bg-amber-50/60">
                  {logoSrc ? (
                    <img src={logoSrc} alt={`${form.shopName || "Shop"} logo`} className="max-h-full max-w-full object-contain p-4" />
                  ) : (
                    <div className="grid place-items-center gap-2 text-center text-sm text-amber-800">
                      <ImageIcon className="h-8 w-8" />
                      No logo uploaded
                    </div>
                  )}
                </div>
                <div className="flex flex-wrap gap-2">
                  <Button asChild variant="outline" disabled={uploadLogo.isPending}>
                    <label className="cursor-pointer">
                      <Upload className="h-4 w-4" />
                      {uploadLogo.isPending ? "Uploading..." : "Upload logo"}
                      <input
                        type="file"
                        accept="image/png,image/jpeg,image/webp"
                        className="hidden"
                        onChange={(event) => handleLogoFile(event.target.files?.[0])}
                      />
                    </label>
                  </Button>
                  <Button
                    variant="outline"
                    disabled={!profile.data?.logoAvailable || deleteLogo.isPending}
                    onClick={() => deleteLogo.mutate()}
                  >
                    <Trash2 className="h-4 w-4" />
                    {deleteLogo.isPending ? "Removing..." : "Remove"}
                  </Button>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <h2 className="font-semibold text-slate-900">Document standard</h2>
              </CardHeader>
              <CardContent className="grid gap-3 text-sm text-slate-600">
                <StandardTip icon={<ReceiptText className="h-4 w-4" />} text="Bills and audit reports should show logo, shop name, address, contact, email, and tax or registration number at the top." />
                <StandardTip icon={<MapPin className="h-4 w-4" />} text="Use a complete address so printed documents are useful for returns, audits, and customer records." />
                <StandardTip icon={<Phone className="h-4 w-4" />} text="Use one customer-service number that can receive WhatsApp or follow-up calls." />
                <StandardTip icon={<Mail className="h-4 w-4" />} text="Use an accounts or shop email instead of a personal email on invoices." />
                <StandardTip icon={<ShieldCheck className="h-4 w-4" />} text="Keep legal terms short in the footer; avoid long policy text that makes receipts difficult to read." />
                <StandardTip icon={<BadgeCheck className="h-4 w-4" />} text="Review details after every logo or address change by viewing a sample bill PDF and audit report PDF." />
              </CardContent>
            </Card>
          </div>
        </div>
      )}
    </div>
  );
}

function Field({ label, required, className, children }: { label: string; required?: boolean; className?: string; children: React.ReactNode }) {
  return (
    <label className={className}>
      <span className="mb-1.5 block text-sm font-medium text-slate-700">
        {label}
        {required && <span className="text-rose-600"> *</span>}
      </span>
      {children}
    </label>
  );
}

function StandardTip({ icon, text }: { icon: React.ReactNode; text: string }) {
  return (
    <div className="flex gap-2 rounded-md border border-amber-100 bg-amber-50/60 p-3">
      <div className="mt-0.5 text-amber-700">{icon}</div>
      <p>{text}</p>
    </div>
  );
}

function normalizeForm(form: FormState): Partial<TenantProfile> {
  return {
    shopName: form.shopName.trim(),
    shopAddress: blankToNull(form.shopAddress),
    shopContactNumber: blankToNull(form.shopContactNumber),
    shopEmail: blankToNull(form.shopEmail),
    taxNumber: blankToNull(form.taxNumber),
    receiptFooterNote: blankToNull(form.receiptFooterNote),
  };
}

function blankToNull(value: string | null) {
  const trimmed = value?.trim() ?? "";
  return trimmed.length > 0 ? trimmed : null;
}
