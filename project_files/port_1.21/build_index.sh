#!/usr/bin/env bash
# Builds the class indexes used by mapimports.py / rewrite.py (Phase 1 of MIGRATION_1.21.md).
# Run from the repository root after `./gradlew createMinecraftArtifacts`. Must run against the 1.20.1 sources.
# Usage: project_files/port_1.21/build_index.sh <output dir>
set -euo pipefail
OUT=$1; mkdir -p "$OUT"
cls() { unzip -Z1 "$1" | grep '\.class$' | grep -v '\$' | sed 's/\.class$//; s|/|.|g'; }
G=~/.gradle/caches/modules-2/files-2.1
cls build/moddev/artifacts/neoforge-*[0-9].jar > "$OUT/neo_mc.txt"
cls "$(ls ../Mantle/build/libs/Mantle-*.jar | grep -v sources | head -1)" > "$OUT/mantle.txt"
cls "$(ls $G/net.neoforged.fancymodloader/loader/*/*/loader-*.jar | head -1)" > "$OUT/fml.txt"
cls "$(ls $G/net.neoforged/bus/*/*/bus-*.jar | head -1)" > "$OUT/bus.txt"
grep -rhoE '^import (static )?net\.minecraftforge\.[A-Za-z0-9_.]+' src/main/java src/test/java | sed -E 's/^import (static )?//' | sort | uniq -c | sort -rn > "$OUT/forge_imports.txt"
grep -rhoE '^import (static )?slimeknights\.mantle\.[A-Za-z0-9_.]+' src/main/java src/test/java | sed -E 's/^import (static )?//' | sort -u > "$OUT/mantle_imports.txt"
