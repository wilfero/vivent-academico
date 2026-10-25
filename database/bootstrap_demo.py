"""
Vivent académico — bootstrap de datos operativos de demostración.

Crea operaciones ficticias suficientes para demostrar los tres procesos
integrados, el dashboard y los nueve reportes, TODO a través de la API real,
de modo que FIFO, kardex, inventario y auditoría quedan correctos por
construcción (sin SQL manual).

Uso (después de levantar el stack local):
    python database/bootstrap_demo.py
    python database/bootstrap_demo.py --base-url http://localhost:8080

Requisitos previos:
    - Stack local arriba (db + backend), migraciones y seeds aplicadas.
    - Cuentas demo (ChangeMe123!): admin, seller, inventory, transfers_origin.

El script es idempotente: consulta cada operación por su número de negocio
(ACAD-*) y omite la que ya exista.
"""

import argparse
import datetime
import json
import sys
import urllib.error
import urllib.request

sys.stdout.reconfigure(encoding="utf-8")

TODAY_ISO = datetime.date.today().isoformat()
NOW_ISO = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


class Api:
    def __init__(self, base_url):
        self.base_url = base_url.rstrip("/")

    def _call(self, method, path, token=None, body=None, ok_codes=(200,)):
        data = json.dumps(body).encode("utf-8") if body is not None else None
        request = urllib.request.Request(
            f"{self.base_url}{path}",
            data=data,
            method=method,
            headers={"Content-Type": "application/json"},
        )
        if token:
            request.add_header("Authorization", f"Bearer {token}")

        try:
            with urllib.request.urlopen(request) as response:
                payload = json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as error:
            try:
                payload = json.loads(error.read().decode("utf-8"))
            except Exception:
                payload = {"success": False, "message": f"HTTP {error.code}"}
            return payload, error.code

        if response.status in ok_codes:
            return payload, response.status
        return payload, response.status

    def login(self, username, password="ChangeMe123!", tenant="demo"):
        payload, status = self._call(
            "POST",
            "/api/v1/auth/login",
            body={"tenantSlug": tenant, "username": username, "password": password},
        )
        if not payload.get("success"):
            raise SystemExit(f"[bootstrap] login failed for {username}: {status} {payload.get('message')}")
        print(f"[bootstrap] login ok: {username}")
        return payload["data"]

    def get(self, path, token):
        payload, _ = self._call("GET", path, token=token)
        return payload

    def post(self, path, token, body=None):
        payload, _ = self._call("POST", path, token=token, body=body)
        return payload


def page_items(payload):
    if isinstance(payload, dict) and isinstance(payload.get("items"), list):
        return payload["items"]
    if isinstance(payload, list):
        return payload
    return []


def find_by_number(api, token, list_path, number):
    result = api.get(f"{list_path}?query={number}", token)
    for item in page_items(result.get("data") if isinstance(result.get("data"), dict) else result.get("data")):
        item_number = item.get("purchaseNumber") or item.get("saleNumber") or item.get("transferNumber") or item.get("adjustmentNumber")
        if item_number == number:
            return item
    return None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://localhost:8080")
    args = parser.parse_args()

    api = Api(args.base_url)
    print(f"[bootstrap] target: {args.base_url}")

    admin = api.login("admin")
    seller = api.login("seller")
    inventory = api.login("inventory")
    manager = api.login("transfers_origin")

    admin_token = admin["accessToken"]
    seller_token = seller["accessToken"]
    inventory_token = inventory["accessToken"]
    manager_token = manager["accessToken"]

    main_branch_id = admin["branchId"]

    # ---------- catálogo base (lookups) ----------
    supplier_result = api.get("/api/v1/suppliers", inventory_token)
    supplier = next((s for s in page_items(supplier_result.get("data")) if s.get("name") == "Distribuidora Demo"), None)
    if supplier is None:
        raise SystemExit("[bootstrap] supplier 'Distribuidora Demo' not found (seed S008 required)")

    barcodes = {
        "lata": "740000000001",
        "six": "740000000006",
        "caja24": "740000000024",
        "agua": "740000000101",
        "pack12": "740000000112",
        "papas": "740000000201",
        "cafe": "740000000301",
        "libreta": "740000000501",
    }
    presentations = {}
    for key, code in barcodes.items():
        lookup = api.get(f"/api/v1/products/by-barcode/{code}", admin_token)
        data = lookup.get("data")
        if data and data.get("presentation"):
            presentations[key] = data["presentation"]
        else:
            print(f"[bootstrap] warn: barcode {code} not found, skipping")

    def pres(key):
        p = presentations.get(key)
        if p is None:
            raise SystemExit(f"[bootstrap] missing presentation '{key}'")
        return p

    # ---------- proceso 1: compra -> FIFO -> kardex ----------
    purchase_number = "ACAD-PUR-001"
    purchase = find_by_number(api, admin_token, "/api/v1/purchases", purchase_number)
    if purchase is None:
        payload = api.post(
            "/api/v1/purchases",
            inventory_token,
            body={
                "supplierId": supplier["id"],
                "purchaseNumber": purchase_number,
                "purchaseDate": TODAY_ISO,
                "paymentMethod": "TRANSFER",
                "paymentType": "CASH",
                "status": "DRAFT",
                "details": [
                    {"presentationId": pres("lata")["id"], "quantityPresentation": 24, "unitCostPresentation": 5.00},
                    {"presentationId": pres("six")["id"], "quantityPresentation": 6, "unitCostPresentation": 28.00},
                    {"presentationId": pres("pack12")["id"], "quantityPresentation": 8, "unitCostPresentation": 27.00},
                    {"presentationId": pres("libreta")["id"], "quantityPresentation": 20, "unitCostPresentation": 6.50},
                ],
            },
        )
        if not payload.get("success"):
            raise SystemExit(f"[bootstrap] purchase create failed: {payload.get('message')}")
        purchase = payload["data"]
        confirm = api.post(f"/api/v1/purchases/{purchase['id']}/confirm", manager_token)
        if not confirm.get("success"):
            raise SystemExit(f"[bootstrap] purchase confirm failed: {confirm.get('message')}")
        print(f"[bootstrap] purchase {purchase_number} created + CONFIRMED (FIFO + kardex)")
    else:
        print(f"[bootstrap] purchase {purchase_number} already exists")

    # ---------- segunda compra: surte cafe, papas y agua para los demas procesos ----------
    purchase_number_2 = "ACAD-PUR-002"
    purchase2 = find_by_number(api, admin_token, "/api/v1/purchases", purchase_number_2)
    if purchase2 is None:
        payload = api.post(
            "/api/v1/purchases",
            inventory_token,
            body={
                "supplierId": supplier["id"],
                "purchaseNumber": purchase_number_2,
                "purchaseDate": TODAY_ISO,
                "paymentMethod": "CASH",
                "paymentType": "CREDIT",
                "status": "DRAFT",
                "details": [
                    {"presentationId": pres("cafe")["id"], "quantityPresentation": 10, "unitCostPresentation": 22.00},
                    {"presentationId": pres("papas")["id"], "quantityPresentation": 12, "unitCostPresentation": 6.00},
                    {"presentationId": pres("agua")["id"], "quantityPresentation": 24, "unitCostPresentation": 4.00},
                ],
            },
        )
        if not payload.get("success"):
            raise SystemExit(f"[bootstrap] purchase create failed: {payload.get('message')}")
        purchase2 = payload["data"]
        confirm = api.post(f"/api/v1/purchases/{purchase2['id']}/confirm", inventory_token)
        if not confirm.get("success"):
            raise SystemExit(f"[bootstrap] purchase confirm failed: {confirm.get('message')}")
        print(f"[bootstrap] purchase {purchase_number_2} created + CONFIRMED (FIFO + kardex)")
    else:
        print(f"[bootstrap] purchase {purchase_number_2} already exists")

    # ---------- proceso 2: venta POS -> FIFO -> cierre ----------
    sale_number = "ACAD-SAL-001"
    sale = find_by_number(api, admin_token, "/api/v1/sales", sale_number)
    if sale is None:
        payload = api.post(
            "/api/v1/sales",
            seller_token,
            body={
                "saleNumber": sale_number,
                "saleDate": NOW_ISO,
                "paymentMethod": "CASH",
                "status": "DRAFT",
                "details": [
                    {"presentationId": pres("lata")["id"], "quantityPresentation": 3, "unitPrice": 10.00, "discountAmount": 0, "taxAmount": 0},
                    {"presentationId": pres("six")["id"], "quantityPresentation": 1, "unitPrice": 55.00, "discountAmount": 0, "taxAmount": 0},
                    {"presentationId": pres("agua")["id"], "quantityPresentation": 4, "unitPrice": 5.00, "discountAmount": 0, "taxAmount": 0},
                ],
            },
        )
        if not payload.get("success"):
            raise SystemExit(f"[bootstrap] sale create failed: {payload.get('message')}")
        sale = payload["data"]
        # Confirming sales requires sales.create, which SELLER owns (WAREHOUSE_USER does not).
        confirm = api.post(f"/api/v1/sales/{sale['id']}/confirm", seller_token)
        if not confirm.get("success"):
            raise SystemExit(f"[bootstrap] sale confirm failed: {confirm.get('message')}")
        print(f"[bootstrap] sale {sale_number} created + CONFIRMED (FIFO + kardex)")
    else:
        print(f"[bootstrap] sale {sale_number} already exists")
        if sale.get("status") == "DRAFT":
            # Heals a draft left behind by an interrupted run.
            confirm = api.post(f"/api/v1/sales/{sale['id']}/confirm", seller_token)
            if confirm.get("success"):
                print(f"[bootstrap] sale {sale_number} draft confirmed")
            else:
                print(f"[bootstrap] warn: sale {sale_number} confirm failed: {confirm.get('message')}")

    # venta con descuento para el reporte de descuentos
    sale_number_2 = "ACAD-SAL-002"
    sale2 = find_by_number(api, admin_token, "/api/v1/sales", sale_number_2)
    if sale2 is None:
        payload = api.post(
            "/api/v1/sales",
            # A sale-level discount needs sales.apply_discount, which SELLER does not
            # have; COMPANY_ADMIN does, so this discounted sale is created as admin.
            admin_token,
            body={
                "saleNumber": sale_number_2,
                "saleDate": NOW_ISO,
                "paymentMethod": "CARD",
                "status": "DRAFT",
                "discountAmount": 5.00,
                "details": [
                    {"presentationId": pres("papas")["id"], "quantityPresentation": 2, "unitPrice": 9.00, "discountAmount": 0, "taxAmount": 0},
                    {"presentationId": pres("libreta")["id"], "quantityPresentation": 5, "unitPrice": 12.00, "discountAmount": 5.00, "taxAmount": 0},
                ],
            },
        )
        if payload.get("success"):
            sale2 = payload["data"]
            confirm = api.post(f"/api/v1/sales/{sale2['id']}/confirm", seller_token)
            if confirm.get("success"):
                print(f"[bootstrap] sale {sale_number_2} created + CONFIRMED (discount + card)")
            else:
                print(f"[bootstrap] warn: sale {sale_number_2} confirm failed: {confirm.get('message')}")
        else:
            print(f"[bootstrap] warn: sale {sale_number_2} create failed: {payload.get('message')}")
    else:
        print(f"[bootstrap] sale {sale_number_2} already exists")

    # ---------- proceso 3: traslado entre sucursales ----------
    transfer_number = "ACAD-TRF-001"
    transfer = find_by_number(api, admin_token, "/api/v1/transfers", transfer_number)
    if transfer is None:
        payload = api.post(
            "/api/v1/transfers",
            admin_token,
            body={
                "transferNumber": transfer_number,
                "sourceBranchId": main_branch_id,
                "destinationBranchId": find_branch2_id(api, admin_token),
                "notes": "Demo academica: traslado a Demo Branch 2",
                "details": [
                    {"presentationId": pres("agua")["id"], "quantityRequestedPresentation": 6},
                    {"presentationId": pres("cafe")["id"], "quantityRequestedPresentation": 2},
                ],
            },
        )
        if not payload.get("success"):
            raise SystemExit(f"[bootstrap] transfer create failed: {payload.get('message')}")
        transfer = payload["data"]
        print(f"[bootstrap] transfer {transfer_number} REQUESTED")
    else:
        print(f"[bootstrap] transfer {transfer_number} already exists")

    # The state machine is advanced step by step so a rerun heals a transfer that
    # stopped halfway (REQUESTED -> APPROVED -> IN_TRANSIT -> RECEIVED).
    # approve needs transfers.approve (COMPANY_ADMIN); dispatch needs
    # transfers.dispatch (WAREHOUSE_USER, the transfers_origin account).
    transfer = api.get(f"/api/v1/transfers/{transfer['id']}", admin_token)["data"]
    for expected_status, action, token in (
        ("REQUESTED", "approve", admin_token),
        ("APPROVED", "dispatch", manager_token),
    ):
        if (transfer.get("status") or "").upper() != expected_status:
            continue
        step = api.post(f"/api/v1/transfers/{transfer['id']}/{action}", token)
        if not step.get("success"):
            print(f"[bootstrap] warn: transfer {action} failed: {step.get('message')}")
            break
        print(f"[bootstrap] transfer {transfer_number} " + {"approve": "APPROVED", "dispatch": "DISPATCHED"}[action])
        transfer = api.get(f"/api/v1/transfers/{transfer['id']}", admin_token)["data"]

    if (transfer.get("status") or "").upper() == "IN_TRANSIT":
        receive = api.post(
            f"/api/v1/transfers/{transfer['id']}/receive",
            admin_token,
            body={"details": [
                {
                    "detailId": d["id"],
                    "quantityReceivedBase": d.get("quantityApprovedBase") or d.get("quantityRequestedBase"),
                }
                for d in transfer.get("details", [])
            ]},
        )
        if receive.get("success"):
            print(f"[bootstrap] transfer {transfer_number} RECEIVED (kardex en ambas sucursales)")
        else:
            print(f"[bootstrap] warn: transfer receive failed: {receive.get('message')}")
    elif (transfer.get("status") or "").upper() == "RECEIVED":
        print(f"[bootstrap] transfer {transfer_number} already RECEIVED")

    # ---------- ajuste con aprobación ----------
    adjustment_number = "ACAD-ADJ-001"
    adjustment = find_by_number(api, admin_token, "/api/v1/inventory/adjustments", adjustment_number)
    if adjustment is None:
        payload = api.post(
            "/api/v1/inventory/adjustments",
            inventory_token,
            body={
                "branchId": main_branch_id,
                "locationId": None,
                "adjustmentNumber": adjustment_number,
                "adjustmentType": "DAMAGE",
                "direction": "OUT",
                "status": "PENDING_APPROVAL",
                "notes": "Demo academica: latas dañadas",
                "lines": [
                    {"productId": None, "presentationId": pres("lata")["id"], "quantityPresentation": 2, "quantityBase": None, "unitCost": 5.00, "notes": None},
                ],
            },
        )
        if payload.get("success"):
            adjustment = payload["data"]
            # inventory.adjust_approve belongs to COMPANY_ADMIN, not to WAREHOUSE_USER.
            approve = api.post(f"/api/v1/inventory/adjustments/{adjustment['id']}/approve", admin_token)
            if approve.get("success"):
                print(f"[bootstrap] adjustment {adjustment_number} APPROVED (kardex DAMAGE_OUT)")
            else:
                print(f"[bootstrap] warn: adjustment approve failed: {approve.get('message')}")
        else:
            print(f"[bootstrap] warn: adjustment create failed: {payload.get('message')}")
    else:
        print(f"[bootstrap] adjustment {adjustment_number} already exists")

    # ---------- cierre de caja del vendedor ----------
    closures = api.get("/api/v1/cash-closures", seller_token)
    today = TODAY_ISO
    existing = next(
        (c for c in page_items(closures.get("data")) if (c.get("closureDate") or "")[:10] == today),
        None,
    )
    if existing is None:
        preview = api.get(f"/api/v1/cash-closures/preview?branchId={main_branch_id}&closureDate={today}", seller_token)
        counted = (preview.get("data") or {}).get("expectedTotal") or 0
        payload = api.post(
            "/api/v1/cash-closures",
            seller_token,
            body={"branchId": main_branch_id, "closureDate": today, "countedTotal": counted},
        )
        if payload.get("success"):
            print(f"[bootstrap] closure for {today} created (expected cash Q{counted})")
        else:
            print(f"[bootstrap] warn: closure create failed: {payload.get('message')}")
    else:
        print(f"[bootstrap] closure for {today} already exists")

    print("[bootstrap] done — dashboard, procesos y reportes listos para demostrar.")


def find_branch2_id(api, admin_token):
    branches = api.get("/api/v1/branches", admin_token)
    for branch in page_items(branches.get("data")):
        if branch.get("code") == "BRANCH2":
            return branch["id"]
    raise SystemExit("[bootstrap] Demo Branch 2 (BRANCH2) not found; run seed S008")


if __name__ == "__main__":
    main()
