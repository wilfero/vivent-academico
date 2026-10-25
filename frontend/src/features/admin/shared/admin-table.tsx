"use client";

import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { Card, CardContent } from "@/components/ui/card";

export function AdminTable<T>({ columns, data }: Readonly<{ columns: ColumnDef<T>[]; data: T[] }>) {
  const t = useTranslations();
  // TanStack Table intentionally returns stable table APIs that React Compiler cannot memoize.
  // eslint-disable-next-line react-hooks/incompatible-library
  const table = useReactTable({ data, columns, getCoreRowModel: getCoreRowModel() });

  if (data.length === 0) {
    return (
      <Card>
        <CardContent className="p-6 text-sm text-muted-foreground">{t("admin.states.empty")}</CardContent>
      </Card>
    );
  }

  return (
    <div className="overflow-hidden rounded-2xl border bg-card/80">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-muted/60 text-left text-xs uppercase tracking-[0.14em] text-muted-foreground">
            {table.getHeaderGroups().map((headerGroup) => (
              <tr key={headerGroup.id}>
                {headerGroup.headers.map((header) => (
                  <th key={header.id} className="px-4 py-3 font-black">
                    {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
                  </th>
                ))}
              </tr>
            ))}
          </thead>
          <tbody>
            {table.getRowModel().rows.map((row) => (
              <tr key={row.id} className="border-t">
                {row.getVisibleCells().map((cell) => (
                  <td key={cell.id} className="px-4 py-3 align-top">
                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
