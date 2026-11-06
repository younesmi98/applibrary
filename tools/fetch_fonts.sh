#!/usr/bin/env bash
# Downloads the official Vazirmatn and Inter releases and copies the four weights NEXUS uses
# (400, 500, 600, 700) plus their SIL OFL licence texts into app/src/main/assets/fonts.
# Needs network and the GitHub CLI (`gh`, preinstalled on GitHub Actions runners).
set -euo pipefail
OUT="$(cd "$(dirname "$0")/.." && pwd)/app/src/main/assets/fonts"
TMP="$(mktemp -d)"
mkdir -p "$OUT" "$TMP/vazir" "$TMP/inter"

gh release download v33.003 --repo rastikerdar/vazirmatn --pattern '*.zip' --dir "$TMP/vazir"
gh release download --repo rsms/inter --pattern 'Inter-*.zip' --dir "$TMP/inter"
for z in "$TMP"/vazir/*.zip; do unzip -q -o "$z" -d "$TMP/vazir/x"; done
for z in "$TMP"/inter/*.zip; do unzip -q -o "$z" -d "$TMP/inter/x"; done

copy() { # root name
  f="$(find "$1" -type f -name "$2" ! -path '*Round-Dots*' | head -n 1)"
  [ -n "$f" ] || { echo "MISSING $2 in release package" >&2; exit 1; }
  cp "$f" "$OUT/$2"; echo "ok $2"
}
for w in Regular Medium SemiBold Bold; do
  copy "$TMP/vazir/x" "Vazirmatn-$w.ttf"
  copy "$TMP/inter/x" "Inter-$w.ttf"
done
cp "$(find "$TMP/vazir/x" -iname 'OFL.txt' | head -n 1)" "$OUT/OFL-Vazirmatn.txt"
cp "$(find "$TMP/inter/x" \( -iname 'LICENSE.txt' -o -iname 'OFL.txt' \) | head -n 1)" "$OUT/OFL-Inter.txt"
ls -la "$OUT"
