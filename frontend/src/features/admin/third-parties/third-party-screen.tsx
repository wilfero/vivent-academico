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
import { can } from "@/features/admin/catalog/shared/catalog-permissions";
import { AdminPage } from "@/features/admin/shared/admin-page";
import { AdminTable } from "@/features/admin/shared/admin-table";
import {
  createCustomer,
  createSupplier,
  deactivateCustomer,
  deactivateSupplier,
  listCustomers,
  listSuppliers,
  updateCustomer,
  updateSupplier,
  type Customer,
  type CustomerPayload,
  type Supplier,
  type SupplierPayload,
} from "@/features/admin/third-parties/third-party-api";

const baseSchema = z.object({
  name: z.string().min(1),
  taxId: z.string().nullable(),
  email: z.string().email().or(z.literal("")).nullable(),
  phone: z.string().nullable(),
  address: z.string().nullable(),
  active: z.boolean(),
});

const supplierSchema = baseSchema.extend({ contactName: z.string().nullable() });
const customerSchema = baseSchema.extend({ genericConsumer: z.boolean() });

type SupplierForm = z.infer<typeof supplierSchema>;
type CustomerForm = z.infer<typeof customerSchema>;

const defaultSupplierValues: SupplierForm = { name: "", taxId: "", email: "", phone: "", address: "", contactName: "", active: true };
const defaultCustomerValues: CustomerForm = { name: "", taxId: "", email: "", phone: "", address: "", genericConsumer: false, active: true };

export function ThirdPartyScreen({ type }: Readonly<{ type: "suppliers" | "customers" }>) {
  return type === "suppliers" ? <SupplierScreen /> : <CustomerScreen />;
}

function SupplierScreen() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [query, setQuery] = useState("");
  const [editing, setEditing] = useState<Supplier | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const suppliersQuery = useQuery({ queryKey: ["thirdParties", "suppliers", query], queryFn: () => listSuppliers(query) });
  const form = useForm<SupplierForm>({ resolver: zodResolver(supplierSchema), defaultValues: defaultSupplierValues });

  useEffect(() => {
    if (!editing) return;
    form.reset({ name: editing.name, taxId: editing.taxId ?? "", email: editing.email ?? "", phone: editing.phone ?? "", address: editing.address ?? "", contactName: editing.contactName ?? "", active: editing.active });
  }, [editing, form]);

  const saveMutation = useMutation({
    mutationFn: (values: SupplierForm) => {
      const payload: SupplierPayload = normalizeSupplierPayload(values);
      return editing ? updateSupplier(editing.id, payload) : createSupplier(payload);
    },
    onSuccess: () => {
      toast.success(t("admin.states.saved"));
      setIsFormOpen(false);
      setEditing(null);
      form.reset(defaultSupplierValues);
      queryClient.invalidateQueries({ queryKey: ["thirdParties", "suppliers"] });
      queryClient.invalidateQueries({ queryKey: ["suppliers"] });
    },
  });
  const deactivateMutation = useMutation({ mutationFn: deactivateSupplier, onSuccess: () => queryClient.invalidateQueries({ queryKey: ["thirdParties", "suppliers"] }) });

  const columns = useMemo<ColumnDef<Supplier>[]>(() => [
    { header: t("admin.fields.name"), accessorKey: "name" },
    { header: t("admin.fields.taxId"), cell: ({ row }) => row.original.taxId ?? "-" },
    { header: t("admin.fields.contactName"), cell: ({ row }) => row.original.contactName ?? "-" },
    { header: t("admin.fields.phone"), cell: ({ row }) => row.original.phone ?? "-" },
    { header: t("admin.fields.status"), cell: ({ row }) => row.original.active ? t("admin.status.active") : t("admin.status.inactive") },
    { header: t("admin.fields.actions"), cell: ({ row }) => <Actions canUpdate="suppliers.update" canDelete="suppliers.delete" active={row.original.active} onEdit={() => { setEditing(row.original); setIsFormOpen(true); }} onDeactivate={() => deactivateMutation.mutate(row.original.id)} /> },
  ], [deactivateMutation, t]);

  return <AdminPage title={t("admin.thirdParties.suppliersTitle")} description={t("admin.thirdParties.description")} isLoading={suppliersQuery.isLoading} error={suppliersQuery.error ?? saveMutation.error ?? deactivateMutation.error}>
    <Toolbar query={query} setQuery={setQuery} canCreate="suppliers.create" onCreate={() => { setEditing(null); form.reset(defaultSupplierValues); setIsFormOpen(true); }} />
    <FormDialog open={isFormOpen} onOpenChange={(open) => { setIsFormOpen(open); if (!open) { setEditing(null); form.reset(defaultSupplierValues); } }} title={editing ? t("admin.actions.update") : t("admin.actions.create")} description={t("admin.thirdParties.suppliersTitle")}>
      <form className="grid gap-4 md:grid-cols-2" onSubmit={form.handleSubmit((values) => saveMutation.mutate(values))}>
        <Field label={t("admin.fields.name")} registration={form.register("name")} />
        <Field label={t("admin.fields.taxId")} registration={form.register("taxId")} />
        <Field label={t("admin.fields.contactName")} registration={form.register("contactName")} />
        <Field label={t("admin.fields.email")} registration={form.register("email")} />
        <Field label={t("admin.fields.phone")} registration={form.register("phone")} />
        <Field label={t("admin.fields.address")} registration={form.register("address")} />
        <Checkbox label={t("admin.fields.active")} registration={form.register("active")} />
        <FormActions isSaving={saveMutation.isPending} isEditing={Boolean(editing)} onCancel={() => setIsFormOpen(false)} />
      </form>
    </FormDialog>
    <AdminTable columns={columns} data={suppliersQuery.data?.items ?? []} />
  </AdminPage>;
}

function CustomerScreen() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [query, setQuery] = useState("");
  const [editing, setEditing] = useState<Customer | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const customersQuery = useQuery({ queryKey: ["thirdParties", "customers", query], queryFn: () => listCustomers(query) });
  const form = useForm<CustomerForm>({ resolver: zodResolver(customerSchema), defaultValues: defaultCustomerValues });

  useEffect(() => {
    if (!editing) return;
    form.reset({ name: editing.name, taxId: editing.taxId ?? "", email: editing.email ?? "", phone: editing.phone ?? "", address: editing.address ?? "", genericConsumer: editing.genericConsumer, active: editing.active });
  }, [editing, form]);

  const saveMutation = useMutation({
    mutationFn: (values: CustomerForm) => {
      const payload: CustomerPayload = normalizeCustomerPayload(values);
      return editing ? updateCustomer(editing.id, payload) : createCustomer(payload);
    },
    onSuccess: () => {
      toast.success(t("admin.states.saved"));
      setIsFormOpen(false);
      setEditing(null);
      form.reset(defaultCustomerValues);
      queryClient.invalidateQueries({ queryKey: ["thirdParties", "customers"] });
      queryClient.invalidateQueries({ queryKey: ["customers"] });
    },
  });
  const deactivateMutation = useMutation({ mutationFn: deactivateCustomer, onSuccess: () => queryClient.invalidateQueries({ queryKey: ["thirdParties", "customers"] }) });

  const columns = useMemo<ColumnDef<Customer>[]>(() => [
    { header: t("admin.fields.name"), accessorKey: "name" },
    { header: t("admin.fields.taxId"), cell: ({ row }) => row.original.taxId ?? "-" },
    { header: t("admin.fields.email"), cell: ({ row }) => row.original.email ?? "-" },
    { header: t("admin.fields.genericConsumer"), cell: ({ row }) => row.original.genericConsumer ? t("admin.status.yes") : t("admin.status.no") },
    { header: t("admin.fields.status"), cell: ({ row }) => row.original.active ? t("admin.status.active") : t("admin.status.inactive") },
    { header: t("admin.fields.actions"), cell: ({ row }) => <Actions canUpdate="customers.update" canDelete="customers.delete" active={row.original.active} onEdit={() => { setEditing(row.original); setIsFormOpen(true); }} onDeactivate={() => deactivateMutation.mutate(row.original.id)} /> },
  ], [deactivateMutation, t]);

  return <AdminPage title={t("admin.thirdParties.customersTitle")} description={t("admin.thirdParties.description")} isLoading={customersQuery.isLoading} error={customersQuery.error ?? saveMutation.error ?? deactivateMutation.error}>
    <Toolbar query={query} setQuery={setQuery} canCreate="customers.create" onCreate={() => { setEditing(null); form.reset(defaultCustomerValues); setIsFormOpen(true); }} />
    <FormDialog open={isFormOpen} onOpenChange={(open) => { setIsFormOpen(open); if (!open) { setEditing(null); form.reset(defaultCustomerValues); } }} title={editing ? t("admin.actions.update") : t("admin.actions.create")} description={t("admin.thirdParties.customersTitle")}>
      <form className="grid gap-4 md:grid-cols-2" onSubmit={form.handleSubmit((values) => saveMutation.mutate(values))}>
        <Field label={t("admin.fields.name")} registration={form.register("name")} />
        <Field label={t("admin.fields.taxId")} registration={form.register("taxId")} />
        <Field label={t("admin.fields.email")} registration={form.register("email")} />
        <Field label={t("admin.fields.phone")} registration={form.register("phone")} />
        <Field label={t("admin.fields.address")} registration={form.register("address")} />
        <div />
        <Checkbox label={t("admin.fields.genericConsumer")} registration={form.register("genericConsumer")} />
        <Checkbox label={t("admin.fields.active")} registration={form.register("active")} />
        <FormActions isSaving={saveMutation.isPending} isEditing={Boolean(editing)} onCancel={() => setIsFormOpen(false)} />
      </form>
    </FormDialog>
    <AdminTable columns={columns} data={customersQuery.data?.items ?? []} />
  </AdminPage>;
}

function Toolbar({ query, setQuery, canCreate, onCreate }: Readonly<{ query: string; setQuery: (value: string) => void; canCreate: string; onCreate: () => void }>) {
  const t = useTranslations();
  return <div className="flex flex-col gap-2 md:max-w-3xl md:flex-row"><Input value={query} onChange={(event) => setQuery(event.target.value)} placeholder={t("catalog.search")} />{can(canCreate) ? <Button type="button" onClick={onCreate}>{t("admin.actions.create")}</Button> : null}</div>;
}

function Field({ label, registration }: Readonly<{ label: string; registration: UseFormRegisterReturn }>) {
  return <div className="space-y-2"><Label>{label}</Label><Input {...registration} /></div>;
}

function Checkbox({ label, registration }: Readonly<{ label: string; registration: UseFormRegisterReturn }>) {
  return <label className="flex items-center gap-2 text-sm"><input type="checkbox" {...registration} /> {label}</label>;
}

function FormActions({ isSaving, isEditing, onCancel }: Readonly<{ isSaving: boolean; isEditing: boolean; onCancel: () => void }>) {
  const t = useTranslations();
  return <div className="flex gap-2 md:col-span-2"><Button disabled={isSaving} type="submit">{isEditing ? t("admin.actions.update") : t("admin.actions.create")}</Button><Button type="button" variant="outline" onClick={onCancel}>{t("admin.actions.cancel")}</Button></div>;
}

function Actions({ canUpdate, canDelete, active, onEdit, onDeactivate }: Readonly<{ canUpdate: string; canDelete: string; active: boolean; onEdit: () => void; onDeactivate: () => void }>) {
  const t = useTranslations();
  return <div className="flex gap-2">{can(canUpdate) ? <Button variant="outline" onClick={onEdit}>{t("admin.actions.edit")}</Button> : null}{can(canDelete) && active ? <ConfirmDialog trigger={<Button variant="outline">{t("admin.actions.deactivate")}</Button>} title={t("catalog.deactivateTitle")} description={t("catalog.confirmDeactivate")} confirmLabel={t("admin.actions.deactivate")} cancelLabel={t("admin.actions.cancel")} onConfirm={onDeactivate} /> : null}</div>;
}

function normalizeSupplierPayload(values: SupplierForm): SupplierPayload {
  return { name: values.name, taxId: emptyToNull(values.taxId), email: emptyToNull(values.email), phone: emptyToNull(values.phone), address: emptyToNull(values.address), contactName: emptyToNull(values.contactName), active: values.active };
}

function normalizeCustomerPayload(values: CustomerForm): CustomerPayload {
  return { name: values.name, taxId: emptyToNull(values.taxId), email: emptyToNull(values.email), phone: emptyToNull(values.phone), address: emptyToNull(values.address), genericConsumer: values.genericConsumer, active: values.active };
}

function emptyToNull(value: string | null) {
  const trimmed = value?.trim() ?? "";
  return trimmed ? trimmed : null;
}
