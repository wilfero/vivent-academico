"use client";

import Link from "next/link";
import {
  Archive,
  BadgeDollarSign,
  Barcode,
  Boxes,
  Building2,
  ChartNoAxesColumnIncreasing,
  ChevronDown,
  ChevronsLeft,
  ChevronsRight,
  ClipboardCheck,
  ClipboardList,
  Gauge,
  GitBranch,
  KeyRound,
  Layers3,
  LogOut,
  Menu,
  Package,
  Ruler,
  ShieldCheck,
  ShoppingBasket,
  Tags,
  Truck,
  UserRound,
  Users,
  type LucideIcon,
} from "lucide-react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { toast } from "sonner";
import { AccessDenied } from "@/components/layout/access-denied";
import { Breadcrumbs } from "@/components/layout/breadcrumbs";
import { Button } from "@/components/ui/button";
import { navigationSections } from "@/components/layout/app-navigation";
import { PreferenceControls } from "@/components/layout/preference-controls";
import { useAppLocale } from "@/components/providers/locale-provider";
import { getCurrentSession, logout } from "@/features/auth/lib/auth-client";
import { useRequireSession } from "@/features/auth/lib/guards";
import { hasAnyPermission } from "@/features/auth/lib/permissions";
import { getTenantConfig } from "@/features/tenant/lib/get-tenant-config";
import { ApiError } from "@/lib/api/errors";

const SIDEBAR_COLLAPSED_KEY = "vivent.sidebar.collapsed";

const navigationIcons: Record<string, LucideIcon> = {
  "/account": UserRound,
  "/dashboard": Gauge,
  "/sales": BadgeDollarSign,
  "/purchases": ShoppingBasket,
  "/transfers": Truck,
  "/inventory": Boxes,
  "/adjustments": ClipboardCheck,
  "/closures": ClipboardList,
  "/admin/catalog/products": Package,
  "/admin/catalog/categories": Tags,
  "/admin/catalog/units": Ruler,
  "/admin/catalog/presentations": Barcode,
  "/admin/catalog/combos": Layers3,
  "/admin/company/profile": Building2,
  "/admin/company/branches": GitBranch,
  "/admin/security/users": Users,
  "/admin/security/roles": ShieldCheck,
  "/admin/security/permissions": KeyRound,
  "/admin/security/audit-logs": ClipboardList,
  "/admin/third-parties/suppliers": Truck,
  "/admin/third-parties/customers": UserRound,
  "/admin/subscription": Layers3,
  "/reports": ChartNoAxesColumnIncreasing,
  "/reports/inventory": Archive,
  "/reports/kardex": ChartNoAxesColumnIncreasing,
  "/reports/movements": ClipboardList,
  "/reports/products": Package,
  "/reports/sales": BadgeDollarSign,
  "/reports/purchases": ShoppingBasket,
  "/reports/transfers": Truck,
  "/reports/adjustments": ClipboardCheck,
  "/reports/closures": ClipboardList,
};

export function AppShell({ children }: Readonly<{ children: React.ReactNode }>) {
  const t = useTranslations();
  const router = useRouter();
  const queryClient = useQueryClient();
  const pathname = usePathname();
  const sidebarNavRef = useRef<HTMLElement>(null);
  const { setTimeZone } = useAppLocale();
  const { session, isChecking } = useRequireSession();
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(() => {
    if (typeof window === "undefined") return true;
    return window.sessionStorage.getItem(SIDEBAR_COLLAPSED_KEY) !== "false";
  });
  const [openSections, setOpenSections] = useState(() =>
    Object.fromEntries(navigationSections.map((section) => [section.labelKey, section.items.some((item) => item.href === pathname)])),
  );

  const sessionQuery = useQuery({
    queryKey: ["auth", "me"],
    queryFn: getCurrentSession,
    enabled: Boolean(session),
    retry: false,
  });

  const activeSession = sessionQuery.data ?? session;
  const tenantQuery = useQuery({
    queryKey: ["tenant-config", activeSession?.tenantSlug],
    queryFn: () => getTenantConfig(activeSession?.tenantSlug ?? "demo"),
    enabled: Boolean(activeSession?.tenantSlug),
    retry: false,
  });

  const currentRouteItem = navigationSections.flatMap((section) => section.items).find((item) => item.href === pathname);
  const canAccessCurrentRoute = hasAnyPermission(activeSession, currentRouteItem?.permissions);
  const visibleSections = navigationSections
    .map((section) => ({
      ...section,
      items: section.items.filter((item) => hasAnyPermission(activeSession, item.permissions)),
    }))
    .filter((section) => section.items.length > 0);

  const companyName = tenantQuery.data?.commercialName ?? activeSession?.tenantSlug ?? "Vivent";

  useEffect(() => {
    if (sessionQuery.error instanceof ApiError && sessionQuery.error.status === 401) {
      logout();
      router.replace("/login");
    }
  }, [router, sessionQuery.error]);

  useEffect(() => {
    if (activeSession?.mustChangePassword) {
      router.replace("/change-password");
    }
  }, [activeSession?.mustChangePassword, router]);

  useEffect(() => {
    const toastId = "private-context-loading";

    if (sessionQuery.isLoading || tenantQuery.isLoading) {
      toast.loading(t("private.loadingContext"), { id: toastId });
      return;
    }

    toast.dismiss(toastId);
  }, [sessionQuery.isLoading, tenantQuery.isLoading, t]);

  useEffect(() => {
    if (!sessionQuery.isError && !tenantQuery.isError) return;
    toast.error(t("private.contextError"));
  }, [sessionQuery.isError, tenantQuery.isError, t]);

  useEffect(() => {
    if (!tenantQuery.data?.timezone) return;
    setTimeZone(tenantQuery.data.timezone);
  }, [setTimeZone, tenantQuery.data?.timezone]);

  useEffect(() => {
    const savedScroll = window.sessionStorage.getItem("vivent.sidebar.scrollTop");
    if (savedScroll && sidebarNavRef.current) {
      sidebarNavRef.current.scrollTop = Number(savedScroll);
    }
  }, []);

  function rememberSidebarScroll() {
    if (!sidebarNavRef.current) return;
    window.sessionStorage.setItem("vivent.sidebar.scrollTop", String(sidebarNavRef.current.scrollTop));
  }

  function toggleSidebarCollapsed() {
    setIsSidebarCollapsed((current) => {
      const nextValue = !current;
      window.sessionStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(nextValue));
      return nextValue;
    });
  }

  if (isChecking) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-background text-sm text-muted-foreground">
        <div className="rounded-full border bg-card/80 px-4 py-2 shadow-sm">Validando sesión...</div>
      </main>
    );
  }

  if (activeSession?.mustChangePassword) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-background text-sm text-muted-foreground">
        <div className="rounded-full border bg-card/80 px-4 py-2 shadow-sm">Redirigiendo al cambio de contraseña...</div>
      </main>
    );
  }

  return (
    <div className={`grid h-screen overflow-hidden transition-[grid-template-columns] duration-300 ${isSidebarCollapsed ? "lg:grid-cols-[88px_1fr]" : "lg:grid-cols-[310px_1fr]"}`}>
      {isSidebarOpen ? <button className="fixed inset-0 z-30 bg-foreground/40 lg:hidden" aria-label="Cerrar menú" type="button" onClick={() => setIsSidebarOpen(false)} /> : null}
      <aside
        className={`sidebar-panel fixed inset-y-0 left-0 z-40 flex w-[310px] min-h-0 flex-col border-r bg-panel text-panel-foreground shadow-2xl shadow-foreground/10 transition-[width,transform] duration-300 lg:relative lg:translate-x-0 ${isSidebarCollapsed ? "lg:w-[88px]" : "lg:w-[310px]"} ${isSidebarOpen ? "translate-x-0" : "-translate-x-full"}`}
      >
        {/* Brand */}
        <div className={`shrink-0 px-5 pt-6 ${isSidebarCollapsed ? "lg:px-4" : ""}`}>
          <div className={`mb-8 flex items-center gap-3 ${isSidebarCollapsed ? "lg:flex-col lg:justify-center lg:gap-2" : ""}`}>
          {tenantQuery.data?.logoUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img className="h-12 w-12 rounded-2xl border border-panel-foreground/15 object-cover" src={tenantQuery.data.logoUrl} alt={`${companyName} logo`} />
          ) : (
            <div className="scanline flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-primary text-lg font-black text-primary-foreground">V</div>
          )}
          <div className={isSidebarCollapsed ? "lg:sr-only" : "min-w-0"}>
            <p className="truncate text-lg font-black tracking-[-0.04em]">{companyName}</p>
            <p className="truncate font-mono text-xs text-panel-foreground/55">{activeSession?.tenantSlug}.corpoviva.com</p>
          </div>
          <Button
            className={`ml-auto hidden h-9 w-9 px-0 lg:inline-flex ${isSidebarCollapsed ? "lg:ml-0 lg:h-8 lg:w-8 lg:rounded-full lg:border-panel-foreground/15 lg:bg-panel/80" : ""}`}
            type="button"
            variant="outline"
            aria-label={isSidebarCollapsed ? "Expandir menú" : "Colapsar menú"}
            onClick={toggleSidebarCollapsed}
          >
            {isSidebarCollapsed ? <ChevronsRight className="h-4 w-4" aria-hidden="true" /> : <ChevronsLeft className="h-4 w-4" aria-hidden="true" />}
          </Button>
        </div>
        </div>

        {/* Navigation */}
        <nav ref={sidebarNavRef} className={`scrollbar-invisible min-h-0 flex-1 space-y-6 overflow-y-auto px-5 pb-6 pr-3 ${isSidebarCollapsed ? "lg:px-4 lg:pr-4" : ""}`}>
          {visibleSections.map((section) => {
            const groups = section.items.reduce<Record<string, typeof section.items>>((accumulator, item) => {
              const groupKey = item.groupKey ?? "__root";
              accumulator[groupKey] = [...(accumulator[groupKey] ?? []), item];
              return accumulator;
            }, {});

            return (
            <section key={section.labelKey} className="space-y-2">
              <button
                className={`flex w-full items-center justify-between rounded-xl px-3 py-2 text-left text-xs font-black uppercase tracking-[0.18em] text-panel-foreground/55 transition-colors hover:bg-panel-foreground/10 hover:text-panel-foreground ${isSidebarCollapsed ? "lg:hidden" : ""}`}
                type="button"
                aria-expanded={openSections[section.labelKey]}
                onClick={() => setOpenSections((current) => ({ ...current, [section.labelKey]: !current[section.labelKey] }))}
              >
                <span>{t(section.labelKey)}</span>
                <ChevronDown className={`h-4 w-4 transition-transform ${openSections[section.labelKey] ? "rotate-180" : ""}`} aria-hidden="true" />
              </button>
              {openSections[section.labelKey] || isSidebarCollapsed ? (
                <div className={`grid gap-3 pl-2 ${isSidebarCollapsed ? "lg:pl-0" : ""}`}>
                  {Object.entries(groups).map(([groupKey, items]) => (
                    <div key={groupKey} className="grid gap-1">
                      {groupKey !== "__root" ? <p className={`px-3 pt-2 text-[0.68rem] font-black uppercase tracking-[0.16em] text-panel-foreground/35 ${isSidebarCollapsed ? "lg:hidden" : ""}`}>{t(groupKey)}</p> : null}
                      {items.map((item) => {
                        const Icon = navigationIcons[item.href] ?? Package;

                        return (
                          <Link
                            key={item.href}
                            className={`group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition-all hover:bg-panel-foreground/10 ${isSidebarCollapsed ? "lg:h-12 lg:w-12 lg:justify-center lg:px-0 lg:py-0" : ""} ${pathname === item.href ? "bg-primary text-primary-foreground shadow-lg shadow-primary/20" : "text-panel-foreground/72"}`}
                            href={item.href}
                            title={t(item.labelKey)}
                            aria-label={t(item.labelKey)}
                            onClick={() => {
                              rememberSidebarScroll();
                              setIsSidebarOpen(false);
                            }}
                          >
                            <Icon className="h-4 w-4 shrink-0 transition-transform group-hover:-translate-y-0.5" aria-hidden="true" />
                            <span className={isSidebarCollapsed ? "lg:sr-only" : ""}>{t(item.labelKey)}</span>
                          </Link>
                        );
                      })}
                    </div>
                  ))}
                </div>
              ) : null}
            </section>
          );
          })}
        </nav>

        {/* Sidebar footer — mobile only: session info + controls */}
        <div className="shrink-0 border-t border-panel-foreground/10 p-5 lg:hidden">
          <div className="space-y-1.5">
            <p className="truncate text-sm font-semibold">{companyName}</p>
            <p className="text-xs text-panel-foreground/60">{t("topbar.branch")}: {activeSession?.branchName ?? activeSession?.branchId?.slice(0, 8) ?? t("topbar.unknown")}</p>
            <Link className="text-xs text-panel-foreground/60 hover:text-panel-foreground" href="/account">
              {t("topbar.user")}: {activeSession?.username ?? t("topbar.unknown")}
            </Link>
            <p className="font-mono text-xs text-panel-foreground/45">{activeSession?.roleCode}</p>
          </div>
          <div className="mt-4 flex items-center justify-between gap-2">
            <PreferenceControls />
            <Button
              className="h-9 shrink-0 px-3"
              variant="outline"
              aria-label={t("auth.logout")}
              onClick={() => {
                queryClient.clear();
                logout();
                router.replace("/login");
              }}
            >
              <LogOut className="h-4 w-4" />
            </Button>
          </div>
        </div>
      </aside>

      {/* Main panel */}
      <div className="flex min-h-0 min-w-0 flex-col overflow-hidden">
        <header className="sticky top-0 z-10 flex min-h-14 items-center justify-between gap-3 border-b bg-background/90 px-4 py-2.5 backdrop-blur-xl sm:px-6">
          <div className="flex min-w-0 flex-1 items-center gap-3">
            <Button className="h-9 w-9 shrink-0 px-0 lg:hidden" type="button" variant="outline" aria-label="Abrir menú" onClick={() => setIsSidebarOpen(true)}>
              <Menu className="h-4 w-4" aria-hidden="true" />
            </Button>
            <div className="min-w-0">
              <Breadcrumbs />
              <p className="truncate text-sm font-semibold lg:hidden">{companyName}</p>
            </div>
          </div>
          <div className="hidden items-center gap-3 lg:flex">
            <span className="text-xs text-muted-foreground">{t("topbar.branch")}: {activeSession?.branchName ?? activeSession?.branchId?.slice(0, 8) ?? "—"}</span>
            <Link className="text-xs text-muted-foreground transition-colors hover:text-foreground" href="/account">
              {t("topbar.user")}: {activeSession?.username ?? "—"}
            </Link>
            <PreferenceControls />
            <Button
              className="h-9 shrink-0 px-3"
              variant="outline"
              aria-label={t("auth.logout")}
              onClick={() => {
                queryClient.clear();
                logout();
                router.replace("/login");
              }}
            >
              <LogOut className="h-4 w-4 sm:mr-2" aria-hidden="true" />
              <span className="hidden sm:inline">{t("auth.logout")}</span>
            </Button>
          </div>
        </header>
        <main className="min-h-0 flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
          {canAccessCurrentRoute ? children : <AccessDenied />}
        </main>
      </div>
    </div>
  );
}
