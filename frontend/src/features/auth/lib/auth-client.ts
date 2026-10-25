import type { CurrentSession, LoginRequest, LoginResponse } from "@/lib/api/contracts";
import { apiRequest, refreshSessionNow } from "@/lib/api/http";
import { clearSession, getStoredRefreshToken, storeSession } from "@/features/auth/lib/session-storage";

export async function login(request: LoginRequest) {
  const session = await apiRequest<LoginResponse>("/api/v1/auth/login", {
    method: "POST",
    body: request,
  });

  storeSession(session);
  return session;
}

/**
 * Closes the session: revokes the refresh token on the backend (best effort)
 * and clears the local session. Refresh tokens rotate server-side, so the
 * presented token is revoked explicitly even if rotation already happened.
 */
export async function logout() {
  const refreshToken = getStoredRefreshToken();

  if (refreshToken) {
    try {
      await apiRequest("/api/v1/auth/logout", {
        method: "POST",
        body: { refreshToken },
      });
    } catch {
      // Network/401 failures must not block the local logout.
    }
  }

  clearSession();
}

/** Renew the access token using the refresh token (single-flight). */
export async function refreshSession() {
  return refreshSessionNow();
}

export function getCurrentSession() {
  return apiRequest<CurrentSession>("/api/v1/auth/me", { authenticated: true });
}
