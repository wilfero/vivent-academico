import { z } from "zod";
import type { AdjustmentPayload } from "@/features/adjustments/types/adjustment-types";

export const adjustmentStatuses = ["DRAFT", "PENDING_APPROVAL", "APPROVED", "REJECTED", "CANCELLED"] as const;
export const adjustmentTypes = ["PHYSICAL_COUNT", "DAMAGE", "LOSS", "THEFT", "EXPIRED", "INTERNAL_USE", "REGISTER_ERROR", "SUPPLIER_RETURN", "CUSTOM"] as const;
export const adjustmentDirections = ["IN", "OUT"] as const;

export const adjustmentItemSchema = z.object({
  productId: z.string().min(1, "Selecciona un producto"),
  presentationId: z.string().min(1, "Selecciona una presentación"),
  quantityPresentation: z.coerce.number().positive("La cantidad debe ser mayor a 0"),
  unitCost: z.coerce.number().min(0, "El costo no puede ser negativo").optional().or(z.literal("")),
  notes: z.string().optional(),
});

export const adjustmentCreateSchema = z.object({
  branchId: z.string().min(1, "Selecciona una sucursal"),
  adjustmentNumber: z.string().optional(),
  adjustmentType: z.enum(adjustmentTypes),
  direction: z.enum(adjustmentDirections),
  status: z.enum(["DRAFT", "PENDING_APPROVAL"]),
  notes: z.string().trim().min(1, "El motivo es obligatorio"),
  lines: z.array(adjustmentItemSchema).min(1, "Agrega al menos un producto"),
});

export const reasonSchema = z.object({
  reason: z.string().trim().min(1, "El motivo es obligatorio"),
});

export type AdjustmentCreateInput = z.input<typeof adjustmentCreateSchema>;
export type AdjustmentCreateForm = z.infer<typeof adjustmentCreateSchema>;
export type ReasonInput = z.input<typeof reasonSchema>;
export type ReasonForm = z.infer<typeof reasonSchema>;

export function toAdjustmentPayload(values: AdjustmentCreateForm): AdjustmentPayload {
  return {
    branchId: values.branchId,
    locationId: null,
    adjustmentNumber: values.adjustmentNumber?.trim() ? values.adjustmentNumber.trim() : null,
    adjustmentType: values.adjustmentType,
    direction: values.direction,
    status: values.status,
    notes: values.notes.trim(),
    lines: values.lines.map((line) => ({
      productId: null,
      presentationId: line.presentationId,
      quantityPresentation: line.quantityPresentation,
      quantityBase: null,
      unitCost: line.unitCost === "" || line.unitCost === undefined ? null : Number(line.unitCost),
      notes: line.notes?.trim() ? line.notes.trim() : null,
    })),
  };
}
