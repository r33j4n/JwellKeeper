"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { PageHeader } from "@/components/shared/page-header";
import { settingsApi } from "@/lib/api/queries";

export function BillingSettingsPage() {
  const queryClient = useQueryClient();
  const settings = useQuery({ queryKey: ["tenant-settings"], queryFn: settingsApi.get });
  const [form, setForm] = useState<Partial<{ defaultCurrencyCode: string; billPrefix: string; billNumberFormat: string; nextBillSequence: string }>>({});
  const values = {
    defaultCurrencyCode: form.defaultCurrencyCode ?? settings.data?.defaultCurrencyCode ?? "",
    billPrefix: form.billPrefix ?? settings.data?.billPrefix ?? "",
    billNumberFormat: form.billNumberFormat ?? settings.data?.billNumberFormat ?? "",
    nextBillSequence: form.nextBillSequence ?? String(settings.data?.nextBillSequence ?? ""),
  };

  const update = useMutation({
    mutationFn: () =>
      settingsApi.update({
        defaultCurrencyCode: values.defaultCurrencyCode.toUpperCase(),
        billPrefix: values.billPrefix,
        billNumberFormat: values.billNumberFormat,
        nextBillSequence: Number(values.nextBillSequence),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tenant-settings"] });
      toast.success("Billing settings updated");
    },
  });

  return (
    <div className="grid gap-6">
      <PageHeader title="Billing settings" description="Owner-only bill sequence and currency configuration." />
      <Card>
        <CardHeader>
          <h2 className="font-semibold">Tenant billing defaults</h2>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-2">
          <Input
            maxLength={3}
            placeholder="Currency"
            value={values.defaultCurrencyCode}
            onChange={(event) => setForm({ ...form, defaultCurrencyCode: event.target.value.toUpperCase() })}
          />
          <Input placeholder="Bill prefix" value={values.billPrefix} onChange={(event) => setForm({ ...form, billPrefix: event.target.value })} />
          <Input
            placeholder="Bill number format"
            value={values.billNumberFormat}
            onChange={(event) => setForm({ ...form, billNumberFormat: event.target.value })}
          />
          <Input
            type="number"
            min={1}
            placeholder="Next sequence"
            value={values.nextBillSequence}
            onChange={(event) => setForm({ ...form, nextBillSequence: event.target.value })}
          />
          <div className="md:col-span-2">
            <Button disabled={update.isPending} onClick={() => update.mutate()}>
              {update.isPending ? "Saving..." : "Save settings"}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
