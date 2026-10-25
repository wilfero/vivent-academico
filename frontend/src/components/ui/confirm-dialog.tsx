"use client";

import * as AlertDialog from "@radix-ui/react-alert-dialog";
import { AlertTriangle } from "lucide-react";
import type { ReactNode } from "react";
import { Button } from "@/components/ui/button";

type ConfirmDialogProps = Readonly<{
  trigger: ReactNode;
  title: string;
  description: string;
  confirmLabel: string;
  cancelLabel: string;
  onConfirm: () => void;
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
}>;

export function ConfirmDialog({ trigger, title, description, confirmLabel, cancelLabel, onConfirm, open, onOpenChange }: ConfirmDialogProps) {
  return (
    <AlertDialog.Root open={open} onOpenChange={onOpenChange}>
      <AlertDialog.Trigger asChild>{trigger}</AlertDialog.Trigger>
      <AlertDialog.Portal>
        <AlertDialog.Overlay className="fixed inset-0 z-50 bg-foreground/35 backdrop-blur-[3px] data-[state=open]:animate-in" />
        <AlertDialog.Content className="fixed left-1/2 top-1/2 z-50 w-[calc(100vw-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 overflow-hidden rounded-[1.5rem] border bg-card text-card-foreground shadow-2xl shadow-foreground/20 outline-none">
          <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-destructive via-warehouse-line to-primary" />
          <div className="p-6">
            <div className="mb-5 flex items-start gap-4">
              <div className="scanline flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-destructive/12 text-destructive ring-1 ring-destructive/25">
                <AlertTriangle className="h-5 w-5" aria-hidden="true" />
              </div>
              <div className="min-w-0">
                <AlertDialog.Title className="text-xl font-black tracking-[-0.04em]">{title}</AlertDialog.Title>
                <AlertDialog.Description className="mt-2 text-sm leading-6 text-muted-foreground">{description}</AlertDialog.Description>
              </div>
            </div>
            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <AlertDialog.Cancel asChild>
                <Button type="button" variant="outline">{cancelLabel}</Button>
              </AlertDialog.Cancel>
              <AlertDialog.Action asChild>
                <Button type="button" className="bg-destructive text-destructive-foreground shadow-destructive/20 hover:bg-destructive/90" onClick={onConfirm}>{confirmLabel}</Button>
              </AlertDialog.Action>
            </div>
          </div>
        </AlertDialog.Content>
      </AlertDialog.Portal>
    </AlertDialog.Root>
  );
}
