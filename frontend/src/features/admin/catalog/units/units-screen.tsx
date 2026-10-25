"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { useEffect, useMemo, useState } from "react";
import { useForm, type UseFormRegisterReturn } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { FormDialog } from "@/components/ui/form-dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { AdminPage } from "@/features/admin/shared/admin-page";
import { AdminTable } from "@/features/admin/shared/admin-table";
import { createUnit, deactivateUnit, listUnits, updateUnit, type UnitPayload } from "@/features/admin/catalog/shared/catalog-api";
import type { ProductUnit } from "@/features/admin/catalog/shared/catalog-types";
import { can } from "@/features/admin/catalog/shared/catalog-permissions";

const unitSchema = z.object({ code: z.string().min(1), name: z.string().min(1), translationKey: z.string().min(1), allowsDecimal: z.boolean(), active: z.boolean() });
type UnitForm = z.infer<typeof unitSchema>;
const defaultUnitValues: UnitForm = { code: "", name: "", translationKey: "unit.custom", allowsDecimal: false, active: true };

export function UnitsScreen() {
	const t = useTranslations();
	const queryClient = useQueryClient();
	const [editing, setEditing] = useState<ProductUnit | null>(null);
	const [isFormOpen, setIsFormOpen] = useState(false);
	const unitsQuery = useQuery({ queryKey: ["catalog", "units"], queryFn: listUnits });
	const form = useForm<UnitForm>({ resolver: zodResolver(unitSchema), defaultValues: defaultUnitValues });

	useEffect(() => {
		if (!editing) return;
		form.reset(editing);
	}, [editing, form]);

	const saveMutation = useMutation({
		mutationFn: (values: UnitForm) => {
			const payload: UnitPayload = values;
			return editing ? updateUnit(editing.code, payload) : createUnit(payload);
		},
		onSuccess: () => {
			toast.success(t("admin.states.saved"));
			setIsFormOpen(false);
			setEditing(null);
			form.reset(defaultUnitValues);
			queryClient.invalidateQueries({ queryKey: ["catalog", "units"] });
		},
	});
	const deactivateMutation = useMutation({ mutationFn: deactivateUnit, onSuccess: () => queryClient.invalidateQueries({ queryKey: ["catalog", "units"] }) });
	const columns = useMemo<ColumnDef<ProductUnit>[]>(() => [
		{ header: t("admin.fields.code"), accessorKey: "code" },
		{ header: t("admin.fields.name"), accessorKey: "name" },
		{ header: t("catalog.fields.allowsDecimal"), cell: ({ row }) => row.original.allowsDecimal ? t("admin.status.yes") : t("admin.status.no") },
		{ header: t("admin.fields.status"), cell: ({ row }) => row.original.active ? t("admin.status.active") : t("admin.status.inactive") },
		{ header: t("admin.fields.actions"), cell: ({ row }) => <div className="flex gap-2">{can("units.update") ? <Button variant="outline" onClick={() => { setEditing(row.original); setIsFormOpen(true); }}>{t("admin.actions.edit")}</Button> : null}{can("units.deactivate") && row.original.active ? <ConfirmDialog trigger={<Button variant="outline">{t("admin.actions.deactivate")}</Button>} title={t("catalog.deactivateTitle")} description={t("catalog.confirmDeactivate")} confirmLabel={t("admin.actions.deactivate")} cancelLabel={t("admin.actions.cancel")} onConfirm={() => deactivateMutation.mutate(row.original.code)} /> : null}</div> },
	], [deactivateMutation, t]);

	return <AdminPage title={t("catalog.units.title")} description={t("catalog.units.description")} isLoading={unitsQuery.isLoading} error={unitsQuery.error ?? saveMutation.error ?? deactivateMutation.error}>
		{can("units.create") ? <div><Button type="button" onClick={() => { setEditing(null); form.reset(defaultUnitValues); setIsFormOpen(true); }}>{t("admin.actions.create")}</Button></div> : null}
		<FormDialog open={isFormOpen} onOpenChange={(open) => { setIsFormOpen(open); if (!open) { setEditing(null); form.reset(defaultUnitValues); } }} title={editing ? t("admin.actions.update") : t("admin.actions.create")} description={t("catalog.units.description")}>
			<form className="grid gap-4 md:grid-cols-3" onSubmit={form.handleSubmit((values) => saveMutation.mutate(values))}>
			<Field label={t("admin.fields.code")} registration={form.register("code")} disabled={Boolean(editing)} />
			<Field label={t("admin.fields.name")} registration={form.register("name")} />
			<Field label={t("admin.fields.translationKey")} registration={form.register("translationKey")} />
			<label className="flex items-center gap-3 pt-6 text-sm"><input type="checkbox" {...form.register("allowsDecimal")} />{t("catalog.fields.allowsDecimal")}</label>
			<label className="flex items-center gap-3 pt-6 text-sm"><input type="checkbox" {...form.register("active")} />{t("admin.fields.active")}</label>
			<div className="flex gap-2 md:col-span-3"><Button disabled={saveMutation.isPending} type="submit">{editing ? t("admin.actions.update") : t("admin.actions.create")}</Button><Button type="button" variant="outline" onClick={() => setIsFormOpen(false)}>{t("admin.actions.cancel")}</Button></div>
			</form>
		</FormDialog>
		<AdminTable columns={columns} data={unitsQuery.data ?? []} />
	</AdminPage>;
}

function Field({ label, registration, disabled = false }: Readonly<{ label: string; registration: UseFormRegisterReturn; disabled?: boolean }>) {
	return <div className="space-y-2"><Label>{label}</Label><Input disabled={disabled} {...registration} /></div>;
}
