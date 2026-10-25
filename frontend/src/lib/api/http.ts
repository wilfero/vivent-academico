import { ApiError } from "@/lib/api/errors";
import type { ApiResponse, LoginResponse } from "@/lib/api/contracts";
import {
  clearSession,
  getStoredAccessToken,
  getStoredRefreshToken,
  storeSession,
} from "@/features/auth/lib/session-storage";

const DEFAULT_API_BASE_URL = "";

function isPrivateLanHostname(hostname: string) {
  return (
    hostname.startsWith("192.168.") ||
    hostname.startsWith("10.") ||
    /^172\.(1[6-9]|2\d|3[0-1])\./.test(hostname)
  );
}

function isLocalHostname(hostname: string) {
  return hostname === "localhost" || hostname === "127.0.0.1" || hostname === "::1";
}

function resolveApiBaseUrl() {
  if (process.env.NEXT_PUBLIC_API_BASE_URL) {
    return process.env.NEXT_PUBLIC_API_BASE_URL;
  }

  // Local Docker / LAN usage: the backend is published on the same machine at
  // port 8080, so derive it from the browser host when no explicit API URL is
  // configured (e.g. http://localhost:3000 -> http://localhost:8080 and
  // http://192.168.1.20:3000 -> http://192.168.1.20:8080).
  if (typeof window !== "undefined") {
    const hostname = window.location.hostname;

    if (isLocalHostname(hostname) || isPrivateLanHostname(hostname)) {
      return `http://${hostname}:8080`;
    }
  }

  return DEFAULT_API_BASE_URL;
}

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
  authenticated?: boolean;
};

/**
 * Refresh-token flow: a single in-flight refresh is shared by concurrent
 * requests so parallel 401s trigger exactly one renewal. The presented
 * refresh token rotates server-side on every use.
 */
let refreshInFlight: Promise<boolean> | null = null;

async function performRefresh(): Promise<boolean> {
  const refreshToken = getStoredRefreshToken();

  if (!refreshToken) {
    return false;
  }

  try {
    const response = await fetch(`${resolveApiBaseUrl()}/api/v1/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });

    const payload = (await response.json().catch(() => null)) as ApiResponse<LoginResponse> | null;

    if (response.ok && payload?.success && payload.data) {
      storeSession(payload.data);
      return true;
    }

    return false;
  } catch {
    return false;
  }
}

function refreshAccessToken(): Promise<boolean> {
  if (!refreshInFlight) {
    refreshInFlight = performRefresh().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

function buildHeaders(options: RequestOptions): Headers {
  const { body, authenticated = false, headers } = options;
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

  return requestHeaders;
}

function endExpiredSession() {
  clearSession();

  if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
    window.location.replace("/login?expired=1");
  }
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, ...requestOptions } = options;
  const isAuthCall = path.startsWith("/api/v1/auth/refresh") || path.startsWith("/api/v1/auth/logout");

  const executeRequest = async (retryAfterRefresh: boolean): Promise<Response> => {
    const requestHeaders = buildHeaders({ ...options, body });
    return fetch(`${resolveApiBaseUrl()}${path}`, {
      ...requestOptions,
      headers: requestHeaders,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  };

  let response = await executeRequest(false);
  let payload = (await response.json().catch(() => null)) as ApiResponse<T> | null;

  // Access token expired or revoked: renew once and replay the request.
  const tokenInvalid =
    options.authenticated &&
    response.status === 401 &&
    (payload?.errorCode === "INVALID_TOKEN" || payload?.errorCode === "AUTHENTICATION_REQUIRED");

  if (tokenInvalid && !isAuthCall) {
    const renewed = await refreshAccessToken();

    if (renewed) {
      response = await executeRequest(true);
      payload = (await response.json().catch(() => null)) as ApiResponse<T> | null;
    } else {
      endExpiredSession();
      throw new ApiError("Session expired", 401, "INVALID_TOKEN");
    }
  }

  if (!response.ok || !payload?.success) {
    throw new ApiError(payload?.message ?? "Request failed", response.status, payload?.errorCode ?? null);
  }

  return payload.data as T;
}

/** Explicit renewal used by the proactive session timer in guards.ts. */
export async function refreshSessionNow(): Promise<boolean> {
  return refreshAccessToken();
}
