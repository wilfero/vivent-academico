import { ApiError } from "@/lib/api/errors";
import type { ApiResponse } from "@/lib/api/contracts";

function resolvePlatformApiBaseUrl() {
  if (process.env.NEXT_PUBLIC_PLATFORM_API_BASE_URL) {
    return process.env.NEXT_PUBLIC_PLATFORM_API_BASE_URL;
  }

  return "";
}

type PlatformRequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
};

export async function platformApiRequest<T>(path: string, options: PlatformRequestOptions = {}): Promise<T> {
  const { body, headers, ...requestOptions } = options;
  const requestHeaders = new Headers(headers);

  if (body !== undefined) {
    requestHeaders.set("Content-Type", "application/json");
  }

  const response = await fetch(`${resolvePlatformApiBaseUrl()}${path}`, {
    ...requestOptions,
    headers: requestHeaders,
    credentials: "include",
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  const payload = (await response.json().catch(() => null)) as ApiResponse<T> | null;

  if (!response.ok || !payload?.success) {
    throw new ApiError(payload?.message ?? "Request failed", response.status, payload?.errorCode ?? null);
  }

  return payload.data as T;
}
