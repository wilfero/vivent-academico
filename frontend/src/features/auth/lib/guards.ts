"use client";

import { useRouter } from "next/navigation";
import { useEffect, useMemo, useSyncExternalStore } from "react";
import { getMillisecondsUntilExpiration, getStoredSessionSnapshot, SESSION_CHANGE_EVENT, type StoredSession } from "@/features/auth/lib/session-storage";
import { logout } from "@/features/auth/lib/auth-client";

function subscribeToSessionChanges(onStoreChange: () => void) {
  window.addEventListener("storage", onStoreChange);
  window.addEventListener(SESSION_CHANGE_EVENT, onStoreChange);

  return () => {
    window.removeEventListener("storage", onStoreChange);
    window.removeEventListener(SESSION_CHANGE_EVENT, onStoreChange);
  };
}

function subscribeToHydration(onStoreChange: () => void) {
  queueMicrotask(onStoreChange);
  return () => undefined;
}

function getClientHydrationSnapshot() {
  return true;
}

function getServerHydrationSnapshot() {
  return false;
}

function getServerSessionSnapshot(): string | null {
  return null;
}

function parseSession(rawSession: string | null): StoredSession | null {
  if (!rawSession) return null;

  try {
    return JSON.parse(rawSession) as StoredSession;
  } catch {
    return null;
  }
}

export function useRequireSession() {
  const router = useRouter();
  const isHydrated = useSyncExternalStore(subscribeToHydration, getClientHydrationSnapshot, getServerHydrationSnapshot);
  const rawSession = useSyncExternalStore(subscribeToSessionChanges, getStoredSessionSnapshot, getServerSessionSnapshot);
  const session = useMemo(() => parseSession(rawSession), [rawSession]);

  useEffect(() => {
    if (!isHydrated) return;

    if (!session) {
      router.replace("/login");
      return;
    }

    const timeout = window.setTimeout(() => {
      logout();
      router.replace("/login?expired=1");
    }, getMillisecondsUntilExpiration(session));

    return () => window.clearTimeout(timeout);
  }, [isHydrated, router, session]);

  return { session, isChecking: !isHydrated || !session };
}
