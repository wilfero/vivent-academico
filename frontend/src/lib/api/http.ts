import { ApiError } from "@/lib/api/errors";
import type { ApiResponse } from "@/lib/api/contracts";
import { getStoredAccessToken } from "@/features/auth/lib/session-storage";

const DEFAULT_API_BASE_URL = "";

function isPrivateLanHostname(hostname: string) {
  return (
    hostname.startsWith("192.168.") ||
    hostname.startsWith("10.") ||
    /^172\.(1[6-9]|2\d|3[0-1])\./.test(hostname)
  );
}

function resolveApiBaseUrl() {
  if (process.env.NEXT_PUBLIC_API_BASE_URL) {
    return process.env.NEXT_PUBLIC_API_BASE_URL;
  }

  if (typeof window !== "undefined" && isPrivateLanHostname(window.location.hostname)) {
    return `http://${window.location.hostname}:8080`;
  }

  return DEFAULT_API_BASE_URL;
}

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
  authenticated?: boolean;
};

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, authenticated = false, headers, ...requestOptions } = options;
  const requestHeaders = new Headers(headers);

  if (body !== undefined) {
    requestHeaders.set("Content-Type", "application/json");
  }

  if (authenticated) {
    const token = getStoredAccessToken();

    if (token) {
      requestHeaders.set("Authorization", `Bearer ${token}`);
    }
  }

  const response = await fetch(`${resolveApiBaseUrl()}${path}`, {
    ...requestOptions,
    headers: requestHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  const payload = (await response.json().catch(() => null)) as ApiResponse<T> | null;

  if (!response.ok || !payload?.success) {
    throw new ApiError(payload?.message ?? "Request failed", response.status, payload?.errorCode ?? null);
  }

  return payload.data as T;
}
