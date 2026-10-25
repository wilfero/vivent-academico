import type { CurrentSession, LoginRequest, LoginResponse } from "@/lib/api/contracts";
import { apiRequest } from "@/lib/api/http";
import { clearSession, storeSession } from "@/features/auth/lib/session-storage";

export async function login(request: LoginRequest) {
  const session = await apiRequest<LoginResponse>("/api/v1/auth/login", {
    method: "POST",
    body: request,
  });

  storeSession(session);
  return session;
}

export function logout() {
  clearSession();
}

export function getCurrentSession() {
  return apiRequest<CurrentSession>("/api/v1/auth/me", { authenticated: true });
}
