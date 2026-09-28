#!/usr/bin/env bash
# Lädt die freien Schriften (SIL Open Font License) nach app/src/main/assets/fonts.
# Bereits vorhandene Dateien werden übersprungen.
set -euo pipefail

dir="$(cd "$(dirname "$0")/.." && pwd)/app/src/main/assets/fonts"
base="https://github.com/google/fonts/raw/main/ofl"
mkdir -p "$dir"

fetch() {
  if [ ! -s "$dir/$2" ]; then
    echo "Lade $2"
    curl -fsSL "$base/$1" -o "$dir/$2"
  fi
}

fetch rye/Rye-Regular.ttf Rye-Regular.ttf
fetch rye/OFL.txt Rye-OFL.txt
fetch imfellenglish/IMFeENrm28P.ttf IMFellEnglish-Regular.ttf
fetch imfellenglish/IMFeENit28P.ttf IMFellEnglish-Italic.ttf
fetch imfellenglish/OFL.txt IMFellEnglish-OFL.txt
