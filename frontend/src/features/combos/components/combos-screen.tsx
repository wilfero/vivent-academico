"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { Plus, Trash2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { useMemo, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { FormDialog } from "@/components/ui/form-dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { AdminTable } from "@/features/admin/shared/admin-table";
import { listGlobalPresentations, listProducts } from "@/features/admin/catalog/shared/catalog-api";
import { canAny } from "@/features/admin/catalog/shared/catalog-permissions";
import { createCombo, deleteCombo, combosQueryKeys, listCombos, updateCombo } from "@/features/combos/api/combos-api";
import type { ComboOffer, ComboOfferFilters, ComboOfferPayload } from "@/features/combos/types/combo-types";
import { formatMoney } from "@/features/sales/components/sale-shared";
import { formatApiErrorMessage } from "@/lib/api/errors";

const emptyFilters: ComboOfferFilters = { page: 1, pageSize: 20, active: "", query: "" };

const emptyPayload: ComboOfferPayload = {
  name: "",
  code: "",
  barcode: null,
  description: null,
  comboPrice: 0,
  startAt: null,
  endAt: null,
  isActive: true,
  items: [],
};

export function CombosScreen() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState<ComboOfferFilters>(emptyFilters);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingCombo, setEditingCombo] = useState<ComboOffer | null>(null);
  const [payload, setPayload] = useState<ComboOfferPayload>(emptyPayload);
  const [deleteId, setDeleteId] = useState<string | null>(null);

  // Item selection state
  const [selectedPresentationId, setSelectedPresentationId] = useState("");
  const [itemQuantity, setItemQuantity] = useState(1);
  const [presentationSearch, setPresentationSearch] = useState("");

  const canLookupProducts = canAny(["products.view"]);
  const canLookupPresentations = canAny(["product_presentations.view"]);

  const combosQuery = useQuery({
    queryKey: combosQueryKeys.list(filters),
    queryFn: () => listCombos(filters),
  });

  const productsQuery = useQuery({
    queryKey: ["combos", "products"],
    queryFn: () => listProducts("", "", true),
    enabled: canLookupProducts,
  });

  const presentationsQuery = useQuery({
    queryKey: ["combos", "presentations"],
    queryFn: () => listGlobalPresentations("", "", true),
    enabled: canLookupPresentations,
  });

  const productById = useMemo(() => new Map((productsQuery.data?.items ?? []).map((product) => [product.id, product])), [productsQuery.data?.items]);
  const presentationById = useMemo(() => new Map((presentationsQuery.data?.items ?? []).map((presentation) => [presentation.id, presentation])), [presentationsQuery.data?.items]);
  const filteredPresentations = useMemo(() => {
    const normalizedSearch = presentationSearch.trim().toLowerCase();
    const presentations = presentationsQuery.data?.items ?? [];
    if (!normalizedSearch) return presentations;
    return presentations.filter((presentation) => {
      const product = productById.get(presentation.productId);
      return [product?.name, presentation.name, presentation.barcode]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(normalizedSearch));
    });
  }, [presentationSearch, presentationsQuery.data?.items, productById]);

  const createMutation = useMutation({
    mutationFn: (data: ComboOfferPayload) => createCombo(data),
    onSuccess: () => {
      toast.success(t("combos.messages.created"));
      queryClient.invalidateQueries({ queryKey: combosQueryKeys.all });
      closeForm();
    },
    onError: (error) => toast.error(formatApiErrorMessage(error, t)),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: ComboOfferPayload }) => updateCombo(id, data),
    onSuccess: () => {
      toast.success(t("combos.messages.updated"));
      queryClient.invalidateQueries({ queryKey: combosQueryKeys.all });
      closeForm();
    },
    onError: (error) => toast.error(formatApiErrorMessage(error, t)),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteCombo(id),
    onSuccess: () => {
      toast.success(t("combos.messages.deleted"));
      queryClient.invalidateQueries({ queryKey: combosQueryKeys.all });
      setDeleteId(null);
    },
    onError: (error) => {
      toast.error(formatApiErrorMessage(error, t));
      setDeleteId(null);
    },
  });

  function closeForm() {
    setIsFormOpen(false);
    setEditingCombo(null);
    setPayload(emptyPayload);
    setSelectedPresentationId("");
    setItemQuantity(1);
    setPresentationSearch("");
  }

  function addItemToPayload() {
    const presentation = presentationById.get(selectedPresentationId);
    if (!presentation) return;

    const existingIndex = payload.items.findIndex((item) => item.presentationId === selectedPresentationId);
    if (existingIndex >= 0) {
      const updated = [...payload.items];
      updated[existingIndex] = { ...updated[existingIndex], quantityPresentation: updated[existingIndex].quantityPresentation + itemQuantity };
      setPayload({ ...payload, items: updated });
    } else {
      setPayload({
        ...payload,
        items: [...payload.items, { productId: presentation.productId, presentationId: presentation.id, quantityPresentation: itemQuantity }],
      });
    }
    setSelectedPresentationId("");
    setItemQuantity(1);
    setPresentationSearch("");
  }

  function removeItem(index: number) {
    setPayload({ ...payload, items: payload.items.filter((_, idx) => idx !== index) });
  }

  function canSave() {
    if (!payload.name.trim() || !payload.code.trim()) return false;
    if (payload.items.length === 0) return false;
    return true;
  }

  const columns: ColumnDef<ComboOffer>[] = [
    { header: t("combos.fields.name"), accessorKey: "name" },
    { header: t("combos.fields.code"), accessorKey: "code" },
    { id: "barcode", header: t("combos.fields.barcode"), cell: ({ row }) => row.original.barcode ?? "-" },
    { id: "comboPrice", header: t("combos.fields.comboPrice"), cell: ({ row }) => formatMoney(row.original.comboPrice) },
    { id: "isActive", header: t("combos.fields.isActive"), cell: ({ row }) => (row.original.isActive ? "✓" : "✗") },
    { id: "items", header: t("combos.fields.items"), cell: ({ row }) => row.original.items.length },
    {
      id: "actions",
      header: "",
      cell: ({ row }) => (
        <div className="flex gap-2">
          <Button
            type="button"
            variant="outline"
            className="h-8 px-3 text-xs"
            onClick={() => {
              setEditingCombo(row.original);
              setPayload({
                name: row.original.name,
                code: row.original.code,
                barcode: row.original.barcode,
                description: row.original.description,
                comboPrice: row.original.comboPrice,
                startAt: row.original.startAt,
                endAt: row.original.endAt,
                isActive: row.original.isActive,
                items: row.original.items.map((item) => ({
                  productId: item.productId,
                  presentationId: item.presentationId,
                  quantityPresentation: item.quantityPresentation,
                })),
              });
              setIsFormOpen(true);
            }}
          >
            {t("combos.actions.edit")}
          </Button>
          <Button type="button" variant="outline" className="h-8 px-3 text-xs border-rose-300 text-rose-600" onClick={() => setDeleteId(row.original.id)}>
            {t("combos.actions.delete")}
          </Button>
        </div>
      ),
    },
  ];

  const handleSubmit = () => {
    if (!canSave()) return;
    if (editingCombo) {
      updateMutation.mutate({ id: editingCombo.id, data: payload });
    } else {
      createMutation.mutate(payload);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black tracking-[-0.04em]">{t("combos.title")}</h1>
          <p className="text-sm text-muted-foreground">{t("combos.description")}</p>
        </div>
        <Button
          type="button"
          onClick={() => {
            setEditingCombo(null);
            setPayload(emptyPayload);
            setSelectedPresentationId("");
            setItemQuantity(1);
            setPresentationSearch("");
            setIsFormOpen(true);
          }}
        >
          {t("combos.actions.create")}
        </Button>
      </div>

      <Card>
        <CardContent className="p-4">
          <Input
            placeholder={t("combos.placeholders.searchCombos")}
            value={filters.query}
            onChange={(e) => setFilters({ ...filters, query: e.target.value, page: 1 })}
          />
        </CardContent>
      </Card>

      <AdminTable
        columns={columns}
        data={combosQuery.data?.items ?? []}
      />

      <FormDialog
        open={isFormOpen}
        onOpenChange={setIsFormOpen}
        title={editingCombo ? t("combos.actions.edit") : t("combos.actions.create")}
        description={t("combos.description")}
      >
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="space-y-2">
              <Label>{t("combos.fields.name")}</Label>
              <Input value={payload.name} onChange={(e) => setPayload({ ...payload, name: e.target.value })} />
            </div>
            <div className="space-y-2">
              <Label>{t("combos.fields.code")}</Label>
              <Input value={payload.code} onChange={(e) => setPayload({ ...payload, code: e.target.value })} />
            </div>
            <div className="space-y-2">
              <Label>{t("combos.fields.barcode")}</Label>
              <Input value={payload.barcode ?? ""} onChange={(e) => setPayload({ ...payload, barcode: e.target.value || null })} />
            </div>
            <div className="space-y-2">
              <Label>{t("combos.fields.comboPrice")}</Label>
              <Input
                type="number"
                step="0.01"
                value={payload.comboPrice}
                onChange={(e) => setPayload({ ...payload, comboPrice: Number(e.target.value) })}
              />
            </div>
          </div>
          <div className="space-y-2">
            <Label>{t("combos.fields.description")}</Label>
            <Input value={payload.description ?? ""} onChange={(e) => setPayload({ ...payload, description: e.target.value || null })} />
          </div>

          {/* Items section */}
          <div className="rounded-2xl border bg-muted/35 p-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <p className="font-bold text-sm">{t("combos.sections.items")}</p>
                <p className="text-xs text-muted-foreground">{payload.items.length} {t("combos.fields.items").toLowerCase()}</p>
              </div>
            </div>

            {/* Item list */}
            {payload.items.length > 0 && (
              <div className="mt-3 grid gap-2">
                {payload.items.map((item, idx) => {
                  const presentation = presentationById.get(item.presentationId);
                  const product = presentation ? productById.get(presentation.productId) : null;
                  return (
                    <div key={idx} className="flex items-center justify-between rounded-xl border bg-background/75 p-3 text-sm">
                      <div>
                        <p className="font-bold">{product?.name ?? item.productId.slice(0, 8)}</p>
                        <p className="text-xs text-muted-foreground">{presentation?.name ?? item.presentationId.slice(0, 8)} · x{item.quantityPresentation}</p>
                      </div>
                      <Button type="button" variant="ghost" className="h-8 px-2 text-rose-500" onClick={() => removeItem(idx)}>
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  );
                })}
              </div>
            )}

            {/* Add item row */}
            <div className="mt-3 grid gap-2">
              <Input
                value={presentationSearch}
                onChange={(e) => setPresentationSearch(e.target.value)}
                placeholder={t("combos.placeholders.searchPresentation")}
              />
              <div className="grid max-h-40 gap-1 overflow-y-auto rounded-xl border bg-background/50 p-2">
                {filteredPresentations.slice(0, 20).map((presentation) => {
                  const product = productById.get(presentation.productId);
                  return (
                    <button
                      key={presentation.id}
                      type="button"
                      className={`cursor-pointer rounded-lg p-2 text-left text-sm transition ${selectedPresentationId === presentation.id ? "bg-primary/15 font-bold" : "hover:bg-muted"}`}
                      onClick={() => {
                        setSelectedPresentationId(presentation.id);
                        setPresentationSearch("");
                      }}
                    >
                      <span className="font-bold">{product?.name ?? presentation.productId.slice(0, 8)}</span>
                      <span className="ml-2 text-muted-foreground">{presentation.name}</span>
                      <span className="ml-2 font-mono text-xs text-muted-foreground">{presentation.barcode ?? "-"}</span>
                    </button>
                  );
                })}
                {presentationSearch && filteredPresentations.length === 0 ? (
                  <p className="p-2 text-xs text-muted-foreground">{t("admin.states.empty")}</p>
                ) : null}
              </div>
              <div className="flex gap-2">
                <Input
                  type="number"
                  min={1}
                  className="w-24"
                  value={itemQuantity}
                  onChange={(e) => setItemQuantity(Math.max(1, Number(e.target.value)))}
                  placeholder={t("combos.fields.quantityPresentation")}
                />
                <Button type="button" className="h-8 px-3 text-xs" disabled={!selectedPresentationId} onClick={addItemToPayload}>
                  <Plus className="mr-1 h-4 w-4" /> {t("combos.actions.addItem")}
                </Button>
              </div>
            </div>

            {payload.items.length === 0 && (
              <p className="mt-3 text-xs text-muted-foreground">{t("combos.validation.itemsRequired")}</p>
            )}
          </div>

          <div className="flex gap-2">
            <Button type="button" disabled={!canSave() || createMutation.isPending || updateMutation.isPending} onClick={handleSubmit}>
              {t("combos.actions.save")}
            </Button>
            <Button type="button" variant="outline" onClick={() => closeForm()}>
              {t("combos.actions.cancel")}
            </Button>
          </div>
        </div>
      </FormDialog>

      <ConfirmDialog
        trigger={null}
        open={deleteId !== null}
        onOpenChange={(open) => !open && setDeleteId(null)}
        title={t("combos.actions.delete")}
        description={t("combos.messages.deleted")}
        confirmLabel={t("combos.actions.delete")}
        cancelLabel={t("combos.actions.cancel")}
        onConfirm={() => deleteId && deleteMutation.mutate(deleteId)}
      />
    </div>
  );
}
