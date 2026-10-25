import { z } from "zod";

export const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, "account.validation.currentPasswordRequired"),
    newPassword: z.string().min(8, "account.validation.passwordLength"),
    confirmPassword: z.string().min(1, "account.validation.confirmPasswordRequired"),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    message: "account.validation.passwordMismatch",
    path: ["confirmPassword"],
  });

export type PasswordFormData = z.infer<typeof passwordSchema>;
