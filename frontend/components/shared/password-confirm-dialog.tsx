"use client";

import * as AlertDialog from "@radix-ui/react-alert-dialog";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

export function PasswordConfirmDialog({
  trigger,
  title,
  description,
  passwordLabel = "Password",
  confirmLabel = "Confirm",
  isPending,
  onConfirm,
}: {
  trigger: React.ReactNode;
  title: string;
  description: string;
  passwordLabel?: string;
  confirmLabel?: string;
  isPending?: boolean;
  onConfirm: (password: string) => void;
}) {
  const [open, setOpen] = useState(false);
  const [password, setPassword] = useState("");

  function confirm() {
    onConfirm(password);
    setPassword("");
    setOpen(false);
  }

  return (
    <AlertDialog.Root open={open} onOpenChange={setOpen}>
      <AlertDialog.Trigger asChild>{trigger}</AlertDialog.Trigger>
      <AlertDialog.Portal>
        <AlertDialog.Overlay className="fixed inset-0 z-50 bg-slate-950/40" />
        <AlertDialog.Content className="fixed left-1/2 top-1/2 z-50 w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 rounded-md bg-white p-5 shadow-xl">
          <AlertDialog.Title className="text-lg font-semibold text-slate-950">{title}</AlertDialog.Title>
          <AlertDialog.Description className="mt-2 text-sm text-slate-600">{description}</AlertDialog.Description>
          <div className="mt-4 grid gap-1.5">
            <label className="text-sm font-medium text-slate-700" htmlFor="confirm-password">
              {passwordLabel}
            </label>
            <Input
              id="confirm-password"
              type="password"
              autoFocus
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter" && password && !isPending) {
                  confirm();
                }
              }}
            />
          </div>
          <div className="mt-5 flex justify-end gap-2">
            <AlertDialog.Cancel asChild>
              <Button variant="outline" onClick={() => setPassword("")}>
                Cancel
              </Button>
            </AlertDialog.Cancel>
            <Button disabled={!password || isPending} onClick={confirm}>
              {isPending ? "Checking..." : confirmLabel}
            </Button>
          </div>
        </AlertDialog.Content>
      </AlertDialog.Portal>
    </AlertDialog.Root>
  );
}
