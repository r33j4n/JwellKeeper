"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { PageHeader } from "@/components/shared/page-header";
import { StatusBadge } from "@/components/shared/status-badge";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { staffApi } from "@/lib/api/queries";
import type { UserRole } from "@/lib/api/types";

export function StaffSettingsPage() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<{ name: string; email: string; password: string; role: UserRole }>({ name: "", email: "", password: "", role: "STAFF" });
  const staff = useQuery({ queryKey: ["staff"], queryFn: () => staffApi.list({ size: 50 }) });
  const create = useMutation({
    mutationFn: () => staffApi.create(form),
    onSuccess: () => {
      setForm({ name: "", email: "", password: "", role: "STAFF" });
      queryClient.invalidateQueries({ queryKey: ["staff"] });
      toast.success("Staff user created");
    },
  });
  const deactivate = useMutation({
    mutationFn: staffApi.deactivate,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["staff"] });
      toast.success("Staff user deactivated");
    },
  });

  return (
    <div className="grid gap-6">
      <PageHeader title="Staff" description="Owner-only staff access management." />
      <Card>
        <CardHeader>
          <h2 className="font-semibold">Create staff user</h2>
        </CardHeader>
        <CardContent className="grid gap-3 md:grid-cols-5">
          <Input placeholder="Name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
          <Input placeholder="Email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} />
          <Input type="password" placeholder="Password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} />
          <Select value={form.role} onChange={(event) => setForm({ ...form, role: event.target.value as UserRole })}>
            <option value="STAFF">Staff legacy</option>
            <option value="MANAGER">Manager</option>
            <option value="CASHIER">Cashier</option>
            <option value="STOCK_KEEPER">Stock keeper</option>
          </Select>
          <Button disabled={!form.name || !form.email || form.password.length < 8 || create.isPending} onClick={() => create.mutate()}>
            Create
          </Button>
        </CardContent>
      </Card>
      <Card className="overflow-hidden">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-100 text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-3">Name</th>
              <th className="px-4 py-3">Email</th>
              <th className="px-4 py-3">Role</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {staff.data?.content.map((user) => (
              <tr key={user.id}>
                <td className="px-4 py-3 font-medium">{user.name}</td>
                <td className="px-4 py-3">{user.email}</td>
                <td className="px-4 py-3">{user.role}</td>
                <td className="px-4 py-3">
                  <StatusBadge value={user.active ? "STAFF" : "SOLD"} />
                </td>
                <td className="px-4 py-3">
                  {user.active ? (
                    <ConfirmDialog
                      trigger={<Button size="sm" variant="outline">Deactivate</Button>}
                      title="Deactivate staff user?"
                      description="This user will no longer be able to log in."
                      confirmLabel="Deactivate"
                      danger
                      onConfirm={() => deactivate.mutate(user.id)}
                    />
                  ) : (
                    "-"
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
