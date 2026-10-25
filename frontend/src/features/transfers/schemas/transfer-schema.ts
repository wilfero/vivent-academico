import { z } from "zod";
import type { ReceiveTransferPayload, TransferPayload } from "@/features/transfers/types/transfer-types";

export const transferItemSchema = z.object({
  presentationId: z.string().min(1),
  quantityRequestedPresentation: z.coerce.number().positive(),
});

export const transferCreateSchema = z.object({
  sourceBranchId: z.string().min(1),
  destinationBranchId: z.string().min(1),
  transferNumber: z.string().optional().default(""),
  notes: z.string().optional().default(""),
  details: z.array(transferItemSchema).min(1),
}).refine((values) => values.sourceBranchId !== values.destinationBranchId, {
  path: ["destinationBranchId"],
  message: "Source and destination branches must be different",
});

export const receiveTransferSchema = z.object({
  details: z.array(z.object({
    detailId: z.string().min(1),
    quantityReceivedBase: z.coerce.number().min(0),
  })).min(1),
});

export const cancelTransferSchema = z.object({
  reason: z.string().trim().min(1),
});

export type TransferCreateInput = z.input<typeof transferCreateSchema>;
export type TransferCreateForm = z.infer<typeof transferCreateSchema>;
export type ReceiveTransferInput = z.input<typeof receiveTransferSchema>;
export type ReceiveTransferForm = z.infer<typeof receiveTransferSchema>;
export type CancelTransferInput = z.input<typeof cancelTransferSchema>;
export type CancelTransferForm = z.infer<typeof cancelTransferSchema>;

export const defaultTransferValues: TransferCreateInput = {
  sourceBranchId: "",
  destinationBranchId: "",
  transferNumber: "",
  notes: "",
  details: [],
};

export function toTransferPayload(values: TransferCreateForm): TransferPayload {
  return {
    sourceBranchId: values.sourceBranchId,
    destinationBranchId: values.destinationBranchId,
    transferNumber: emptyToNull(values.transferNumber),
    notes: emptyToNull(values.notes),
    details: values.details.map((detail) => ({
      presentationId: detail.presentationId,
      quantityRequestedPresentation: detail.quantityRequestedPresentation,
    })),
  };
}

export function toReceiveTransferPayload(values: ReceiveTransferForm): ReceiveTransferPayload {
  return {
    details: values.details.map((detail) => ({
      detailId: detail.detailId,
      quantityReceivedBase: detail.quantityReceivedBase,
    })),
  };
}

function emptyToNull(value: string | undefined) {
  const trimmed = value?.trim() ?? "";
  return trimmed.length === 0 ? null : trimmed;
}
