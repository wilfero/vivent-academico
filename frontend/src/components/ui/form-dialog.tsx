"use client";

import * as Dialog from "@radix-ui/react-dialog";
import { X } from "lucide-react";
import type { ReactNode } from "react";
import { Button } from "@/components/ui/button";

type FormDialogProps = Readonly<{
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: string;
  children: ReactNode;
}>;

export function FormDialog({ open, onOpenChange, title, description, children }: FormDialogProps) {
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-foreground/35 backdrop-blur-[3px]" />
        <Dialog.Content className="fixed left-1/2 top-1/2 z-50 max-h-[calc(100vh-2rem)] w-[calc(100vw-2rem)] max-w-4xl -translate-x-1/2 -translate-y-1/2 overflow-hidden rounded-[1.5rem] border bg-card text-card-foreground shadow-2xl shadow-foreground/20 outline-none">
          <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-primary via-warehouse-line to-accent" />
          <div className="flex items-start justify-between gap-4 border-b p-6">
            <div>
              <Dialog.Title className="text-2xl font-black tracking-[-0.05em]">{title}</Dialog.Title>
              <Dialog.Description className="mt-2 max-w-2xl text-sm leading-6 text-muted-foreground">{description}</Dialog.Description>
            </div>
            <Dialog.Close asChild>
              <Button className="h-9 w-9 shrink-0 px-0" type="button" variant="outline" aria-label="Cerrar diálogo">
                <X className="h-4 w-4" aria-hidden="true" />
              </Button>
            </Dialog.Close>
          </div>
          <div className="max-h-[calc(100vh-11rem)] overflow-y-auto p-6">{children}</div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
