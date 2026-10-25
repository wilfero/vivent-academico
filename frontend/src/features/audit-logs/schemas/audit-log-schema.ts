import { z } from "zod";

export const auditLogFiltersSchema = z.object({
  entityType: z.string().optional(),
  entityId: z.string().uuid("Ingresa un UUID válido").optional().or(z.literal("")),
});

export const auditActions = ["CREATE", "UPDATE", "DELETE", "DEACTIVATE", "LOGIN", "LOGOUT", "CONFIRM", "VOID", "CANCEL", "APPROVE", "REJECT", "DISPATCH", "RECEIVE", "AUTHORIZE"] as const;
