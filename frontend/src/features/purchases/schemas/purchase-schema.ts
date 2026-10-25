import { z } from "zod";

const writablePurchaseStatusSchema = z.enum(["DRAFT", "PENDING_APPROVAL"]);
const paymentMethodSchema = z.enum(["CASH", "CARD", "TRANSFER", "CHECK", "OTHER"]);
const paymentTypeSchema = z.enum(["CASH", "CREDIT"]);

export const purchaseDetailSchema = z.object({
  presentationId: z.string().min(1),
  quantityPresentation: z.coerce.number().positive(),
  unitCostPresentation: z.coerce.number().min(0),
  taxAmount: z.coerce.number().min(0).default(0),
  expirationDate: z.string().optional().default(""),
  lotNumber: z.string().optional().default(""),
});

export const purchaseSchema = z.object({
  branchId: z.string().min(1),
  supplierId: z.string().min(1),
  purchaseNumber: z.string().optional().default(""),
  supplierInvoiceNumber: z.string().optional().default(""),
  purchaseDate: z.string().min(1),
  paymentMethod: paymentMethodSchema,
  paymentType: paymentTypeSchema,
  status: writablePurchaseStatusSchema,
  discountAmount: z.coerce.number().min(0).default(0),
  notes: z.string().optional().default(""),
  details: z.array(purchaseDetailSchema).min(1),
});

export type PurchaseFormInput = z.input<typeof purchaseSchema>;
export type PurchaseForm = z.infer<typeof purchaseSchema>;

export const defaultPurchaseValues: PurchaseFormInput = {
  branchId: "",
  supplierId: "",
  purchaseNumber: "",
  supplierInvoiceNumber: "",
  purchaseDate: new Date().toISOString().slice(0, 10),
  paymentMethod: "CASH",
  paymentType: "CASH",
  status: "DRAFT",
  discountAmount: 0,
  notes: "",
  details: [{ presentationId: "", quantityPresentation: 1, unitCostPresentation: 0, taxAmount: 0, expirationDate: "", lotNumber: "" }],
};

export function toPurchasePayload(values: PurchaseForm) {
  return {
    branchId: values.branchId,
    supplierId: values.supplierId,
    purchaseNumber: emptyToNull(values.purchaseNumber),
    supplierInvoiceNumber: emptyToNull(values.supplierInvoiceNumber),
    purchaseDate: values.purchaseDate,
    paymentMethod: values.paymentMethod,
    paymentType: values.paymentType,
    status: values.status,
    discountAmount: values.discountAmount,
    notes: emptyToNull(values.notes),
    details: values.details.map((detail) => ({
      presentationId: detail.presentationId,
      quantityPresentation: detail.quantityPresentation,
      unitCostPresentation: detail.unitCostPresentation,
      taxAmount: detail.taxAmount,
      expirationDate: emptyToNull(detail.expirationDate),
      lotNumber: emptyToNull(detail.lotNumber),
    })),
  };
}

function emptyToNull(value: string | undefined) {
  const trimmed = value?.trim() ?? "";
  return trimmed.length === 0 ? null : trimmed;
}
