export type NavigationItem = {
  labelKey: string;
  href: string;
  groupKey?: string;
  permissions?: string[];
};

export type NavigationSection = {
  labelKey: string;
  items: NavigationItem[];
};

export const navigationSections: NavigationSection[] = [
  {
    labelKey: "navigation.operation",
    items: [
      { labelKey: "navigation.dashboard", href: "/dashboard" },
      { labelKey: "navigation.sales", href: "/sales", permissions: ["sales.view", "sales.create"] },
      { labelKey: "navigation.purchases", href: "/purchases", permissions: ["purchases.view"] },
      { labelKey: "navigation.transfers", href: "/transfers", permissions: ["transfers.view", "transfers.request"] },
      { labelKey: "navigation.inventory", href: "/inventory", permissions: ["inventory.view"] },
      { labelKey: "navigation.adjustments", href: "/adjustments", permissions: ["inventory.view"] },
      { labelKey: "navigation.cashClosings", href: "/closures", permissions: ["closures.view", "cash_closures.view", "cash_closures.create"] },
    ],
  },
  {
    labelKey: "navigation.administration",
    items: [
      { labelKey: "navigation.products", href: "/admin/catalog/products", groupKey: "navigation.catalog", permissions: ["products.view"] },
      { labelKey: "navigation.categories", href: "/admin/catalog/categories", groupKey: "navigation.catalog", permissions: ["product_categories.view"] },
      { labelKey: "navigation.units", href: "/admin/catalog/units", groupKey: "navigation.catalog", permissions: ["units.view"] },
      { labelKey: "navigation.presentations", href: "/admin/catalog/presentations", groupKey: "navigation.catalog", permissions: ["product_presentations.view"] },
      { labelKey: "navigation.combos", href: "/admin/catalog/combos", groupKey: "navigation.catalog", permissions: ["combos.view"] },
      { labelKey: "navigation.companyProfile", href: "/admin/company/profile", groupKey: "navigation.company", permissions: ["settings.view"] },
      { labelKey: "navigation.branches", href: "/admin/company/branches", groupKey: "navigation.company", permissions: ["settings.view"] },
      { labelKey: "navigation.users", href: "/admin/security/users", groupKey: "navigation.security", permissions: ["users.view"] },
      { labelKey: "navigation.roles", href: "/admin/security/roles", groupKey: "navigation.security", permissions: ["settings.view"] },
      { labelKey: "navigation.permissions", href: "/admin/security/permissions", groupKey: "navigation.security", permissions: ["settings.view"] },
      { labelKey: "navigation.auditLogs", href: "/admin/security/audit-logs", groupKey: "navigation.security", permissions: ["settings.view"] },
      { labelKey: "navigation.suppliers", href: "/admin/third-parties/suppliers", groupKey: "navigation.thirdParties", permissions: ["suppliers.view"] },
      { labelKey: "navigation.customers", href: "/admin/third-parties/customers", groupKey: "navigation.thirdParties", permissions: ["customers.view"] },
      { labelKey: "navigation.planAndLimits", href: "/admin/subscription", groupKey: "navigation.subscription", permissions: ["settings.view"] },
    ],
  },
  {
    labelKey: "navigation.reports",
    items: [
      { labelKey: "navigation.reportsHome", href: "/reports", permissions: ["reports.view", "reports.view_branch", "reports.view_company"] },
      { labelKey: "navigation.stock", href: "/reports/inventory", permissions: ["reports.inventory.view", "reports.view", "reports.view_branch", "reports.view_company"] },
      { labelKey: "navigation.kardex", href: "/reports/kardex", permissions: ["reports.kardex.view", "inventory.view_kardex"] },
      { labelKey: "navigation.movements", href: "/reports/movements", permissions: ["reports.movements.view", "inventory.view_movements"] },
      { labelKey: "navigation.products", href: "/reports/products", permissions: ["reports.products.view", "reports.view", "reports.view_branch", "reports.view_company"] },
      { labelKey: "navigation.sales", href: "/reports/sales", permissions: ["reports.sales.view", "reports.view", "reports.view_branch", "reports.view_company"] },
      { labelKey: "navigation.purchases", href: "/reports/purchases", permissions: ["reports.purchases.view", "reports.view", "reports.view_branch", "reports.view_company"] },
      { labelKey: "navigation.transfers", href: "/reports/transfers", permissions: ["reports.view_branch", "reports.view_company"] },
      { labelKey: "navigation.adjustments", href: "/reports/adjustments", permissions: ["reports.view_branch", "reports.view_company"] },
      { labelKey: "navigation.cashClosings", href: "/reports/closures", permissions: ["closures.view", "cash_closures.view"] },
    ],
  },
  {
    labelKey: "navigation.account",
    items: [{ labelKey: "navigation.myAccount", href: "/account" }],
  },
];

export function getNavigationItem(pathname: string) {
  return navigationSections.flatMap((section) => section.items).find((item) => item.href === pathname) ?? null;
}
