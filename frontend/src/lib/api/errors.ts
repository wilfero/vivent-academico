export class ApiError extends Error {
  readonly status: number;
  readonly errorCode: string | null;

  constructor(message: string, status: number, errorCode: string | null = null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.errorCode = errorCode;
  }
}

export function getMissingPermissions(error: unknown) {
  if (!(error instanceof ApiError) || error.errorCode !== "PERMISSION_DENIED") {
    return [];
  }

  const singlePermission = error.message.match(/^Missing required permission: (.+)$/);

  if (singlePermission) {
    return [singlePermission[1].trim()].filter(Boolean);
  }

  const multiplePermissions = error.message.match(/^Missing required permissions: (.+)$/);

  if (!multiplePermissions) {
    return [];
  }

  return multiplePermissions[1]
    .split(",")
    .map((permission) => permission.trim())
    .filter(Boolean);
}

export function formatPermissionError(
  error: unknown,
  t: (key: string, values?: Record<string, string>) => string,
) {
  const missingPermissions = getMissingPermissions(error);

  if (missingPermissions.length === 0) {
    return null;
  }

  if (missingPermissions.length === 1) {
    return t("errors.missingPermission", { permission: missingPermissions[0] });
  }

  return t("errors.missingPermissions", { permissions: missingPermissions.map((permission) => `"${permission}"`).join(", ") });
}

export function formatApiErrorMessage(
  error: unknown,
  t: (key: string, values?: Record<string, string>) => string,
) {
  const permissionError = formatPermissionError(error, t);

  if (permissionError) {
    return permissionError;
  }

  if (error instanceof ApiError) {
    if (error.errorCode === "INVALID_CURRENT_PASSWORD") {
      return t("errors.invalidCurrentPassword");
    }

    if (error.errorCode === "PASSWORD_CHANGE_REQUIRED") {
      return t("errors.passwordChangeRequired");
    }

    if (error.errorCode === "INSUFFICIENT_STOCK") {
      return t("errors.insufficientStock");
    }

    const translatedMessageKey = translatedBackendMessages[error.message];

    if (translatedMessageKey) {
      return t(translatedMessageKey);
    }
  }

  return error instanceof Error ? error.message : t("admin.states.error");
}

const translatedBackendMessages: Record<string, string> = {
  "Cash closure already exists for this user, branch and date": "errors.cashClosureAlreadyExists",
  "Purchase stock has already been consumed and cannot be voided exactly": "errors.purchaseStockAlreadyConsumed",
  "Purchase stock is reserved and cannot be voided": "errors.purchaseStockReserved",
  "User cannot access another branch": "errors.userCannotAccessAnotherBranch",
  "Unexpected server error": "errors.unexpectedServerError",
};
