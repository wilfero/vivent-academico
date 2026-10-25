"use client";

import Link from "next/link";
import { ChevronRight } from "lucide-react";
import { useTranslations } from "next-intl";
import { usePathname } from "next/navigation";
import { getNavigationItem, navigationSections } from "@/components/layout/app-navigation";

export function Breadcrumbs() {
  const t = useTranslations();
  const pathname = usePathname();
  const item = getNavigationItem(pathname);
  const section = navigationSections.find((navigationSection) => navigationSection.items.some((navigationItem) => navigationItem.href === pathname));
  const crumbs = [section?.labelKey, item?.groupKey, item?.labelKey].filter(Boolean) as string[];

  return (
    <nav className="hidden items-center gap-1 text-xs text-muted-foreground md:flex" aria-label="Breadcrumb">
      <Link className="hover:text-foreground" href="/dashboard">
        {t("app.name")}
      </Link>
      {crumbs.map((crumb) => (
        <span key={crumb} className="flex items-center gap-1">
          <ChevronRight className="h-3.5 w-3.5" aria-hidden="true" />
          <span>{t(crumb)}</span>
        </span>
      ))}
    </nav>
  );
}
