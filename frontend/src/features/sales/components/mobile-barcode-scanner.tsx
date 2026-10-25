"use client";

import { BrowserMultiFormatReader, type IScannerControls } from "@zxing/browser";
import { Camera, CameraOff, Loader2, ScanLine, X } from "lucide-react";
import { useTranslations } from "next-intl";
import { useEffect, useMemo, useRef, useState, useSyncExternalStore } from "react";
import { Button } from "@/components/ui/button";

type MobileBarcodeScannerProps = Readonly<{
  isOpen: boolean;
  disabled?: boolean;
  paused?: boolean;
  processing?: boolean;
  onDetected: (barcode: string) => void;
  onOpenChange: (open: boolean) => void;
}>;

export function MobileBarcodeScanner({ isOpen, disabled = false, paused = false, processing = false, onDetected, onOpenChange }: MobileBarcodeScannerProps) {
  const t = useTranslations();
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const controlsRef = useRef<IScannerControls | null>(null);
  const lastScanRef = useRef({ barcode: "", scannedAt: 0 });
  const blockedRef = useRef(false);
  const isMobile = useSyncExternalStore(subscribeToMobileChanges, isLikelyMobileDevice, () => false);
  const [isStarting, setIsStarting] = useState(false);
  const [isScanning, setIsScanning] = useState(false);
  const [lastDetected, setLastDetected] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const isBlocked = disabled || paused || processing;
  blockedRef.current = isBlocked;

  const statusLabel = useMemo(() => {
    if (isStarting) return t("sales.mobileScanner.starting");
    if (isScanning && isBlocked) return t("sales.mobileScanner.paused");
    if (isScanning) return t("sales.mobileScanner.active");
    return t("sales.mobileScanner.inactive");
  }, [isBlocked, isScanning, isStarting, t]);

  // Cleanup on unmount
  useEffect(() => () => {
    controlsRef.current?.stop();
  }, []);

  // Stop scanner when closed; start when opened
  useEffect(() => {
    if (!isOpen) {
      stopScanner();
      return;
    }

    if (isScanning || isStarting || disabled) return;

    const timeoutId = window.setTimeout(() => {
      void startScanner();
    }, 80);

    return () => window.clearTimeout(timeoutId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen]);

  async function startScanner() {
    if (disabled || isStarting || isScanning) return;
    setErrorMessage("");

    if (typeof window === "undefined" || !navigator.mediaDevices?.getUserMedia) {
      setErrorMessage(t("sales.mobileScanner.unsupported"));
      return;
    }

    if (!window.isSecureContext) {
      setErrorMessage(t("sales.mobileScanner.secureContext"));
      return;
    }

    if (!videoRef.current) return;

    setIsStarting(true);

    try {
      const reader = new BrowserMultiFormatReader(undefined, { delayBetweenScanAttempts: 450 });
      controlsRef.current = await reader.decodeFromConstraints(
        {
          audio: false,
          video: {
            facingMode: { ideal: "environment" },
            width: { ideal: 1280 },
            height: { ideal: 720 },
          },
        },
        videoRef.current,
        (result) => {
          const text = result?.getText().trim();

          if (!text || blockedRef.current) return;

          const now = Date.now();
          const recentDuplicate = lastScanRef.current.barcode === text && now - lastScanRef.current.scannedAt < 1600;

          if (recentDuplicate) return;

          lastScanRef.current = { barcode: text, scannedAt: now };
          setLastDetected(text);
          navigator.vibrate?.(35);
          onDetected(text);
        },
      );
      setIsScanning(true);
    } catch {
      setErrorMessage(t("sales.mobileScanner.permissionError"));
      stopScanner();
    } finally {
      setIsStarting(false);
    }
  }

  function stopScanner() {
    controlsRef.current?.stop();
    controlsRef.current = null;
    setIsScanning(false);
    setIsStarting(false);
  }

  function openScanner() {
    if (disabled || isBlocked) return;
    onOpenChange(true);
  }

  function closeScanner() {
    onOpenChange(false);
  }

  if (!isMobile) return null;

  // Closed: just the trigger button
  if (!isOpen) {
    return (
      <Button
        type="button"
        variant="outline"
        className="h-11 w-full border-dashed"
        disabled={disabled || isBlocked}
        onClick={openScanner}
      >
        <ScanLine className="mr-2 h-4 w-4" aria-hidden="true" />
        {t("sales.scannerMode")}
      </Button>
    );
  }

  // Open: inline camera dock
  return (
    <div className="flex min-w-0 flex-col gap-2">
      {/* Status bar */}
      <div className="flex items-center justify-between gap-2">
        <span className="truncate text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">{t("sales.mobileScanner.title")}</span>
        <div className="flex shrink-0 items-center gap-2">
          <span className="rounded-full border bg-background/80 px-2 py-0.5 text-[0.65rem] font-bold uppercase tracking-[0.12em] text-muted-foreground">
            {statusLabel}
          </span>
          <button type="button" className="rounded-full p-1 hover:bg-muted" aria-label={t("sales.scannerClose")} onClick={closeScanner}>
            <X className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Camera container — always mounted when open so videoRef is available */}
      <div className="overflow-hidden rounded-2xl border bg-black">
        <div className="relative aspect-[16/7]">
          <video ref={videoRef} className="h-full w-full object-cover" muted playsInline />

          {/* Intro overlay */}
          {!isScanning && !isStarting && !errorMessage ? (
            <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/60 px-4 text-center">
              <p className="text-xs text-white/80">{t("sales.scannerIntro")}</p>
              <Button className="mt-3 h-9" type="button" onClick={startScanner}>
                <Camera className="mr-2 h-4 w-4" aria-hidden="true" />
                {t("sales.mobileScanner.start")}
              </Button>
            </div>
          ) : null}

          {/* Loading overlay */}
          {isStarting ? (
            <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/70">
              <Loader2 className="h-6 w-6 animate-spin text-white" aria-hidden="true" />
              <span className="mt-2 text-sm text-white/80">{t("sales.mobileScanner.starting")}</span>
            </div>
          ) : null}

          {/* Scanner frame */}
          {isScanning ? (
            <>
              <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(to_bottom,rgba(0,0,0,0.22),transparent_30%,transparent_70%,rgba(0,0,0,0.22))]" />
              <div className="pointer-events-none absolute left-1/2 top-1/2 h-16 w-[60%] -translate-x-1/2 -translate-y-1/2 rounded-2xl border-2 border-cyan-300/90 shadow-[0_0_0_999px_rgba(2,8,23,0.22)]" />
              <div className="pointer-events-none absolute left-[18%] right-[18%] top-1/2 h-px bg-cyan-200 shadow-[0_0_18px_rgba(103,232,249,0.9)]" />
            </>
          ) : null}
        </div>

        {/* Detection bar */}
        <div className="flex items-center gap-2 bg-black/90 px-3 py-1.5 text-[0.7rem] text-white">
          <ScanLine className="h-3 w-3 shrink-0 text-cyan-200" aria-hidden="true" />
          <span className="truncate">{lastDetected ? t("sales.mobileScanner.lastDetected", { barcode: lastDetected }) : t("sales.mobileScanner.aim")}</span>
          {isScanning ? (
            <Button className="ml-auto h-6 px-2 text-[0.65rem]" type="button" variant="outline" onClick={stopScanner}>
              <CameraOff className="mr-1 h-3 w-3" aria-hidden="true" />
              {t("sales.mobileScanner.stop")}
            </Button>
          ) : null}
        </div>
      </div>

      {/* Error */}
      {errorMessage ? <div className="rounded-2xl bg-destructive/10 px-3 py-2 text-xs text-destructive">{errorMessage}</div> : null}
    </div>
  );
}

function isLikelyMobileDevice() {
  if (typeof window === "undefined") return false;

  const userAgent = navigator.userAgent.toLowerCase();
  const mobileUserAgent = /android|iphone|ipad|ipod|mobile/.test(userAgent);
  const coarsePointer = window.matchMedia?.("(pointer: coarse)").matches ?? false;

  return mobileUserAgent || (coarsePointer && navigator.maxTouchPoints > 0);
}

function subscribeToMobileChanges(onStoreChange: () => void) {
  if (typeof window === "undefined" || !window.matchMedia) return () => {};

  const mediaQuery = window.matchMedia("(pointer: coarse)");
  mediaQuery.addEventListener("change", onStoreChange);

  return () => mediaQuery.removeEventListener("change", onStoreChange);
}
