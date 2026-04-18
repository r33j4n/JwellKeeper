"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Camera, ImagePlus, Lock, Plus, RefreshCw, StoreIcon, X, ZapOff } from "lucide-react";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { FormField } from "@/components/forms/form-field";
import { PageHeader } from "@/components/shared/page-header";
import { jewelleryApi, shopApi } from "@/lib/api/queries";
import { jewellerySchema } from "@/lib/validation/schemas";
import { cn } from "@/lib/utils/cn";

type JewelleryFormValues = { typeId: string; karat: string; designName?: string; weight: string; notes?: string };

// ─── Camera modal ────────────────────────────────────────────────────────────

function CameraModal({ onCapture, onClose }: { onCapture: (file: File) => void; onClose: () => void }) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const [facingMode, setFacingMode] = useState<"environment" | "user">("environment");
  const [error, setError] = useState<string | null>(null);
  const [ready, setReady] = useState(false);

  const startCamera = useCallback(async (facing: "environment" | "user") => {
    // Stop any existing stream first
    streamRef.current?.getTracks().forEach((t) => t.stop());
    setReady(false);
    setError(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: facing, width: { ideal: 1280 }, height: { ideal: 960 } },
      });
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        videoRef.current.onloadedmetadata = () => setReady(true);
      }
    } catch (err) {
      const msg = err instanceof DOMException && err.name === "NotAllowedError"
        ? "Camera permission denied. Allow access in your browser settings."
        : "Could not access camera. Try uploading a photo instead.";
      setError(msg);
    }
  }, []);

  useEffect(() => {
    startCamera(facingMode);
    return () => { streamRef.current?.getTracks().forEach((t) => t.stop()); };
  }, [facingMode, startCamera]);

  // Close on Escape
  useEffect(() => {
    const handler = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [onClose]);

  function capture() {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas) return;
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext("2d")?.drawImage(video, 0, 0);
    canvas.toBlob((blob) => {
      if (!blob) return;
      onCapture(new File([blob], `capture-${Date.now()}.jpg`, { type: "image/jpeg" }));
      onClose();
    }, "image/jpeg", 0.92);
  }

  function flipCamera() {
    setFacingMode((prev) => (prev === "environment" ? "user" : "environment"));
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 p-4 backdrop-blur-sm">
      <div className="relative flex w-full max-w-lg flex-col rounded-xl bg-slate-900 shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-3 border-b border-slate-700">
          <div className="flex items-center gap-2 text-white">
            <Camera className="h-4 w-4 text-amber-400" />
            <span className="text-sm font-medium">Take a photo</span>
          </div>
          <button
            onClick={onClose}
            className="rounded-md p-1.5 text-slate-400 hover:bg-slate-700 hover:text-white transition-colors"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Viewfinder */}
        <div className="relative aspect-[4/3] w-full bg-slate-950">
          {error ? (
            <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 p-6 text-center">
              <ZapOff className="h-10 w-10 text-slate-500" />
              <p className="text-sm text-slate-400">{error}</p>
            </div>
          ) : (
            <video
              ref={videoRef}
              autoPlay
              playsInline
              muted
              className={cn("h-full w-full object-cover transition-opacity", ready ? "opacity-100" : "opacity-0")}
            />
          )}
          {!ready && !error && (
            <div className="absolute inset-0 flex items-center justify-center">
              <div className="h-8 w-8 rounded-full border-2 border-amber-400 border-t-transparent animate-spin" />
            </div>
          )}
          {/* Corner guides */}
          {ready && (
            <>
              <div className="pointer-events-none absolute inset-8 rounded-lg border border-white/20" />
            </>
          )}
          {/* Canvas (hidden, used for capture) */}
          <canvas ref={canvasRef} className="hidden" />
        </div>

        {/* Controls */}
        <div className="flex items-center justify-between px-6 py-4 bg-slate-900">
          <button
            onClick={flipCamera}
            disabled={!!error}
            className="flex h-10 w-10 items-center justify-center rounded-full text-slate-400 hover:bg-slate-700 hover:text-white transition-colors disabled:opacity-30"
            title="Flip camera"
          >
            <RefreshCw className="h-5 w-5" />
          </button>

          {/* Shutter */}
          <button
            onClick={capture}
            disabled={!ready || !!error}
            className="flex h-16 w-16 items-center justify-center rounded-full border-4 border-white bg-white/10 hover:bg-white/20 transition-all active:scale-95 disabled:opacity-30"
            title="Capture"
          >
            <div className="h-10 w-10 rounded-full bg-white" />
          </button>

          <div className="w-10" />
        </div>
      </div>
    </div>
  );
}

// ─── Main form ───────────────────────────────────────────────────────────────

export function JewelleryCreatePage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [customType, setCustomType] = useState("");
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [cameraOpen, setCameraOpen] = useState(false);
  const uploadInputRef = useRef<HTMLInputElement>(null);

  const types = useQuery({ queryKey: ["jewellery-types"], queryFn: jewelleryApi.types });
  const shopState = useQuery({ queryKey: ["shop-state"], queryFn: () => shopApi.state() });
  const shopClosed = shopState.data?.status === "CLOSED";
  const form = useForm<JewelleryFormValues>({
    resolver: zodResolver(jewellerySchema),
    defaultValues: { karat: "22K", designName: "", weight: "", notes: "" },
  });

  function handleFileSelected(file: File | undefined) {
    if (!file) return;
    if (imagePreview) URL.revokeObjectURL(imagePreview);
    setImageFile(file);
    setImagePreview(URL.createObjectURL(file));
  }

  function clearImage() {
    setImageFile(null);
    if (imagePreview) URL.revokeObjectURL(imagePreview);
    setImagePreview(null);
    if (uploadInputRef.current) uploadInputRef.current.value = "";
  }

  const createType = useMutation({
    mutationFn: jewelleryApi.createType,
    onSuccess: async (type) => {
      await queryClient.invalidateQueries({ queryKey: ["jewellery-types"] });
      form.setValue("typeId", type.id);
      setCustomType("");
      toast.success("Jewellery type added");
    },
  });

  const create = useMutation({
    mutationFn: async (payload: { typeId: string; karat: string; designName?: string | null; weight: string; notes?: string | null }) => {
      const item = await jewelleryApi.create(payload);
      if (imageFile) {
        try {
          await jewelleryApi.uploadImage(item.id, imageFile);
        } catch {
          toast.error("Item created, but image upload failed. Retry from the detail page.");
        }
      }
      return item;
    },
    onSuccess: (item) => {
      queryClient.invalidateQueries({ queryKey: ["jewellery"] });
      toast.success("Jewellery added successfully");
      router.replace(`/dashboard/jewellery/${item.id}`);
    },
  });

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      {cameraOpen && (
        <CameraModal
          onCapture={(file) => { handleFileSelected(file); setCameraOpen(false); }}
          onClose={() => setCameraOpen(false)}
        />
      )}

      <PageHeader
        title="Add New Jewellery"
        description="Fill in the details below. An image is optional but helps with identification."
      />

      {shopClosed && (
        <div className="flex items-start gap-3 rounded-xl border border-rose-200 bg-rose-50 p-4">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-rose-100">
            <StoreIcon className="h-4 w-4 text-rose-600" />
          </div>
          <div>
            <p className="text-sm font-semibold text-rose-800">Shop is currently closed</p>
            <p className="mt-0.5 text-sm text-rose-700">
              You cannot add jewellery while the shop is closed. Reopen the shop from the{" "}
              <a href="/dashboard" className="font-semibold underline underline-offset-2">Dashboard</a>
              {" "}first.
            </p>
          </div>
        </div>
      )}

      <form
        onSubmit={form.handleSubmit((values) =>
          create.mutate({
            ...values,
            designName: values.designName?.trim() || null,
            notes: values.notes?.trim() || null,
          }),
        )}
        className="space-y-5"
      >
        {/* Photo section */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-md bg-amber-100">
                <ImagePlus className="h-4 w-4 text-amber-700" />
              </div>
              <h2 className="text-sm font-semibold text-slate-900">
                Photo <span className="font-normal text-slate-400">(optional)</span>
              </h2>
            </div>
          </CardHeader>
          <CardContent>
            {imagePreview ? (
              <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
                <div className="relative w-full max-w-[180px]">
                  <img
                    src={imagePreview}
                    alt="Preview"
                    className="aspect-square w-full rounded-lg border border-amber-100 object-cover shadow-sm"
                  />
                  <button
                    type="button"
                    onClick={clearImage}
                    className="absolute -right-2 -top-2 flex h-6 w-6 items-center justify-center rounded-full bg-rose-600 text-white shadow transition hover:bg-rose-700"
                  >
                    <X className="h-3.5 w-3.5" />
                  </button>
                </div>
                <div className="flex flex-col gap-2">
                  <p className="text-sm text-slate-500">Image selected. Replace it?</p>
                  <div className="flex flex-wrap gap-2">
                    <Button type="button" variant="outline" size="sm" onClick={() => uploadInputRef.current?.click()}>
                      <ImagePlus className="h-4 w-4" />
                      Upload different
                    </Button>
                    <Button type="button" variant="outline" size="sm" onClick={() => setCameraOpen(true)}>
                      <Camera className="h-4 w-4" />
                      Retake
                    </Button>
                  </div>
                </div>
              </div>
            ) : (
              <div className="flex flex-wrap gap-3">
                <button
                  type="button"
                  onClick={() => uploadInputRef.current?.click()}
                  className="flex flex-1 flex-col items-center gap-2 rounded-lg border-2 border-dashed border-slate-200 p-6 text-center transition hover:border-amber-400 hover:bg-amber-50 sm:flex-none sm:min-w-[160px]"
                >
                  <div className="flex h-10 w-10 items-center justify-center rounded-full bg-slate-100">
                    <ImagePlus className="h-5 w-5 text-slate-500" />
                  </div>
                  <div>
                    <p className="text-sm font-medium text-slate-700">Upload photo</p>
                    <p className="mt-0.5 text-xs text-slate-400">PNG, JPG, WEBP</p>
                  </div>
                </button>
                <button
                  type="button"
                  onClick={() => setCameraOpen(true)}
                  className="flex flex-1 flex-col items-center gap-2 rounded-lg border-2 border-dashed border-slate-200 p-6 text-center transition hover:border-amber-400 hover:bg-amber-50 sm:flex-none sm:min-w-[160px]"
                >
                  <div className="flex h-10 w-10 items-center justify-center rounded-full bg-slate-100">
                    <Camera className="h-5 w-5 text-slate-500" />
                  </div>
                  <div>
                    <p className="text-sm font-medium text-slate-700">Take photo</p>
                    <p className="mt-0.5 text-xs text-slate-400">Webcam / rear camera</p>
                  </div>
                </button>
              </div>
            )}
            <input
              ref={uploadInputRef}
              type="file"
              accept="image/png,image/jpeg,image/webp"
              className="hidden"
              onChange={(e) => handleFileSelected(e.target.files?.[0])}
            />
          </CardContent>
        </Card>

        {/* Item details */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-md bg-amber-100">
                <span className="text-sm">💎</span>
              </div>
              <h2 className="text-sm font-semibold text-slate-900">Item details</h2>
            </div>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
            <FormField label="Type" error={form.formState.errors.typeId?.message}>
              <Select {...form.register("typeId")}>
                <option value="">Select type</option>
                {types.data?.map((type) => (
                  <option key={type.id} value={type.id}>{type.name}</option>
                ))}
              </Select>
            </FormField>

            <FormField label="Sub-type / Design Name" error={form.formState.errors.designName?.message}>
              <Input placeholder="e.g. Floral design" {...form.register("designName")} />
            </FormField>

            <FormField label="Karat" error={form.formState.errors.karat?.message}>
              <Input
                placeholder="e.g. 22K"
                {...form.register("karat")}
                onChange={(e) => form.setValue("karat", e.target.value.toUpperCase())}
              />
            </FormField>

            <FormField label="Weight (g)" error={form.formState.errors.weight?.message}>
              <Input inputMode="decimal" placeholder="0.001" {...form.register("weight")} />
            </FormField>

            <FormField label="Availability">
              <Select value="AVAILABLE" disabled>
                <option value="AVAILABLE">Available</option>
              </Select>
            </FormField>

            <FormField label="Notes" error={form.formState.errors.notes?.message} className="sm:col-span-2">
              <Textarea placeholder="Optional notes about this item" rows={3} {...form.register("notes")} />
            </FormField>
          </CardContent>
        </Card>

        {/* Custom type */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-md bg-amber-100">
                <Plus className="h-4 w-4 text-amber-700" />
              </div>
              <div>
                <h2 className="text-sm font-semibold text-slate-900">Custom type</h2>
                <p className="text-xs text-slate-500">Add a new jewellery type if it&apos;s not listed above</p>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <div className="flex flex-col gap-3 sm:flex-row">
              <Input
                placeholder="Type name, e.g. Bangle"
                value={customType}
                onChange={(e) => setCustomType(e.target.value)}
                className="sm:max-w-xs"
              />
              <Button
                type="button"
                variant="outline"
                disabled={!customType.trim() || createType.isPending}
                onClick={() => createType.mutate(customType.trim())}
              >
                <Plus className="h-4 w-4" />
                {createType.isPending ? "Adding..." : "Add type"}
              </Button>
            </div>
          </CardContent>
        </Card>

        {/* Submit */}
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between rounded-lg border border-amber-100 bg-amber-50 p-4">
          <p className="text-sm text-slate-600">A QR token will be generated automatically on creation.</p>
          <Button type="submit" size="lg" disabled={create.isPending || shopClosed} className="sm:flex-none">
            {shopClosed ? <><Lock className="h-4 w-4" />Shop closed</> : create.isPending ? "Adding item..." : "Add jewellery"}
          </Button>
        </div>
      </form>
    </div>
  );
}
