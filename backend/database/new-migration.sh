#!/usr/bin/env bash
# ==============================================================================
# Vivent - Collaborative Flyway Migration & Seed Generator
#
# Usage:
#   ./database/new-migration.sh migration "add_customer_credit_limit"
#   ./database/new-migration.sh seed "default_discounts"
#
# Formats generated:
#   migrations/V<YYYYMMDDHHMMSS>__<description>.sql
#   seed/S<YYYYMMDDHHMMSS>__<description>.sql
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TYPE="${1:-migration}"
RAW_NAME="${2:-}"

if [[ -z "$RAW_NAME" ]]; then
  echo "Error: Migration description is required."
  echo "Usage:"
  echo "  $0 migration \"description_in_english\""
  echo "  $0 seed \"description_in_english\""
  exit 1
fi

# Generate 14-digit UTC timestamp (YYYYMMDDHHMMSS)
TIMESTAMP="$(date -u +%Y%m%d%H%M%S)"

# Convert description to clean snake_case: lowercase, replace spaces/dashes with underscores
CLEAN_NAME="$(echo "$RAW_NAME" | tr '[:upper:]' '[:lower:]' | tr ' -' '__' | tr -cd 'a-z0-9_')"
# Deduplicate repeated underscores
CLEAN_NAME="$(echo "$CLEAN_NAME" | sed 's/_\+/_/g')"

case "$TYPE" in
  migration|m)
    TARGET_DIR="${SCRIPT_DIR}/migrations"
    PREFIX="V"
    ;;
  seed|s)
    TARGET_DIR="${SCRIPT_DIR}/seed"
    PREFIX="S"
    ;;
  *)
    echo "Error: Unknown type '$TYPE'. Use 'migration' or 'seed'."
    exit 1
    ;;
esac

mkdir -p "$TARGET_DIR"
FILENAME="${PREFIX}${TIMESTAMP}__${CLEAN_NAME}.sql"
FILEPATH="${TARGET_DIR}/${FILENAME}"

if [[ -f "$FILEPATH" ]]; then
  echo "Warning: File already exists: $FILEPATH"
  exit 0
fi

touch "$FILEPATH"
chmod 644 "$FILEPATH"

echo "Created new Flyway $(echo "$TYPE" | tr '[:lower:]' '[:upper:]'):"
echo "  $FILEPATH"
