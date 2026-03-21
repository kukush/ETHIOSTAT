#!/usr/bin/env bash
# inject_mock_data.sh
# Injects mock financial SMS messages into a connected Android device via ADB.
# Uses the sender short codes defined in AccountSourceType.
#
# Usage:
#   ./inject_mock_data.sh            # default 0.8s delay between messages
#   ./inject_mock_data.sh --delay 1  # custom delay in seconds

set -euo pipefail

# ── Argument parsing ──────────────────────────────────────────────────────────
DELAY=0.8
while [[ $# -gt 0 ]]; do
    case "$1" in
        --delay) DELAY="$2"; shift 2 ;;
        *) echo "Unknown argument: $1"; exit 1 ;;
    esac
done

# ── Prerequisites ─────────────────────────────────────────────────────────────
if ! command -v jq &> /dev/null; then
    echo "ERROR: jq is not installed. Run: brew install jq"
    exit 1
fi

if ! command -v adb &> /dev/null; then
    echo "ERROR: adb is not installed or not in PATH."
    exit 1
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JSON_PATH="${DIR}/mock_transactions.json"

if [ ! -f "$JSON_PATH" ]; then
    echo "ERROR: mock_transactions.json not found in $DIR"
    exit 1
fi

# ── Injection loop ────────────────────────────────────────────────────────────
echo "══════════════════════════════════════════════════════"
echo " EthioStat – Mock SMS Injector"
echo " Delay: ${DELAY}s between messages"
echo "══════════════════════════════════════════════════════"

total=$(jq 'length' "$JSON_PATH")
sent=0
failed=0


while IFS= read -r item; do
    sender=$(echo "$item" | jq -r '.sender // empty')
    lang=$(echo "$item"   | jq -r '.language // "unknown"')
    type=$(echo "$item"   | jq -r '.type // "unknown"')
    body=$(echo "$item"   | jq -r '.body // empty')

    if [[ -z "$sender" || -z "$body" ]]; then
        echo "  [SKIP] Missing sender or body in entry"
        continue
    fi

    # Escape quotes in body for shell safety
    safe_body="${body//\"/\\\"}"

    printf "  → [%s | %s | %s]\n" "$sender" "$lang" "$type"

    if adb shell "am broadcast \
        -a com.ethiostat.app.DEBUG_SMS \
        -n com.ethiostat.app/.receiver.SmsReceiver \
        --es sender \"$sender\" \
        --es body \"$safe_body\"" \
        </dev/null >/dev/null 2>&1; then
        sent=$((sent + 1))
    else
        echo "    ✗ Failed to send (is device connected and app installed?)"
        failed=$((failed + 1))
    fi

    sleep "$DELAY"
done < <(jq -c '.[]' "$JSON_PATH")

echo ""
echo "══════════════════════════════════════════════════════"
echo " Done!  Total: $total  |  Sent: $sent  |  Failed: $failed"
echo " Check the EthioStat Transaction History screen."
echo "══════════════════════════════════════════════════════"
