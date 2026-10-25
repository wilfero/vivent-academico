import { z } from "zod";

export const stockManualSchema = z.object({
  operation: z.enum(["IN", "OUT"]),
  branchId: z.string().min(1),
  productId: z.string().min(1),
  quantity: z.coerce.number().positive(),
  unitCost: z.coerce.number().min(0).optional(),
  expirationDate: z.string().nullable(),
  lotNumber: z.string().nullable(),
  notes: z.string().trim().min(1).max(1000),
});

export type StockManualForm = z.infer<typeof stockManualSchema>;
export type StockManualInput = z.input<typeof stockManualSchema>;

export const defaultStockManualValues: StockManualForm = {
  operation: "IN",
  branchId: "",
  productId: "",
  quantity: 1,
  unitCost: 0,
  expirationDate: null,
  lotNumber: "",
  notes: "",
};
