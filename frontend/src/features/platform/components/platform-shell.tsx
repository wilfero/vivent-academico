"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { Building2, LogOut, ScrollText } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { platformLogout, platformMe } from "@/features/platform/lib/platform-api";

const navItems = [
  { href: "/master/companies", label: "Empresas", icon: Building2 },
  { href: "/master/audit-logs", label: "Auditoría", icon: ScrollText },
];

export function PlatformShell({ children }: Readonly<{ children: React.ReactNode }>) {
  const router = useRouter();
  const pathname = usePathname();

  const sessionQuery = useQuery({
    queryKey: ["platform-session"],
    queryFn: platformMe,
    retry: false,
    staleTime: 60_000,
  });

  useEffect(() => {
    if (sessionQuery.isError) {
      router.replace("/master/login");
    }
  }, [sessionQuery.isError, router]);

  if (sessionQuery.isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <p className="text-sm text-muted-foreground">Cargando panel…</p>
      </div>
    );
  }

  if (sessionQuery.isError || !sessionQuery.data) {
    return null;
  }

  async function handleLogout() {
    try {
      await platformLogout();
    } finally {
      router.replace("/master/login");
      toast.success("Sesión cerrada");
    }
  }

  return (
    <div className="flex min-h-screen bg-background">
      <aside className="flex w-64 flex-col border-r border-border bg-card/60 p-4">
        <div className="mb-6 px-2">
          <p className="text-lg font-black tracking-tight">Vivent Master</p>
          <p className="text-xs text-muted-foreground">{sessionQuery.data.displayName}</p>
        </div>
        <nav className="flex flex-1 flex-col gap-1">
          {navItems.map((item) => {
            const Icon = item.icon;
            const active = pathname.startsWith(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-semibold transition-colors ${
                  active ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-accent hover:text-foreground"
                }`}
              >
                <Icon className="h-4 w-4" />
                {item.label}
              </Link>
            );
          })}
        </nav>
        <Button variant="outline" className="justify-start gap-2" onClick={handleLogout}>
          <LogOut className="h-4 w-4" />
          Cerrar sesión
        </Button>
      </aside>
      <main className="flex-1 overflow-y-auto p-8">{children}</main>
    </div>
  );
}
