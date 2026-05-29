#!/bin/bash
# Release keystore SHA-1 (Firebase / Google Sign-In için)
set -euo pipefail

KEYSTORE="${1:-$(dirname "$0")/../Untitled.jks}"
ALIAS="${2:-bustracker}"

JAVA_BIN="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool"
if [[ ! -x "$JAVA_BIN" ]]; then
  echo "Android Studio JBR keytool bulunamadı."
  exit 1
fi

echo "Keystore: $KEYSTORE"
echo "Alias: $ALIAS"
echo "(Şifre sorulacak)"
echo ""

"$JAVA_BIN" -list -v -keystore "$KEYSTORE" -alias "$ALIAS" | grep -E "SHA1:|SHA256:"
