"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { PageHeader } from "@/components/shared/page-header";
import { StatusBadge } from "@/components/shared/status-badge";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { QrImageCard } from "@/components/qr/qr-image-card";
import { jewelleryApi } from "@/lib/api/queries";
import { useAuthStore } from "@/lib/store/auth-store";
import { formatDateTime, formatWeight } from "@/lib/utils/format";

export function JewelleryDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const router = useRouter();
  const queryClient = useQueryClient();
  const user = useAuthStore((state) => state.user);
  const [ownerPassword, setOwnerPassword] = useState("");
  const [adjustPassword, setAdjustPassword] = useState("");
  const [adjustReason, setAdjustReason] = useState("");
  const [adjustKarat, setAdjustKarat] = useState("");
  const [adjustWeight, setAdjustWeight] = useState("");
  const [adjustTypeId, setAdjustTypeId] = useState("");
  const canManageStock = user?.role === "OWNER" || user?.role === "MANAGER";

  const item = useQuery({ queryKey: ["jewellery", id], queryFn: () => jewelleryApi.get(id) });
  const qr = useQuery({ queryKey: ["jewellery-qr", id], queryFn: () => jewelleryApi.qr(id), enabled: Boolean(item.data) });
  const types = useQuery({ queryKey: ["jewellery-types"], queryFn: jewelleryApi.types });
  const images = useQuery({ queryKey: ["jewellery-images", id], queryFn: () => jewelleryApi.images(id), enabled: Boolean(item.data) });

  const remove = useMutation({
    mutationFn: () => jewelleryApi.remove(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["jewellery"] });
      toast.success("Jewellery deleted");
      router.replace("/dashboard/jewellery");
    },
  });

  const markFound = useMutation({
    mutationFn: () => jewelleryApi.markFound(id, ownerPassword),
    onSuccess: () => {
      setOwnerPassword("");
      queryClient.invalidateQueries({ queryKey: ["jewellery", id] });
      queryClient.invalidateQueries({ queryKey: ["jewellery"] });
      toast.success("Jewellery restored");
    },
  });
  const uploadImage = useMutation({
    mutationFn: (file: File) => jewelleryApi.uploadImage(id, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["jewellery-images", id] });
      toast.success("Image uploaded");
    },
  });
  const primaryImage = useMutation({
    mutationFn: (imageId: string) => jewelleryApi.primaryImage(id, imageId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["jewellery-images", id] }),
  });
  const deleteImage = useMutation({
    mutationFn: (imageId: string) => jewelleryApi.deleteImage(id, imageId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["jewellery-images", id] });
      toast.success("Image deleted");
    },
  });
  const adjust = useMutation({
    mutationFn: (archive: boolean) =>
      jewelleryApi.adjust(id, {
        password: adjustPassword,
        reason: adjustReason,
        typeId: adjustTypeId || null,
        karat: adjustKarat || null,
        weight: adjustWeight || null,
        archive,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["jewellery", id] });
      queryClient.invalidateQueries({ queryKey: ["jewellery"] });
      setAdjustPassword("");
      setAdjustReason("");
      setAdjustKarat("");
      setAdjustWeight("");
      setAdjustTypeId("");
      toast.success("Stock adjustment recorded");
    },
  });

  if (item.isLoading || !item.data) {
    return <div className="rounded-md bg-white p-6">Loading jewellery...</div>;
  }

  return (
    <div className="grid gap-6">
      <PageHeader
        title={`${item.data.typeName}${item.data.designName ? ` · ${item.data.designName}` : ""}`}
        description="Stock details and printable QR label."
        actions={
          <>
            <Button asChild variant="outline">
              <Link href="/dashboard/jewellery">Back</Link>
            </Button>
            <Button asChild>
              <Link href="/dashboard/jewellery/create">Add another jewellery</Link>
            </Button>
            {canManageStock && item.data.status !== "SOLD" ? (
              <ConfirmDialog
                trigger={<Button variant="danger">Delete</Button>}
                title="Delete jewellery?"
                description="This soft deletes the jewellery item. It will no longer appear in stock lists."
                confirmLabel="Delete"
                danger
                onConfirm={() => remove.mutate()}
              />
            ) : null}
          </>
        }
      />
      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        <Card>
          <CardHeader>
            <h2 className="font-semibold">Details</h2>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
            <Info label="Type" value={item.data.typeName} />
            <Info label="Sub-type / Design" value={item.data.designName || "-"} />
            <Info label="Karat" value={item.data.karat} />
            <Info label="Weight" value={formatWeight(item.data.weight)} />
            <Info label="Status" value={<StatusBadge value={item.data.status} />} />
            <Info label="Bill no" value={item.data.billNo || "-"} />
            <Info label="Created" value={formatDateTime(item.data.createdAt)} />
            <Info label="Sold at" value={formatDateTime(item.data.soldAt)} />
            <Info label="Notes" value={item.data.notes || "-"} />
          </CardContent>
        </Card>
        <QrImageCard jewellery={item.data} image={qr.data?.qrCodeBase64} />
      </div>
      <Card>
        <CardHeader>
          <h2 className="font-semibold">Images</h2>
          <p className="text-sm text-slate-500">Upload camera or product photos. Images are served through authenticated backend routes.</p>
        </CardHeader>
        <CardContent className="grid gap-4">
          <Input
            type="file"
            accept="image/png,image/jpeg,image/webp"
            disabled={uploadImage.isPending}
            onChange={(event) => {
              const file = event.target.files?.[0];
              if (file) uploadImage.mutate(file);
              event.currentTarget.value = "";
            }}
          />
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {images.data?.map((image) => (
              <div key={image.id} className="rounded-md border border-slate-200 bg-white p-2">
                <img src={image.url.replace(/^\/api/, "/api/backend")} alt="Jewellery" className="aspect-square w-full rounded-md object-cover" />
                <div className="mt-2 flex flex-wrap gap-2">
                  <Button size="sm" variant={image.primary ? "secondary" : "outline"} disabled={image.primary} onClick={() => primaryImage.mutate(image.id)}>
                    {image.primary ? "Primary" : "Make primary"}
                  </Button>
                  <Button size="sm" variant="danger" onClick={() => deleteImage.mutate(image.id)}>
                    Delete
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
      {canManageStock && item.data.status !== "SOLD" ? (
        <Card>
          <CardHeader>
            <h2 className="font-semibold">Stock adjustment</h2>
            <p className="text-sm text-slate-500">Use this only for stock-critical corrections. Every adjustment writes the stock ledger.</p>
          </CardHeader>
          <CardContent className="grid gap-3 md:grid-cols-2">
            <select className="h-10 rounded-md border border-slate-300 bg-white px-3 text-sm" value={adjustTypeId} onChange={(event) => setAdjustTypeId(event.target.value)}>
              <option value="">Keep type</option>
              {types.data?.map((type) => (
                <option key={type.id} value={type.id}>
                  {type.name}
                </option>
              ))}
            </select>
            <Input placeholder="New karat, e.g. 22K" value={adjustKarat} onChange={(event) => setAdjustKarat(event.target.value.toUpperCase())} />
            <Input inputMode="decimal" placeholder="New weight" value={adjustWeight} onChange={(event) => setAdjustWeight(event.target.value)} />
            <Input placeholder="Reason" value={adjustReason} onChange={(event) => setAdjustReason(event.target.value)} />
            <Input type="password" placeholder="Owner/manager password" value={adjustPassword} onChange={(event) => setAdjustPassword(event.target.value)} />
            <div className="flex flex-wrap gap-2">
              <Button disabled={!adjustPassword || !adjustReason.trim() || adjust.isPending} onClick={() => adjust.mutate(false)}>
                Save adjustment
              </Button>
              <Button variant="danger" disabled={!adjustPassword || !adjustReason.trim() || adjust.isPending} onClick={() => adjust.mutate(true)}>
                Archive item
              </Button>
            </div>
          </CardContent>
        </Card>
      ) : null}
      {canManageStock && item.data.status === "MISSING" ? (
        <Card>
          <CardHeader>
            <h2 className="font-semibold">Restore missing jewellery</h2>
          </CardHeader>
          <CardContent className="flex flex-col gap-3 sm:flex-row">
            <Input
              type="password"
              placeholder="Owner password"
              value={ownerPassword}
              onChange={(event) => setOwnerPassword(event.target.value)}
            />
            <Button disabled={!ownerPassword || markFound.isPending} onClick={() => markFound.mutate()}>
              Mark found
            </Button>
          </CardContent>
        </Card>
      ) : null}
    </div>
  );
}

function Info({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <p className="text-xs font-semibold uppercase text-slate-500">{label}</p>
      <div className="mt-1 text-sm font-medium text-slate-900">{value}</div>
    </div>
  );
}
