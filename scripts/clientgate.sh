#!/bin/bash
# Launch each node as a real GAME CLIENT and require it to reach the title screen.
#
# WHY THIS EXISTS: every client-only mixin is blocked by RuntimeDistCleaner on a dedicated server, so
# `runServer` can NEVER exercise one. Three consecutive releases (1.0.2, 1.0.3, 1.0.6) shipped fatal
# client-side mixin faults through a fully green 18-node server gate. A client run is the only gate that
# touches client mixin APPLY, renderer registration and the client resource reload.
#
# Ready marker is "Sound engine started" — it lands after mixin apply, mod construction, client setup,
# renderer registration and the first resource reload, i.e. after everything that has ever broken here.
#
# usage: scripts/clientgate.sh <node> [node …]        env: SOAK=<seconds> (default 15)
set -u
SOAK="${SOAK:-15}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT" || exit 1
OUT="${OUT:-$ROOT/build/clientgate}"
mkdir -p "$OUT"
export DISPLAY="${DISPLAY:-:0}"
rc=0
for node in "$@"; do
  log="$OUT/cgate-$node.log"
  : > "$log"
  # Stash old crash reports so a stale one can't be mistaken for a new one (rm is sandbox-blocked here).
  cr="versions/$node/run/crash-reports"
  if [ -d "$cr" ] && [ -n "$(ls -A "$cr" 2>/dev/null)" ]; then
    mkdir -p "$OUT/old-crashes/$node"; mv "$cr"/* "$OUT/old-crashes/$node/" 2>/dev/null
  fi
  # fml_earlydisplay often cannot hand its GLFW window to Minecraft on Linux/Wayland; that fails at
  # Minecraft.<init>, BEFORE any client mixin target is class-loaded, so it must be off or the gate
  # proves nothing. FML reads this from run/config/fml.toml — NOT from a system property
  # (-Dfml.earlyWindowControl=false via JAVA_TOOL_OPTIONS is accepted by the JVM and ignored by FML).
  # FML fills in defaults for keys a partial file omits, so a one-key file is enough on a fresh node.
  fml="versions/$node/run/config/fml.toml"
  mkdir -p "$(dirname "$fml")"
  if [ -f "$fml" ]; then sed -i 's/^earlyWindowControl = true$/earlyWindowControl = false/' "$fml"
  else echo 'earlyWindowControl = false' > "$fml"; fi

  setsid ./gradlew ":$node:runClient" > "$log" 2>&1 &
  pid=$!
  verdict=TIMEOUT
  for _ in $(seq 1 240); do
    if grep -q 'Sound engine started' "$log"; then verdict=READY; break; fi
    if grep -qE 'BUILD FAILED|FAILURE:|Critical injection failure|InvalidMixin|A fatal error|Failed to create mod' "$log"; then verdict=FAILED; break; fi
    sleep 2
  done
  [ "$verdict" = READY ] && sleep "$SOAK"
  kill -- -"$pid" 2>/dev/null; sleep 3; kill -9 -- -"$pid" 2>/dev/null
  echo "########## $node : $verdict"
  [ "$verdict" = READY ] || rc=1
  # "No data fixer registered for <type>" is vanilla's dev-only DFU schema check, one line per modded
  # type (116 on an obfuscated Fabric node) — see the long note in bootgate.sh. Unfiltered it fills
  # the head -30 window and hides the lines that matter. Unlike bootgate's, this grep only PRINTS;
  # rc here is the READY verdict plus crash-reports.
  grep -E 'Sound engine started|/ERROR\]|Critical injection|InvalidInjection|InvalidMixin|Mixin apply failed|Failed to create mod|Couldn.t parse data file|Parsing error' "$log" \
    | grep -vE 'Missing subtitle translation|No data fixer registered for' | head -30
  if [ -d "$cr" ] && [ -n "$(ls -A "$cr" 2>/dev/null)" ]; then ls "$cr" | sed 's/^/  CRASH: /'; rc=1; fi
  sleep 2
done
echo "########## ALL DONE (rc=$rc)"
exit $rc
