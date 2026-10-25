"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Barcode, CheckCircle2, Layers3, PackagePlus, Save, Search, Trash2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useFieldArray, useForm, useWatch, type FieldErrors, type UseFormRegisterReturn } from "react-hook-form";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { FormDialog } from "@/components/ui/form-dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  createPresentation,
  createProduct,
  listGlobalPresentations,
  listProducts,
  listUnits,
  lookupBarcode,
  lookupExternalBarcode,
  type ExternalBarcodeLookup,
} from "@/features/admin/catalog/shared/catalog-api";
import type { ProductPresentation } from "@/features/admin/catalog/shared/catalog-types";
import { canAny } from "@/features/admin/catalog/shared/catalog-permissions";
import { listBranches } from "@/features/admin/shared/admin-api";
import { createCustomer, listCustomers, type Customer, type CustomerPayload } from "@/features/admin/third-parties/third-party-api";
import { withSessionBranchFallback } from "@/features/auth/lib/session-branch";
import { getComboByBarcode, listCombos } from "@/features/combos/api/combos-api";
import type { ComboOffer } from "@/features/combos/types/combo-types";
import { listInventoryBalances } from "@/features/inventory/api/inventory-api";
import { createSale, confirmSale } from "@/features/sales/api/sales-api";
import { MobileBarcodeScanner } from "@/features/sales/components/mobile-barcode-scanner";
import { formatMoney, formatNumber } from "@/features/sales/components/sale-shared";
import { canApplySaleDiscounts, canCreateSales, canLookupSalePresentations, canLookupSaleProducts, canRemoveSaleItems } from "@/features/sales/lib/sale-permissions";
import { defaultSaleCreateValues, saleCreateSchema, toSalePayload, type SaleCreateForm, type SaleCreateInput } from "@/features/sales/schemas/sale-schema";
import { formatPermissionError } from "@/lib/api/errors";

const paymentMethods = ["CASH", "CARD", "TRANSFER", "CREDIT", "OTHER"] as const;
type PendingCartItem =
  | { type: "presentation"; presentation: ProductPresentation; sourceBarcode: string }
  | { type: "combo"; combo: ComboOffer };

export function SalePosScreen() {
  const t = useTranslations();
  const router = useRouter();
  const queryClient = useQueryClient();
  const [barcode, setBarcode] = useState("");
  const [selectedPresentationId, setSelectedPresentationId] = useState("");
  const [presentationSearch, setPresentationSearch] = useState("");
  const [quickQuantity, setQuickQuantity] = useState(1);
  const [isProductSearchOpen, setIsProductSearchOpen] = useState(false);
  const [isCustomerDialogOpen, setIsCustomerDialogOpen] = useState(false);
  const [isFinalizeOpen, setIsFinalizeOpen] = useState(false);
  const [isMobileScannerOpen, setIsMobileScannerOpen] = useState(false);
  const [lastAddedKey, setLastAddedKey] = useState(0);
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
  const [customerTaxId, setCustomerTaxId] = useState("");
  const [customerName, setCustomerName] = useState("");
  const [customerEmail, setCustomerEmail] = useState("");
  const [customerPhone, setCustomerPhone] = useState("");
  const [customerAddress, setCustomerAddress] = useState("CIUDAD");
  const [missingCustomerTaxId, setMissingCustomerTaxId] = useState("");
  const [pendingItem, setPendingItem] = useState<PendingCartItem | null>(null);
  const [externalProduct, setExternalProduct] = useState<ExternalBarcodeLookup | null>(null);
  const [externalName, setExternalName] = useState("");
  const [externalSalePrice, setExternalSalePrice] = useState("");
  const [externalUnitCode, setExternalUnitCode] = useState("unit");
  const cartEndRef = useRef<HTMLDivElement | null>(null);
  const canCreate = canCreateSales();
  const canLookupProducts = canLookupSaleProducts();
  const canLookupPresentations = canLookupSalePresentations();
  const canDiscount = canApplySaleDiscounts();
  const canRemove = canRemoveSaleItems();
  const canViewStock = canAny(["inventory.view"]);
  const canReadBranches = canAny(["settings.view"]);
  const canReadCustomers = canAny(["customers.view"]);
  const canCreateCustomers = canAny(["customers.create"]);
  const canViewCombos = canAny(["combos.view"]);
  const canCreateProducts = canAny(["products.create"]);
  const canViewUnits = canAny(["units.view"]);

  const form = useForm<SaleCreateInput, unknown, SaleCreateForm>({ resolver: zodResolver(saleCreateSchema), defaultValues: defaultSaleCreateValues, reValidateMode: "onBlur" });
  const { fields, append, remove, update } = useFieldArray({ control: form.control, name: "details" });
  const { fields: comboFields, append: appendComboField, remove: removeCombo, update: updateComboLine } = useFieldArray({ control: form.control, name: "comboLines" });
  const watchedDetailsValue = useWatch({ control: form.control, name: "details" });
  const watchedDetails = useMemo(() => watchedDetailsValue ?? [], [watchedDetailsValue]);
  const watchedComboLinesValue = useWatch({ control: form.control, name: "comboLines" });
  const watchedComboLines = useMemo(() => watchedComboLinesValue ?? [], [watchedComboLinesValue]);
  const selectedBranchId = useWatch({ control: form.control, name: "branchId" }) ?? "";
  const discountAmount = Number(useWatch({ control: form.control, name: "discountAmount" }) ?? 0);

  const branchesQuery = useQuery({ queryKey: ["sales", "branches"], queryFn: () => listBranches(), enabled: canCreate && canReadBranches });
  const productsQuery = useQuery({ queryKey: ["sales", "products"], queryFn: () => listProducts("", "", true), enabled: canCreate && canLookupProducts });
  const presentationsQuery = useQuery({ queryKey: ["sales", "presentations"], queryFn: () => listGlobalPresentations("", "", true), enabled: canCreate && canLookupPresentations });
  const balancesQuery = useQuery({
    queryKey: ["sales", "inventory-balances", selectedBranchId],
    queryFn: () => listInventoryBalances({ branchId: selectedBranchId, locationId: "", productId: "", categoryId: "", query: "", movementType: "", fromDate: "", toDate: "", page: 1, pageSize: 100 }),
    enabled: canCreate && canViewStock && Boolean(selectedBranchId),
  });
  const customersQuery = useQuery({ queryKey: ["sales", "customers"], queryFn: () => listCustomers("", true), enabled: canCreate && canReadCustomers });
  const combosQuery = useQuery({ queryKey: ["sales", "combos"], queryFn: () => listCombos({ page: 1, pageSize: 100, active: true, query: "" }), enabled: canCreate && canViewCombos });
  const unitsQuery = useQuery({ queryKey: ["sales", "units"], queryFn: () => listUnits(), enabled: canCreateProducts && canViewUnits && externalProduct != null });

  const branchOptions = useMemo(() => withSessionBranchFallback((branchesQuery.data?.items ?? []).map((branch) => ({ value: branch.id, label: branch.name }))), [branchesQuery.data?.items]);
  const productById = useMemo(() => new Map((productsQuery.data?.items ?? []).map((product) => [product.id, product])), [productsQuery.data?.items]);
  const presentationById = useMemo(() => new Map((presentationsQuery.data?.items ?? []).map((presentation) => [presentation.id, presentation])), [presentationsQuery.data?.items]);
  const stockByProductId = useMemo(() => {
    const stock = new Map<string, number>();
    for (const balance of balancesQuery.data?.items ?? []) {
      stock.set(balance.productId, (stock.get(balance.productId) ?? 0) + Number(balance.quantityAvailable));
    }
    return stock;
  }, [balancesQuery.data?.items]);
  const comboById = useMemo(() => new Map((combosQuery.data?.items ?? []).map((combo) => [combo.id, combo])), [combosQuery.data?.items]);
  const comboByBarcode = useMemo(() => {
    const map = new Map<string, ComboOffer>();
    for (const combo of combosQuery.data?.items ?? []) {
      if (combo.barcode) map.set(combo.barcode, combo);
    }
    return map;
  }, [combosQuery.data?.items]);
  const totals = useMemo(() => calculateDraftTotals(watchedDetails, watchedComboLines, canDiscount ? discountAmount : 0, comboById), [canDiscount, discountAmount, watchedDetails, watchedComboLines, comboById]);
  const filteredPresentations = useMemo(() => {
    const normalizedSearch = presentationSearch.trim().toLowerCase();
    const presentations = presentationsQuery.data?.items ?? [];

    if (!normalizedSearch) {
      return presentations;
    }

    return presentations.filter((presentation) => {
      const product = productById.get(presentation.productId);
      return [product?.name, presentation.name, presentation.barcode]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(normalizedSearch));
    });
  }, [presentationSearch, presentationsQuery.data?.items, productById]);
  const filteredCombos = useMemo(() => {
    const normalizedSearch = presentationSearch.trim().toLowerCase();
    const combos = combosQuery.data?.items ?? [];

    if (!normalizedSearch) {
      return combos;
    }

    return combos.filter((combo) => [combo.name, combo.code, combo.barcode]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(normalizedSearch)));
  }, [combosQuery.data?.items, presentationSearch]);
  const requestedBaseByProductId = useMemo(() => {
    const requested = new Map<string, number>();
    for (const detail of watchedDetails) {
      const presentation = presentationById.get(detail.presentationId);
      if (!presentation) continue;
      const quantityBase = Number(detail.quantityPresentation || 0) * Number(presentation.conversionFactor || 0);
      requested.set(presentation.productId, (requested.get(presentation.productId) ?? 0) + quantityBase);
    }
    for (const line of watchedComboLines) {
      const combo = comboById.get(line.comboOfferId);
      if (!combo) continue;
      for (const item of combo.items) {
        const quantityBase = Number(item.quantityBase || 0) * Number(line.quantity || 0);
        requested.set(item.productId, (requested.get(item.productId) ?? 0) + quantityBase);
      }
    }
    return requested;
  }, [comboById, presentationById, watchedComboLines, watchedDetails]);
  const lookupError = (canReadBranches ? branchesQuery.error : null) ?? productsQuery.error ?? presentationsQuery.error ?? balancesQuery.error ?? (canReadCustomers ? customersQuery.error : null) ?? (canViewCombos ? combosQuery.error : null);

  const createCustomerMutation = useMutation({
    mutationFn: createCustomer,
    onSuccess: (customer) => {
      queryClient.invalidateQueries({ queryKey: ["sales", "customers"] });
      queryClient.invalidateQueries({ queryKey: ["thirdParties", "customers"] });
      applyCustomerAndPendingLine(customer);
      toast.success(t("sales.customer.created"));
    },
    onError: (error) => toast.error(formatErrorMessage(error, t)),
  });

  const createMutation = useMutation({
    mutationFn: (values: SaleCreateForm) => createSale(toSalePayload(values, canDiscount)),
    onSuccess: (sale) => {
      toast.success(t("sales.messages.created"));
      invalidateSaleImpact(queryClient, sale.id);
      router.push(`/sales/${sale.id}`);
    },
    onError: (error) => toast.error(formatErrorMessage(error, t)),
  });

  const finalizeMutation = useMutation({
    mutationFn: async (values: SaleCreateForm) => {
      const sale = await createSale(toSalePayload(values, canDiscount));
      return confirmSale(sale.id);
    },
    onSuccess: (sale) => {
      toast.success(t("sales.messages.confirmed"));
      invalidateSaleImpact(queryClient, sale.id);
      router.push(`/sales/${sale.id}`);
    },
    onError: (error) => toast.error(formatErrorMessage(error, t)),
  });

  const barcodeMutation = useMutation({
    mutationFn: (value: string) => lookupBarcode(value),
    onSuccess: (lookup, value) => {
      const appended = appendPresentation(lookup.presentation, lookup.presentation.barcode ?? value);
      setBarcode("");
      focusBarcodeInput();
      if (appended) {
        toast.success(t("sales.messages.itemAdded"));
      }
    },
    onError: (error, value) => {
      // Fallback: try combo barcode lookup
      const combo = comboByBarcode.get(value);
      if (combo) {
        const appended = appendCombo(combo);
        setBarcode("");
        focusBarcodeInput();
        if (appended) {
          toast.success(t("sales.messages.itemAdded"));
        }
        return;
      }
      // Try live combo barcode lookup
      if (canViewCombos) {
        getComboByBarcode(value).then((comboResponse) => {
          const appended = appendCombo(comboResponse);
          setBarcode("");
          focusBarcodeInput();
          if (appended) {
            toast.success(t("sales.messages.itemAdded"));
          }
          queryClient.invalidateQueries({ queryKey: ["sales", "combos"] });
        }).catch(() => {
          offerExternalProduct(value, error);
        });
      } else {
        offerExternalProduct(value, error);
      }
    },
  });

  function offerExternalProduct(value: string, originalError: unknown) {
    // Third-party integration fallback: when the local catalog does not know
    // the barcode, ask the external catalog (Open Food Facts). If it is
    // found there, offer to register the product and keep selling.
    if (!canCreateProducts) {
      toast.error(formatErrorMessage(originalError, t));
      return;
    }

    lookupExternalBarcode(value).then((external) => {
      if (external?.found && external.name) {
        setExternalProduct(external);
        setExternalName(external.name);
        setExternalSalePrice("");
        setExternalUnitCode("unit");
        return;
      }
      toast.error(formatErrorMessage(originalError, t));
    }).catch(() => {
      toast.error(formatErrorMessage(originalError, t));
    });
  }

  const externalProductMutation = useMutation({
    mutationFn: async () => {
      const salePrice = Number(externalSalePrice.replace(",", "."));
      if (!externalName.trim() || Number.isNaN(salePrice) || salePrice < 0) {
        throw new Error("invalid");
      }

      const product = await createProduct({
        name: externalName.trim(),
        description: externalProduct?.brand ? `Marca: ${externalProduct.brand}` : null,
        baseUnitCode: externalUnitCode || "unit",
        sku: null,
        minimumStock: 0,
        stockable: true,
        hasExpirationDate: false,
        hasLotTracking: false,
        hasSerialTracking: false,
        active: true,
        categoryId: null,
      });

      return createPresentation(product.id, {
        name: "Unidad",
        barcode: externalProduct?.barcode ?? null,
        conversionFactor: 1,
        purchasePriceSuggested: 0,
        salePrice,
        defaultPurchase: true,
        defaultSale: true,
        active: true,
      });
    },
    onSuccess: (presentation) => {
      queryClient.invalidateQueries({ queryKey: ["sales", "products"] });
      queryClient.invalidateQueries({ queryKey: ["sales", "presentations"] });
      setExternalProduct(null);
      setBarcode("");
      focusBarcodeInput();
      const appended = appendPresentation(presentation, presentation.barcode ?? "");
      if (appended) {
        toast.success(t("sales.messages.itemAdded"));
      } else {
        toast.success(t("sales.messages.itemAdded"));
      }
    },
    onError: (error) => {
      if ((error as Error).message === "invalid") {
        toast.error(t("sales.externalProduct.invalidFields"));
        return;
      }
      toast.error(formatErrorMessage(error, t));
    },
  });

  function appendPresentation(presentation: ProductPresentation, sourceBarcode = "") {
    if (fields.length === 0 && comboFields.length === 0 && !form.getValues("customerId")) {
      requestCustomer({ type: "presentation", presentation, sourceBarcode });
      return false;
    }

    appendPresentationToCart(presentation, sourceBarcode);
    return true;
  }

  function appendPresentationToCart(presentation: ProductPresentation, sourceBarcode = "") {
    const existingIndex = watchedDetails.findIndex((detail) => detail.presentationId === presentation.id);
    if (existingIndex >= 0) {
      const current = watchedDetails[existingIndex];
      update(existingIndex, { ...current, quantityPresentation: Number(current.quantityPresentation || 0) + quickQuantity });
      setLastAddedKey((currentKey) => currentKey + 1);
      setQuickQuantity(1);
      return;
    }

    append({ presentationId: presentation.id, barcode: sourceBarcode, quantityPresentation: quickQuantity, unitPrice: presentation.salePrice, discountAmount: 0, taxAmount: 0 });
    setLastAddedKey((currentKey) => currentKey + 1);
    setQuickQuantity(1);
  }

  function appendCombo(combo: ComboOffer) {
    if (fields.length === 0 && comboFields.length === 0 && !form.getValues("customerId")) {
      requestCustomer({ type: "combo", combo });
      return false;
    }

    appendComboToCart(combo);
    return true;
  }

  function appendComboToCart(combo: ComboOffer) {
    const watchedComboLines = form.getValues("comboLines") ?? [];
    const existingIndex = watchedComboLines.findIndex((line) => line.comboOfferId === combo.id);
    if (existingIndex >= 0) {
      const current = watchedComboLines[existingIndex];
      updateComboLine(existingIndex, { ...current, quantity: Number(current.quantity || 0) + quickQuantity });
    } else {
      appendComboField({ comboOfferId: combo.id, quantity: quickQuantity });
    }
    setLastAddedKey((currentKey) => currentKey + 1);
    setQuickQuantity(1);
  }

  function requestCustomer(item: PendingCartItem) {
    setPendingItem(item);
    setCustomerTaxId("");
    setMissingCustomerTaxId("");
    setCustomerName("");
    setCustomerEmail("");
    setCustomerPhone("");
    setCustomerAddress("CIUDAD");
    setIsCustomerDialogOpen(true);
  }

  function applyCustomerAndPendingLine(customer: Customer) {
    setSelectedCustomer(customer);
    form.setValue("customerId", customer.id, { shouldDirty: true, shouldValidate: true });
    if (pendingItem?.type === "presentation") {
      appendPresentationToCart(pendingItem.presentation, pendingItem.sourceBarcode);
    } else if (pendingItem?.type === "combo") {
      appendComboToCart(pendingItem.combo);
    }
    resetCustomerDialog();
  }

  function resetCustomerDialog() {
    setIsCustomerDialogOpen(false);
    setPendingItem(null);
    setCustomerTaxId("");
    setMissingCustomerTaxId("");
    setCustomerName("");
    setCustomerEmail("");
    setCustomerPhone("");
    setCustomerAddress("CIUDAD");
  }

  function selectCustomerByTaxId() {
    const normalizedTaxId = customerTaxId.trim();
    if (!normalizedTaxId) {
      selectFinalConsumer();
      return;
    }

    const customer = findCustomerByTaxId(customersQuery.data?.items ?? [], normalizedTaxId);
    if (customer) {
      applyCustomerAndPendingLine(customer);
      return;
    }

    setMissingCustomerTaxId(normalizedTaxId);
    setCustomerName("");
    setCustomerEmail("");
    setCustomerPhone("");
    setCustomerAddress("CIUDAD");
  }

  function selectFinalConsumer() {
    const existing = (customersQuery.data?.items ?? []).find((customer) => customer.genericConsumer || customer.taxId?.toUpperCase() === "CF");
    if (existing) {
      applyCustomerAndPendingLine(existing);
      return;
    }

    if (!canCreateCustomers) {
      toast.error(formatErrorMessage(new Error(t("sales.customer.missingCf")), t));
      return;
    }

    createCustomerMutation.mutate({ name: "Consumidor Final", taxId: "CF", email: null, phone: null, address: "CIUDAD", genericConsumer: true, active: true });
  }

  function createMissingCustomer() {
    const payload: CustomerPayload = {
      name: customerName.trim(),
      taxId: missingCustomerTaxId,
      email: emptyToNull(customerEmail),
      phone: emptyToNull(customerPhone),
      address: customerAddress.trim() || "CIUDAD",
      genericConsumer: false,
      active: true,
    };
    createCustomerMutation.mutate(payload);
  }

  function addSelectedPresentation() {
    const presentation = presentationById.get(selectedPresentationId);
    if (!presentation) return;
    appendPresentation(presentation, presentation.barcode ?? "");
    setSelectedPresentationId("");
    setPresentationSearch("");
    setIsProductSearchOpen(false);
    focusBarcodeInput();
  }

  function scanBarcode(value: string) {
    const normalizedBarcode = value.trim();

    if (!normalizedBarcode || barcodeMutation.isPending) return;

    setBarcode(normalizedBarcode);
    barcodeMutation.mutate(normalizedBarcode);
  }

  const submitDraft = useCallback((values: SaleCreateForm) => {
    createMutation.mutate(values);
  }, [createMutation]);

  const submitAndConfirm = useCallback((values: SaleCreateForm) => {
    finalizeMutation.mutate(values);
  }, [finalizeMutation]);

  const openFinalize = useCallback(() => {
    if (fields.length === 0 && comboFields.length === 0) return;
    setIsFinalizeOpen(true);
  }, [fields.length, comboFields.length]);

  useEffect(() => {
    if (branchOptions.length === 1 && !form.getValues("branchId")) {
      form.setValue("branchId", branchOptions[0].value, { shouldValidate: true });
    }
  }, [branchOptions, form]);

  useEffect(() => {
    function handleShortcut(event: KeyboardEvent) {
      const withModifier = event.ctrlKey || event.metaKey;
      if (!withModifier) return;

      if (event.key === "Enter" && (fields.length > 0 || comboFields.length > 0)) {
        event.preventDefault();
        openFinalize();
      }

      if (event.key.toLowerCase() === "s") {
        event.preventDefault();
        form.handleSubmit(submitDraft)();
      }

      if (event.key.toLowerCase() === "b") {
        event.preventDefault();
        setIsProductSearchOpen(true);
      }
    }

    window.addEventListener("keydown", handleShortcut);
    return () => window.removeEventListener("keydown", handleShortcut);
  }, [fields.length, comboFields.length, form, openFinalize, submitDraft]);

  useEffect(() => {
    if (lastAddedKey === 0) return;
    cartEndRef.current?.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }, [lastAddedKey]);

  if (!canCreate) {
    return <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("accessDenied.description")}</CardContent></Card>;
  }

  return (
    <form className="space-y-6" onSubmit={form.handleSubmit(submitDraft)}>
      {lookupError ? <Card><CardContent className="p-6 text-sm text-destructive">{formatErrorMessage(lookupError, t)}</CardContent></Card> : null}
      {createMutation.error || finalizeMutation.error || barcodeMutation.error ? <Card><CardContent className="p-6 text-sm text-destructive">{firstErrorMessage(t, createMutation.error, finalizeMutation.error, barcodeMutation.error)}</CardContent></Card> : null}

      <Card className="overflow-hidden border bg-card">
        <CardContent className="grid gap-4 p-5 lg:grid-cols-[1fr_360px]">
          <div className="grid gap-4 md:grid-cols-4">
            <SelectField className="md:col-span-2" label={t("sales.fields.branch")} registration={form.register("branchId")} options={branchOptions} error={form.formState.errors.branchId?.message} />
            <div className="rounded-2xl border bg-background/75 p-3 text-sm md:col-span-4">
              <p className="font-bold">{selectedCustomer?.name ?? t("sales.finalConsumer")}</p>
              <p className="mt-1 text-xs text-muted-foreground">{selectedCustomer ? `${t("admin.fields.taxId")}: ${selectedCustomer.taxId ?? "-"}` : t("sales.placeholders.customers")}</p>
            </div>
          </div>
          <div className="rounded-3xl border bg-background/80 p-4 shadow-inner">
            <p className="text-xs font-black uppercase tracking-[0.18em] text-muted-foreground">{t("sales.sections.totals")}</p>
            <SummaryRow label={t("sales.fields.subtotal")} value={formatMoney(totals.subtotal)} />
            <SummaryRow label={t("sales.fields.taxAmount")} value={formatMoney(totals.taxAmount)} />
            <SummaryRow label={t("sales.fields.discountAmount")} value={formatMoney(canDiscount ? discountAmount + totals.lineDiscounts : totals.lineDiscounts)} />
            <div className="mt-3 border-t pt-3"><SummaryRow strong label={t("sales.fields.totalAmount")} value={formatMoney(totals.totalAmount)} /></div>
          </div>
        </CardContent>
      </Card>

      <section className="space-y-3">
        <div>
          <h2 className="text-xl font-black tracking-[-0.04em]">{t("sales.sections.cart")}</h2>
          <p className="text-sm text-muted-foreground">{t("sales.help.cart")}</p>
        </div>
        {fields.length === 0 && comboFields.length === 0 ? <Card><CardContent className="p-6 text-sm text-muted-foreground">{t("sales.emptyCart")}</CardContent></Card> : null}
        <div className="grid gap-3">
          {fields.map((field, index) => {
            const detail = watchedDetails[index];
            const presentation = presentationById.get(detail?.presentationId ?? "");
            const product = presentation ? productById.get(presentation.productId) : null;
            const available = product ? stockByProductId.get(product.id) : undefined;
            const requestedBase = Number(detail?.quantityPresentation || 0) * Number(presentation?.conversionFactor || 0);
            const totalRequestedBase = product ? requestedBaseByProductId.get(product.id) ?? requestedBase : requestedBase;
            const lowStock = available !== undefined && totalRequestedBase > available;

            return (
              <Card key={field.id} className={lowStock ? "border-amber-300 bg-amber-50/50 dark:border-amber-800 dark:bg-amber-950/20" : "border-dashed"}>
                <CardContent className="grid gap-4 p-4 md:grid-cols-12 md:items-end">
                  <div className="md:col-span-3">
                    <p className="font-bold">{product?.name ?? t("sales.placeholders.selectPresentation")}</p>
                    <p className="mt-1 text-xs text-muted-foreground">{presentation?.name ?? "-"}</p>
                    <p className="mt-1 font-mono text-xs text-muted-foreground">{detail?.barcode || presentation?.barcode || "-"}</p>
                  </div>
                  <FormField className="md:col-span-2" label={t("sales.fields.quantityPresentation")} registration={form.register(`details.${index}.quantityPresentation` as const, { valueAsNumber: true })} type="number" error={lineError(form.formState.errors, index, "quantityPresentation")} />
                  <FormField className="md:col-span-2" label={t("sales.fields.unitPrice")} registration={form.register(`details.${index}.unitPrice` as const, { valueAsNumber: true })} type="number" error={lineError(form.formState.errors, index, "unitPrice")} />
                  {canDiscount ? <FormField className="md:col-span-2" label={t("sales.fields.discountAmount")} registration={form.register(`details.${index}.discountAmount` as const, { valueAsNumber: true })} type="number" /> : <div className="rounded-2xl border bg-muted/35 p-3 text-xs text-muted-foreground md:col-span-2">{t("sales.placeholders.discountPermission")}</div>}
                  <div className="text-sm md:col-span-2">
                    <p>{t("sales.fields.quantityBase")}: <span className="font-bold">{formatNumber(requestedBase)}</span></p>
                    <p>{t("sales.fields.available")}: <span className={lowStock ? "font-bold text-amber-700 dark:text-amber-300" : "font-bold"}>{available === undefined ? t("sales.placeholders.stockUnavailable") : formatNumber(available)}</span></p>
                    <p>{t("sales.fields.lineTotal")}: <span className="font-bold">{formatMoney(lineTotal(detail))}</span></p>
                  </div>
                  <div className="md:col-span-1">{canRemove ? <Button className="w-full" type="button" variant="outline" onClick={() => remove(index)}><Trash2 className="h-4 w-4" aria-hidden="true" /></Button> : null}</div>
                </CardContent>
              </Card>
            );
          })}
          <div ref={cartEndRef} />
        </div>

        {/* Combo lines */}
        {comboFields.length > 0 ? <div className="mt-4 grid gap-3">
          <p className="text-sm font-black uppercase tracking-[0.12em] text-muted-foreground"><Layers3 className="mr-2 inline h-4 w-4" />{t("sales.sections.combos")}</p>
          {comboFields.map((field, index) => {
            const line = watchedComboLines[index];
            const combo = comboById.get(line?.comboOfferId ?? "");
            const quantity = Number(line?.quantity || 0);
            const availableCombos = combo ? calculateAvailableCombos(combo, stockByProductId) : null;
            const quantityBaseLabel = combo ? formatComboBaseQuantity(combo, quantity, productById) : "-";
            const insufficientComponents = combo?.items.filter((item) => {
              const available = stockByProductId.get(item.productId);
              const requested = requestedBaseByProductId.get(item.productId) ?? 0;
              return available !== undefined && requested > available;
            }) ?? [];
            const lowStock = insufficientComponents.length > 0;
            return (
              <Card key={field.id} className={lowStock ? "border-amber-300 bg-amber-50/50 dark:border-amber-800 dark:bg-amber-950/20" : "border-dashed"}>
                <CardContent className="grid gap-4 p-4 md:grid-cols-12 md:items-end">
                  <div className="md:col-span-3">
                    <p className="font-bold">{combo?.name ?? line?.comboOfferId?.slice(0, 8) ?? "-"}</p>
                    <p className="mt-1 text-xs text-muted-foreground">{combo?.code ?? "-"}</p>
                    <p className="mt-1 font-mono text-xs text-muted-foreground">{combo?.barcode ?? "-"}</p>
                    {lowStock ? <p className="mt-1 text-xs font-bold text-amber-700 dark:text-amber-300">{t("sales.messages.comboStockWarning", { components: insufficientComponents.map((item) => item.productName).join(", ") })}</p> : null}
                  </div>
                  <FormField className="md:col-span-2" label={t("sales.fields.comboQuantity")} registration={form.register(`comboLines.${index}.quantity` as const, { valueAsNumber: true })} type="number" />
                  <div className="space-y-2 md:col-span-2">
                    <Label>{t("sales.fields.unitPrice")}</Label>
                    <Input value={combo ? formatMoney(combo.comboPrice) : "-"} readOnly aria-readonly="true" />
                  </div>
                  <div className="text-sm md:col-span-4">
                    <p>{t("sales.fields.quantityBase")}: <span className="font-bold">{quantityBaseLabel}</span></p>
                    <p>{t("sales.fields.available")}: <span className={lowStock ? "font-bold text-amber-700 dark:text-amber-300" : "font-bold"}>{availableCombos === null ? t("sales.placeholders.stockUnavailable") : t("sales.units.combo", { count: availableCombos })}</span></p>
                    <p>{t("sales.fields.lineTotal")}: <span className="font-bold">{combo ? formatMoney(quantity * combo.comboPrice) : "-"}</span></p>
                  </div>
                  <div className="md:col-span-1">{canRemove ? <Button className="w-full" type="button" variant="outline" onClick={() => removeCombo(index)}><Trash2 className="h-4 w-4" aria-hidden="true" /></Button> : null}</div>
                </CardContent>
              </Card>
            );
          })}
        </div> : null}
      </section>

      <Card className={`sticky z-20 border bg-card/95 shadow-2xl shadow-foreground/10 backdrop-blur supports-[backdrop-filter]:bg-card/80 ${isMobileScannerOpen ? "bottom-0 rounded-t-3xl" : "bottom-3"}`}>
        <CardContent className={isMobileScannerOpen ? "space-y-2 p-3" : "space-y-3 p-3 lg:p-5"}>
          {/* Scanner dock mode (mobile only): camera + quantity */}
          {isMobileScannerOpen ? (
            <div className="grid gap-3">
              <MobileBarcodeScanner
                isOpen={isMobileScannerOpen}
                disabled={!canLookupPresentations}
                paused={isCustomerDialogOpen || isFinalizeOpen || isProductSearchOpen}
                processing={barcodeMutation.isPending}
                onDetected={scanBarcode}
                onOpenChange={setIsMobileScannerOpen}
              />
              <QuickNumberField label={t("sales.fields.quantityPresentation")} value={quickQuantity} onValueChange={setQuickQuantity} type="number" />
            </div>
          ) : (
            <>
              <div className="hidden rounded-2xl border bg-muted/35 p-3 text-xs text-muted-foreground lg:block">
                {t("sales.shortcuts.scan")} · {t("sales.shortcuts.search")} · {t("sales.shortcuts.finish")} · {t("sales.shortcuts.save")}
              </div>
              <MobileBarcodeScanner
                isOpen={false}
                disabled={!canLookupPresentations}
                paused={isCustomerDialogOpen || isFinalizeOpen || isProductSearchOpen}
                processing={barcodeMutation.isPending}
                onDetected={scanBarcode}
                onOpenChange={setIsMobileScannerOpen}
              />
              <div className="grid gap-4 lg:grid-cols-[1fr_160px_auto_auto] lg:items-end">
              <div className="space-y-2">
                <Label>{t("sales.fields.barcode")}</Label>
                <div className="relative">
                  <Barcode className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
                  <Input id="sale-barcode-input" className="pl-9 font-mono text-base" value={barcode} autoFocus onChange={(event) => setBarcode(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter") { event.preventDefault(); scanBarcode(barcode); } }} placeholder={t("sales.placeholders.scanBarcode")} />
                </div>
              </div>
              <QuickNumberField label={t("sales.fields.quantityPresentation")} value={quickQuantity} onValueChange={setQuickQuantity} type="number" />
              <Button type="button" disabled={!barcode.trim() || barcodeMutation.isPending} onClick={() => scanBarcode(barcode)}>{t("sales.actions.addBarcode")} (Enter)</Button>
              <Button type="button" variant="outline" onClick={() => setIsProductSearchOpen(true)}><Search className="mr-2 h-4 w-4" aria-hidden="true" />{t("sales.actions.searchProduct")} (Ctrl+B)</Button>
              </div>
            </>
          )}
        </CardContent>
      </Card>

      <Card className={isMobileScannerOpen ? "hidden lg:block" : ""}>
        <CardContent className="grid gap-4 p-5 md:grid-cols-[1fr_260px] md:items-end">
          <div className="space-y-4">
            {canDiscount ? <FormField label={t("sales.fields.globalDiscount")} registration={form.register("discountAmount", { valueAsNumber: true })} type="number" error={form.formState.errors.discountAmount?.message} /> : null}
            <div className="space-y-2"><Label>{t("sales.fields.notes")}</Label><Input {...form.register("notes")} /></div>
          </div>
          <div className="grid gap-2">
            <Button disabled={createMutation.isPending || finalizeMutation.isPending} type="submit"><Save className="mr-2 h-4 w-4" aria-hidden="true" />{t("sales.actions.saveDraft")} (Ctrl+S)</Button>
            <Button disabled={createMutation.isPending || finalizeMutation.isPending || (fields.length === 0 && comboFields.length === 0)} type="button" onClick={openFinalize}><CheckCircle2 className="mr-2 h-4 w-4" aria-hidden="true" />{t("sales.actions.confirm")} (Ctrl+Enter)</Button>
            <Button type="button" variant="outline" onClick={() => router.push("/sales")}>{t("sales.actions.back")}</Button>
          </div>
        </CardContent>
      </Card>

      <FormDialog open={isFinalizeOpen} onOpenChange={setIsFinalizeOpen} title={t("sales.finalize.title")} description={t("sales.finalize.description")}>
        <div className="space-y-4">
          <div className="grid gap-4 md:grid-cols-2">
            <SelectField label={t("sales.fields.paymentMethod")} registration={form.register("paymentMethod")} options={paymentMethods.map((method) => ({ value: method, label: t(`sales.paymentMethods.${method}`) }))} />
            <FormField label={t("sales.fields.saleNumber")} registration={form.register("saleNumber")} placeholder={t("sales.placeholders.autoNumber")} />
          </div>
          <div className="rounded-3xl border bg-muted/35 p-4 text-sm">
            <SummaryRow label={t("sales.fields.subtotal")} value={formatMoney(totals.subtotal)} />
            <SummaryRow label={t("sales.fields.taxAmount")} value={formatMoney(totals.taxAmount)} />
            <SummaryRow label={t("sales.fields.discountAmount")} value={formatMoney(canDiscount ? discountAmount + totals.lineDiscounts : totals.lineDiscounts)} />
            <div className="mt-3 border-t pt-3"><SummaryRow strong label={t("sales.fields.totalAmount")} value={formatMoney(totals.totalAmount)} /></div>
          </div>
          <div className="flex flex-wrap justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => setIsFinalizeOpen(false)}>{t("admin.actions.cancel")}</Button>
            <Button disabled={finalizeMutation.isPending} type="button" onClick={form.handleSubmit(submitAndConfirm)}><CheckCircle2 className="mr-2 h-4 w-4" aria-hidden="true" />{t("sales.actions.confirm")}</Button>
          </div>
        </div>
      </FormDialog>

      <FormDialog open={isCustomerDialogOpen} onOpenChange={(open) => { if (!open && !form.getValues("customerId")) return; setIsCustomerDialogOpen(open); }} title={t("sales.customer.title")} description={t("sales.customer.description")}>
        <div className="space-y-4">
          {!missingCustomerTaxId ? <div className="space-y-3">
            <div className="space-y-2"><Label>{t("sales.customer.taxId")}</Label><Input autoFocus value={customerTaxId} onChange={(event) => setCustomerTaxId(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter") { event.preventDefault(); selectCustomerByTaxId(); } }} placeholder={t("sales.customer.taxIdPlaceholder")} /></div>
            <div className="rounded-2xl border bg-muted/35 p-3 text-xs text-muted-foreground">{t("sales.customer.cfHelp")}</div>
            <div className="flex flex-wrap justify-end gap-2"><Button type="button" variant="outline" onClick={selectFinalConsumer}>{t("sales.customer.useCf")}</Button><Button type="button" disabled={customersQuery.isLoading} onClick={selectCustomerByTaxId}>{t("sales.customer.continue")}</Button></div>
          </div> : <div className="grid gap-4 md:grid-cols-2">
            <div className="rounded-2xl border bg-muted/35 p-3 text-sm md:col-span-2"><p className="font-bold">{t("sales.customer.notFound")}</p><p className="mt-1 text-xs text-muted-foreground">{missingCustomerTaxId}</p></div>
            <div className="space-y-2"><Label>{t("admin.fields.name")}</Label><Input autoFocus value={customerName} onChange={(event) => setCustomerName(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter" && customerName.trim()) { event.preventDefault(); createMissingCustomer(); } }} /></div>
            <div className="space-y-2"><Label>{t("admin.fields.email")}</Label><Input value={customerEmail} onChange={(event) => setCustomerEmail(event.target.value)} /></div>
            <div className="space-y-2"><Label>{t("admin.fields.phone")}</Label><Input value={customerPhone} onChange={(event) => setCustomerPhone(event.target.value)} /></div>
            <div className="space-y-2"><Label>{t("admin.fields.address")}</Label><Input value={customerAddress} onChange={(event) => setCustomerAddress(event.target.value)} placeholder="CIUDAD" /></div>
            <div className="flex flex-wrap justify-end gap-2 md:col-span-2"><Button type="button" variant="outline" onClick={() => setMissingCustomerTaxId("")}>{t("sales.customer.changeTaxId")}</Button><Button type="button" disabled={!customerName.trim() || createCustomerMutation.isPending || !canCreateCustomers} onClick={createMissingCustomer}>{t("sales.customer.createAndContinue")}</Button></div>
          </div>}
        </div>
      </FormDialog>

      <FormDialog open={externalProduct != null} onOpenChange={(open) => { if (!open) setExternalProduct(null); }} title={t("sales.externalProduct.title")} description={t("sales.externalProduct.description")}>
        <div className="space-y-4">
          <div className="rounded-2xl border bg-muted/35 p-3 text-sm">
            <p className="font-bold">{externalProduct?.name}</p>
            {externalProduct?.brand ? <p className="mt-1 text-xs text-muted-foreground">{externalProduct.brand}</p> : null}
            {externalProduct?.quantityText ? <p className="text-xs text-muted-foreground">{externalProduct.quantityText}</p> : null}
            <p className="mt-1 text-[0.68rem] uppercase tracking-[0.12em] text-muted-foreground">{t("sales.externalProduct.source")}</p>
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            <div className="space-y-2"><Label>{t("admin.fields.name")}</Label><Input autoFocus value={externalName} onChange={(event) => setExternalName(event.target.value)} /></div>
            <div className="space-y-2"><Label>{t("sales.externalProduct.salePrice")}</Label><Input inputMode="decimal" value={externalSalePrice} onChange={(event) => setExternalSalePrice(event.target.value)} placeholder="0.00" /></div>
            <div className="space-y-2 md:col-span-2">
              <Label>{t("sales.externalProduct.baseUnit")}</Label>
              <select className="w-full rounded-xl border bg-background px-3 py-2 text-sm outline-none" value={externalUnitCode} onChange={(event) => setExternalUnitCode(event.target.value)}>
                {(canViewUnits ? unitsQuery.data ?? [] : []).map((unit) => (
                  <option key={unit.code} value={unit.code}>{unit.name}</option>
                ))}
                {!canViewUnits ? <option value="unit">Unit</option> : null}
              </select>
            </div>
          </div>
          <div className="flex flex-wrap justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => setExternalProduct(null)}>{t("admin.actions.cancel")}</Button>
            <Button type="button" disabled={externalProductMutation.isPending} onClick={() => externalProductMutation.mutate()}>{t("sales.externalProduct.registerAndAdd")}</Button>
          </div>
        </div>
      </FormDialog>

      <FormDialog open={isProductSearchOpen} onOpenChange={setIsProductSearchOpen} title={t("sales.productSearch.title")} description={t("sales.productSearch.description")}>
        <div className="space-y-4">
          <Input autoFocus value={presentationSearch} onChange={(event) => setPresentationSearch(event.target.value)} placeholder={t("sales.placeholders.searchPresentation")} />
          <div className="grid max-h-[50vh] gap-2 overflow-y-auto pr-1">
            {filteredPresentations.length > 0 ? <p className="text-xs font-black uppercase tracking-[0.12em] text-muted-foreground">{t("sales.sections.items")}</p> : null}
            {filteredPresentations.map((presentation) => {
              const product = productById.get(presentation.productId);
              return (
                <button key={presentation.id} type="button" className="rounded-2xl border bg-background/75 p-4 text-left transition hover:border-primary hover:bg-primary/5" onClick={() => { setSelectedPresentationId(presentation.id); appendPresentation(presentation, presentation.barcode ?? ""); setSelectedPresentationId(""); setPresentationSearch(""); setIsProductSearchOpen(false); focusBarcodeInput(); }}>
                  <p className="font-bold">{product?.name ?? presentation.productId.slice(0, 8)}</p>
                  <p className="mt-1 text-sm text-muted-foreground">{presentation.name} · {formatMoney(presentation.salePrice)}</p>
                  <p className="mt-1 font-mono text-xs text-muted-foreground">{presentation.barcode ?? "-"}</p>
                </button>
              );
            })}
            {filteredCombos.length > 0 ? <p className="mt-2 text-xs font-black uppercase tracking-[0.12em] text-muted-foreground">{t("sales.sections.combos")}</p> : null}
            {filteredCombos.map((combo) => (
              <button key={combo.id} type="button" className="rounded-2xl border border-primary/30 bg-primary/5 p-4 text-left transition hover:border-primary hover:bg-primary/10" onClick={() => { appendCombo(combo); setPresentationSearch(""); setIsProductSearchOpen(false); focusBarcodeInput(); }}>
                <p className="font-bold">{combo.name}</p>
                <p className="mt-1 text-sm text-muted-foreground">{combo.code} · {formatMoney(combo.comboPrice)}</p>
                <p className="mt-1 font-mono text-xs text-muted-foreground">{combo.barcode ?? "-"}</p>
              </button>
            ))}
            {filteredPresentations.length === 0 && filteredCombos.length === 0 ? <div className="rounded-2xl border bg-muted/35 p-4 text-sm text-muted-foreground">{t("admin.states.empty")}</div> : null}
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => setIsProductSearchOpen(false)}>{t("admin.actions.cancel")}</Button>
            <Button type="button" disabled={!selectedPresentationId} onClick={addSelectedPresentation}><PackagePlus className="mr-2 h-4 w-4" aria-hidden="true" />{t("sales.actions.addLine")}</Button>
          </div>
        </div>
      </FormDialog>
    </form>
  );
}

function invalidateSaleImpact(queryClient: ReturnType<typeof useQueryClient>, saleId: string) {
  queryClient.invalidateQueries({ queryKey: ["sales"] });
  queryClient.invalidateQueries({ queryKey: ["sales", "detail", saleId] });
  queryClient.invalidateQueries({ queryKey: ["inventory", "balances"] });
  queryClient.invalidateQueries({ queryKey: ["inventory", "layers"] });
  queryClient.invalidateQueries({ queryKey: ["inventory", "movements"] });
  queryClient.invalidateQueries({ queryKey: ["inventory", "kardex"] });
  queryClient.invalidateQueries({ queryKey: ["inventory", "summary"] });
  queryClient.invalidateQueries({ queryKey: ["combos"] });
}

function focusBarcodeInput() {
  window.setTimeout(() => document.getElementById("sale-barcode-input")?.focus(), 0);
}

function findCustomerByTaxId(customers: Customer[], taxId: string) {
  const normalizedTaxId = normalizeTaxId(taxId);
  return customers.find((customer) => normalizeTaxId(customer.taxId ?? "") === normalizedTaxId) ?? null;
}

function normalizeTaxId(value: string) {
  return value.trim().toUpperCase();
}

function emptyToNull(value: string) {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function SelectField({ label, registration, options, error, className }: Readonly<{ label: string; registration: UseFormRegisterReturn; options: Array<{ value: string; label: string }>; error?: string; className?: string }>) {
  return <div className={`space-y-2 ${className ?? ""}`}><Label>{label}</Label><select className="h-11 w-full rounded-xl border bg-background/75 px-3 text-sm" {...registration}><option value="">-</option>{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select>{error ? <p className="text-xs text-destructive">{error}</p> : null}</div>;
}

function FormField({ label, registration, type = "text", placeholder, error, className }: Readonly<{ label: string; registration: UseFormRegisterReturn; type?: string; placeholder?: string; error?: string; className?: string }>) {
  return <div className={`space-y-2 ${className ?? ""}`}><Label>{label}</Label><Input type={type} step="0.000001" placeholder={placeholder} {...registration} />{error ? <p className="text-xs text-destructive">{error}</p> : null}</div>;
}

function QuickNumberField({ label, value, onValueChange, type = "text" }: Readonly<{ label: string; value: number; onValueChange: (value: number) => void; type?: string }>) {
  return <div className="space-y-2"><Label>{label}</Label><Input type={type} step="0.000001" value={value} onChange={(event) => onValueChange(Number(event.target.value))} /></div>;
}

function SummaryRow({ label, value, strong }: Readonly<{ label: string; value: string; strong?: boolean }>) {
  return <div className={`mt-3 flex items-center justify-between gap-3 ${strong ? "text-lg font-black" : "text-sm"}`}><span className="text-muted-foreground">{label}</span><span>{value}</span></div>;
}

function calculateDraftTotals(details: SaleCreateInput["details"], comboLines: SaleCreateInput["comboLines"], globalDiscount: number, comboById: Map<string, ComboOffer>) {
  const subtotal = (details ?? []).reduce((total, detail) => total + Number(detail.quantityPresentation || 0) * Number(detail.unitPrice || 0), 0);
  const combosSubtotal = (comboLines ?? []).reduce((total, line) => {
    const combo = comboById.get(line.comboOfferId);
    return total + (combo ? combo.comboPrice * Number(line.quantity || 0) : 0);
  }, 0);
  const lineDiscounts = (details ?? []).reduce((total, detail) => total + Number(detail.discountAmount || 0), 0);
  const taxAmount = (details ?? []).reduce((total, detail) => total + Number(detail.taxAmount || 0), 0);
  return { subtotal: subtotal + combosSubtotal, lineDiscounts, taxAmount, totalAmount: Math.max(subtotal + combosSubtotal + taxAmount - lineDiscounts - Number(globalDiscount || 0), 0) };
}

function lineTotal(detail: NonNullable<SaleCreateInput["details"]>[number] | undefined) {
  return Math.max(Number(detail?.quantityPresentation || 0) * Number(detail?.unitPrice || 0) + Number(detail?.taxAmount || 0) - Number(detail?.discountAmount || 0), 0);
}

function calculateAvailableCombos(combo: ComboOffer, stockByProductId: Map<string, number>) {
  const requiredBaseByProductId = new Map<string, number>();

  for (const item of combo.items) {
    const requiredBase = Number(item.quantityBase || 0);
    if (requiredBase <= 0) return null;
    requiredBaseByProductId.set(item.productId, (requiredBaseByProductId.get(item.productId) ?? 0) + requiredBase);
  }

  if (requiredBaseByProductId.size === 0) return null;

  let availableCombos = Number.POSITIVE_INFINITY;
  for (const [productId, requiredBase] of requiredBaseByProductId) {
    const availableBase = stockByProductId.get(productId);
    if (availableBase === undefined) return null;
    availableCombos = Math.min(availableCombos, Math.floor((availableBase / requiredBase) + 1e-9));
  }

  return Math.max(availableCombos, 0);
}

function formatComboBaseQuantity(combo: ComboOffer, comboQuantity: number, productById: ReadonlyMap<string, { baseUnitCode: string }>) {
  const quantityByProductId = new Map<string, { fallbackName: string; quantityBase: number }>();

  for (const item of combo.items) {
    const current = quantityByProductId.get(item.productId);
    quantityByProductId.set(item.productId, {
      fallbackName: item.productName,
      quantityBase: (current?.quantityBase ?? 0) + Number(item.quantityBase || 0) * comboQuantity,
    });
  }

  return Array.from(quantityByProductId, ([productId, value]) => {
    const product = productById.get(productId);
    return `${formatNumber(value.quantityBase)} ${product?.baseUnitCode ?? value.fallbackName}`;
  }).join(" + ") || "-";
}

function lineError(errors: FieldErrors<SaleCreateInput>, index: number, key: "quantityPresentation" | "unitPrice") {
  const message = errors.details?.[index]?.[key]?.message;
  return typeof message === "string" ? message : undefined;
}

function firstErrorMessage(t: (key: string, values?: Record<string, string>) => string, ...errors: unknown[]) {
  const error = errors.find(Boolean);
  return formatErrorMessage(error, t);
}

function formatErrorMessage(error: unknown, t: (key: string, values?: Record<string, string>) => string) {
  if (error && typeof error === "object" && "errorCode" in error && error.errorCode === "INSUFFICIENT_STOCK") {
    return t("errors.insufficientStock");
  }

  return formatPermissionError(error, t) ?? (error instanceof Error ? error.message : t("admin.states.error"));
}
