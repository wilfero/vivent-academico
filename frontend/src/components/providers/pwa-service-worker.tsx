"use client";

import { useEffect } from "react";

function canRegisterServiceWorker() {
  return (
    process.env.NODE_ENV === "production" &&
    typeof window !== "undefined" &&
    "serviceWorker" in navigator &&
    (window.isSecureContext || window.location.hostname === "localhost")
  );
}

export function PwaServiceWorker() {
  useEffect(() => {
    if (!canRegisterServiceWorker()) {
      return;
    }

    let cancelled = false;

    const register = async () => {
      try {
        const registration = await navigator.serviceWorker.register("/sw.js", { scope: "/" });

        if (!cancelled && registration.waiting) {
          registration.waiting.postMessage({ type: "SKIP_WAITING" });
        }
      } catch (error) {
        console.warn("Vivent service worker registration failed", error);
      }
    };

    if (document.readyState === "complete") {
      void register();
      return () => {
        cancelled = true;
      };
    }

    window.addEventListener("load", register, { once: true });

    return () => {
      cancelled = true;
      window.removeEventListener("load", register);
    };
  }, []);

  return null;
}
