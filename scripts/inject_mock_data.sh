#!/usr/bin/env bash

# Check if jq is installed
if ! command -v jq &> /dev/null
then
    echo "jq could not be found. Please install jq to run this script (e.g. brew install jq)"
    exit 1
fi

MOCK_FILE="mock_transactions.json"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JSON_PATH="${DIR}/${MOCK_FILE}"

if [ ! -f "$JSON_PATH" ]; then
    echo "Missing $MOCK_FILE in $DIR"
    exit 1
fi

echo "Injecting EthioStat mock transactions via ADB..."

# Parse JSON array and iterate over each object
jq -c '.[]' "$JSON_PATH" | while read i; do
    bank=$(echo "$i" | jq -r '.bank')
    lang=$(echo "$i" | jq -r '.language')
    type=$(echo "$i" | jq -r '.type')
    body=$(echo "$i" | jq -r '.body')

    echo "Sending [$bank - $lang - $type]..."
    safe_body="${body//\'/\\\'}"
    
    # Redirect stdin to avoid adb consuming the while-loop input stream
    adb shell "am broadcast -a com.ethiostat.app.DEBUG_SMS -n com.ethiostat.app/.receiver.SmsReceiver --es sender \"$bank\" --es body '${safe_body}'" </dev/null >/dev/null

    # small pause to allow the background receiver and Flow collect to process cleanly
    sleep 1.0
done

echo "Done! Check your EthioStat Transaction History screen."
