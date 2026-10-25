import { z } from "zod";
import type { SalePayload } from "@/features/sales/types/sale-types";

const salePaymentMethodSchema = z.enum(["CASH", "CARD", "TRANSFER", "CREDIT", "OTHER"]);

export const saleItemSchema = z.object({
  presentationId: z.string().min(1),
  barcode: z.string().optional().default(""),
  quantityPresentation: z.coerce.number().positive(),
  unitPrice: z.coerce.number().min(0),
  discountAmount: z.coerce.number().min(0).default(0),
  taxAmount: z.coerce.number().min(0).default(0),
});

export const saleComboLineSchema = z.object({
  comboOfferId: z.string().min(1),
  quantity: z.coerce.number().positive(),
});

export const saleCreateSchema = z.object({
  branchId: z.string().min(1),
  customerId: z.string().optional().default(""),
  saleNumber: z.string().optional().default(""),
  paymentMethod: salePaymentMethodSchema,
  discountAmount: z.coerce.number().min(0).default(0),
  notes: z.string().optional().default(""),
  details: z.array(saleItemSchema).default([]),
  comboLines: z.array(saleComboLineSchema).default([]),
}).refine((data) => data.details.length > 0 || data.comboLines.length > 0, {
  message: "sale must have at least one product or combo",
  path: ["details"],
});

export const cancelSaleSchema = z.object({
  reason: z.string().trim().min(1),
});

export type SaleCreateInput = z.input<typeof saleCreateSchema>;
export type SaleCreateForm = z.infer<typeof saleCreateSchema>;
export type CancelSaleForm = z.infer<typeof cancelSaleSchema>;

export const defaultSaleCreateValues: SaleCreateInput = {
  branchId: "",
  customerId: "",
  saleNumber: "",
  paymentMethod: "CASH",
  discountAmount: 0,
  notes: "",
  details: [],
  comboLines: [],
};

export function toSalePayload(values: SaleCreateForm, allowDiscounts: boolean): SalePayload {
  return {
    branchId: values.branchId,
    customerId: emptyToNull(values.customerId),
    saleNumber: emptyToNull(values.saleNumber),
    paymentMethod: values.paymentMethod,
    status: "DRAFT",
    discountAmount: allowDiscounts ? values.discountAmount : 0,
    notes: emptyToNull(values.notes),
    details: values.details.map((detail) => ({
      presentationId: detail.presentationId,
      barcode: emptyToNull(detail.barcode),
      quantityPresentation: detail.quantityPresentation,
      unitPrice: detail.unitPrice,
      discountAmount: allowDiscounts ? detail.discountAmount : 0,
      taxAmount: detail.taxAmount,
    })),
    comboLines: (values.comboLines ?? []).map((line) => ({
      comboOfferId: line.comboOfferId,
      quantity: line.quantity,
    })),
  };
}

function emptyToNull(value: string | undefined) {
  const trimmed = value?.trim() ?? "";
  return trimmed.length === 0 ? null : trimmed;
}
