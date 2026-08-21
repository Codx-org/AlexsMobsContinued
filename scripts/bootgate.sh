#!/bin/bash
# Boot each node's DEDICATED SERVER, wait for "Done (", then keep it TICKING for SOAK seconds.
#
# The soak is load-bearing: the minecraft:tempt_range crash (Milestone 8) fired a few seconds AFTER
# "Done (", when a tempted mob first ticked, and a gate that stopped at that line passed eight nodes
# for three milestones while they crashed in practice.
#
# This gate is BLIND to client-only mixins — RuntimeDistCleaner blocks them on a server. Always pair it
# with scripts/clientgate.sh.
#
# usage: scripts/bootgate.sh <node> [node …]           env: SOAK=<seconds> (default 45)
set -u
SOAK="${SOAK:-45}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT" || exit 1
OUT="${OUT:-$ROOT/build/bootgate}"
mkdir -p "$OUT"
rc=0
for node in "$@"; do
  log="$OUT/soak-$node.log"
  : > "$log"
  cr="versions/$node/run/crash-reports"
  if [ -d "$cr" ] && [ -n "$(ls -A "$cr" 2>/dev/null)" ]; then
    mkdir -p "$OUT/old-crashes/$node"; mv "$cr"/* "$OUT/old-crashes/$node/" 2>/dev/null
  fi
  # MDG/loom write eula=false on a fresh node and the server then refuses to start.
  eula="versions/$node/run/eula.txt"
  mkdir -p "$(dirname "$eula")"; echo 'eula=true' > "$eula"
  # Same class of first-run scaffolding: a never-booted node has no server.properties, so vanilla logs
  # "Failed to load properties from file: server.properties" at ERROR and only then writes it. That is a
  # non-benign line by the filter below, so a node's FIRST run would always be red for a reason that has
  # nothing to do with the mod. Seed an empty one (vanilla fills in every default for a key it omits) —
  # never overwrite, some nodes carry a hand-edited one with RCON enabled.
  props="versions/$node/run/server.properties"
  [ -f "$props" ] || echo '# seeded by scripts/bootgate.sh; vanilla defaults every absent key' > "$props"

  # Forge's runServer consumes a piped `stop`; MDG's (NeoForge) does not — so always run detached and kill.
  setsid ./gradlew ":$node:runServer" > "$log" 2>&1 &
  pid=$!
  verdict=TIMEOUT
  for _ in $(seq 1 200); do
    if grep -q 'Done (' "$log"; then verdict=DONE; break; fi
    if grep -qE 'BUILD FAILED|Failed to start the minecraft server' "$log"; then verdict=FAILED; break; fi
    sleep 2
  done
  [ "$verdict" = DONE ] && sleep "$SOAK"
  kill -- -"$pid" 2>/dev/null; sleep 3; kill -9 -- -"$pid" 2>/dev/null
  echo "########## $node : $verdict"
  [ "$verdict" = DONE ] || rc=1
  # The five benign ERROR-level patterns, all documented in docs/notes/gates.md: Forge's RuntimeDistCleaner and
  # NeoForge's NeoForgeDevDistCleaner name the client mixins a server never loads; OnlyInWarningsHandler
  # (NeoForge >= 1.21.7) reports every @OnlyIn usage; RealmsClient fails with no Mojang session.
  #
  # "No data fixer registered for <type>" is vanilla's own dev-only DFU schema check
  # (Util#fetchChoiceType, guarded by SharedConstants.CHECK_DATA_FIXER_SCHEMA). It fires once per
  # registered entity / block-entity type that the vanilla schema does not know, i.e. once per MODDED
  # type — 116 lines on 1.20.1-fabric. It is invisible on Forge/NeoForge only because they patch that
  # method to skip non-vanilla namespaces, and it stopped being logged in the 26.x era, which is why
  # only the obfuscated Fabric nodes show it. It can never indicate an AMC fault: this mod ships no
  # datafixers on ANY loader, so the Forge nodes are in exactly the same state and merely quieter.
  benign='RuntimeDistCleaner|NeoForgeDevDistCleaner|OnlyInWarningsHandler|RealmsClient'
  benign="$benign"'|No data fixer registered for'
  grep -E 'Done \(' "$log"
  bad="$(grep -E '/ERROR\]|Couldn.t load tag|Couldn.t parse data file|Parsing error|InvalidMixin' "$log" \
         | grep -vE "$benign")"
  # A data-pack fault is LOGGED, NOT THROWN — the server still reaches "Done (" and exits 0. That is
  # exactly how Forge 26's `c:` convention-tag break (Milestone 13) survived a green 30-node run, so
  # these lines have to fail the node, not merely be printed.
  if [ -n "$bad" ]; then
    echo "$bad" | head -40
    echo "  NON-BENIGN: $(echo "$bad" | wc -l) line(s)"
    rc=1
  fi
  if [ -d "$cr" ] && [ -n "$(ls -A "$cr" 2>/dev/null)" ]; then ls "$cr" | sed 's/^/  CRASH: /'; rc=1; fi
  sleep 2
done
echo "########## ALL DONE (rc=$rc)"
exit $rc
