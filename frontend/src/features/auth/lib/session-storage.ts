import type { LoginResponse } from "@/lib/api/contracts";

const SESSION_KEY = "vivent.session";
export const SESSION_CHANGE_EVENT = "vivent.session.change";

export type StoredSession = LoginResponse;

function isBrowser() {
  return typeof window !== "undefined";
}

export function storeSession(session: StoredSession) {
  if (!isBrowser()) return;
  window.sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  window.dispatchEvent(new Event(SESSION_CHANGE_EVENT));
}

export function clearSession() {
  if (!isBrowser()) return;
  window.sessionStorage.removeItem(SESSION_KEY);
  window.dispatchEvent(new Event(SESSION_CHANGE_EVENT));
}

export function getStoredSession(): StoredSession | null {
  if (!isBrowser()) return null;

  const rawSession = getStoredSessionSnapshot();
  if (!rawSession) return null;

  try {
    const session = JSON.parse(rawSession) as StoredSession;

    if (isSessionExpired(session)) {
      clearSession();
      return null;
    }

    return session;
  } catch {
    clearSession();
    return null;
  }
}

export function getStoredSessionSnapshot() {
  if (!isBrowser()) return null;
  return window.sessionStorage.getItem(SESSION_KEY);
}

export function getStoredAccessToken() {
  return getStoredSession()?.accessToken ?? null;
}

export function isSessionExpired(session: Pick<StoredSession, "expiresAt">) {
  return new Date(session.expiresAt).getTime() <= Date.now();
}

export function getMillisecondsUntilExpiration(session: Pick<StoredSession, "expiresAt">) {
  return Math.max(new Date(session.expiresAt).getTime() - Date.now(), 0);
}
