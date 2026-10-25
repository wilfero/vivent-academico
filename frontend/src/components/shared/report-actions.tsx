"use client";

import { Download, Printer } from "lucide-react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";

function collectVisibleTablesAsCsv(): string {
  const tables = Array.from(document.querySelectorAll("table"));
  const blocks: string[] = [];

  for (const table of tables) {
    const rows = Array.from(table.querySelectorAll("tr"));

    if (rows.length === 0) {
      continue;
    }

    const lines = rows.map((row) =>
      Array.from(row.querySelectorAll("th,td"))
        .map((cell) => `"${(cell.textContent ?? "").replace(/\s+/g, " ").trim().replace(/"/g, '""')}"`)
        .join(";"),
    );

    blocks.push(lines.join("\r\n"));
  }

  return blocks.join("\r\n\r\n");
}

/**
 * Print / CSV export for the nine reports. The CSV is generated from the table
 * currently rendered on screen, so it always matches the active filters.
 */
export function ReportActions({ fileBaseName }: Readonly<{ fileBaseName: string }>) {
  const t = useTranslations();

  function handleExport() {
    const csv = collectVisibleTablesAsCsv();

    if (!csv) {
      toast.error(t("reports.actions.nothingToExport"));
      return;
    }

    // UTF-8 BOM so Excel opens the accents correctly.
    const blob = new Blob(["\uFEFF" + csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    const stamp = new Date().toISOString().slice(0, 19).replace(/[:T]/g, "-");

    link.href = url;
    link.download = `${fileBaseName}-${stamp}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    toast.success(t("reports.actions.exported"));
  }

  return (
    <div className="flex flex-wrap gap-2 print:hidden" data-print-hide>
      <Button type="button" variant="outline" onClick={handleExport}>
        <Download className="mr-2 h-4 w-4" />
        {t("reports.actions.exportCsv")}
      </Button>
      <Button type="button" variant="outline" onClick={() => window.print()}>
        <Printer className="mr-2 h-4 w-4" />
        {t("reports.actions.print")}
      </Button>
    </div>
  );
}
