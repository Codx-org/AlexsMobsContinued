#!/bin/bash
# Parallel sibling of clientgate.sh: run JOBS nodes' clients concurrently instead of one at a time.
#
# WHY THIS IS SAFE, and why clientgate.sh is still sequential:
#   Rule 1 ("multi-node Gradle must be ONE invocation") is about the ACTIVE node — separate
#   ./gradlew calls collide on Stonecutter's active-version state because the active node's sources
#   live in the root src/. Every NON-active node is projected into its own versions/<node>/src and
#   is touched by nothing else (stonecutter.gradle.kts:474), so concurrent runClient on distinct
#   non-active nodes does not share mutable state.
#   ⚠️ The active node (stonecutter.gradle.kts: `stonecutter active`) is therefore run ALONE, first,
#   before any parallel batch — see ACTIVE below.
#   Gradle itself: a busy daemon makes the next invocation spawn another daemon (org.gradle.jvmargs
#   is -Xmx4G each), and the shared caches under ~/.gradle are file-locked, so the configuration
#   phases partly serialise. The forked client JVMs then run genuinely in parallel, which is where
#   the wall-clock actually goes.
#
# usage: scripts/clientgate_par.sh <node> [node …]
# env:   SOAK=<seconds> (default 15)   JOBS=<n> (default 4)
set -u
SOAK="${SOAK:-15}"
JOBS="${JOBS:-4}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT" || exit 1
OUT="${OUT:-$ROOT/build/clientgate}"
mkdir -p "$OUT"
export DISPLAY="${DISPLAY:-:0}"
rc=0

ACTIVE="$(sed -n 's/^stonecutter active "\(.*\)".*/\1/p' stonecutter.gradle.kts | head -1)"
echo "active node = ${ACTIVE:-<none>} (run alone, never inside a parallel batch)"

prep() { # $1 = node — the per-node setup clientgate.sh does before launching
  local node="$1" cr fml
  cr="versions/$node/run/crash-reports"
  if [ -d "$cr" ] && [ -n "$(ls -A "$cr" 2>/dev/null)" ]; then
    mkdir -p "$OUT/old-crashes/$node"; mv "$cr"/* "$OUT/old-crashes/$node/" 2>/dev/null
  fi
  # fml_earlydisplay cannot hand its GLFW window to Minecraft on Linux/Wayland and fails at
  # Minecraft.<init>, BEFORE any client mixin target is class-loaded — so the gate would prove
  # nothing. FML reads this from run/config/fml.toml, NOT from a system property.
  fml="versions/$node/run/config/fml.toml"
  mkdir -p "$(dirname "$fml")"
  if [ -f "$fml" ]; then sed -i 's/^earlyWindowControl = true$/earlyWindowControl = false/' "$fml"
  else echo 'earlyWindowControl = false' > "$fml"; fi
}

report() { # $1 = node, $2 = verdict
  local node="$1" verdict="$2" log="$OUT/cgate-$node.log" cr="versions/$node/run/crash-reports"
  echo "########## $node : $verdict"
  [ "$verdict" = READY ] || rc=1
  grep -E 'Sound engine started|/ERROR\]|Critical injection|InvalidInjection|InvalidMixin|Mixin apply failed|Failed to create mod|Couldn.t parse data file|Parsing error' "$log" \
    | grep -vE 'Missing subtitle translation|No data fixer registered for' | head -30
  if [ -d "$cr" ] && [ -n "$(ls -A "$cr" 2>/dev/null)" ]; then ls "$cr" | sed 's/^/  CRASH: /'; rc=1; fi
}

run_batch() { # $@ = nodes to run concurrently
  local -a batch=("$@") pids=() verdicts=() live
  local i node log
  for i in "${!batch[@]}"; do
    node="${batch[$i]}"; log="$OUT/cgate-$node.log"; : > "$log"
    prep "$node"
    setsid ./gradlew ":$node:runClient" > "$log" 2>&1 &
    pids[$i]=$!
    verdicts[$i]=TIMEOUT
  done
  echo "--- batch: ${batch[*]}"
  # 240 × 2s = 8 min, the same ceiling clientgate.sh gives a single node. Concurrency raises
  # per-node wall-clock (shared CPU + Gradle lock waits), so the ceiling is per-batch, not per-node.
  for _ in $(seq 1 360); do
    live=0
    for i in "${!batch[@]}"; do
      [ "${verdicts[$i]}" = TIMEOUT ] || continue
      log="$OUT/cgate-${batch[$i]}.log"
      if grep -q 'Sound engine started' "$log" 2>/dev/null; then verdicts[$i]=READY
      elif grep -qE 'BUILD FAILED|FAILURE:|Critical injection failure|InvalidMixin|A fatal error|Failed to create mod' "$log" 2>/dev/null; then verdicts[$i]=FAILED
      else live=$((live + 1)); fi
    done
    [ "$live" -eq 0 ] && break
    sleep 2
  done
  # Soak every client that came up, together — they are already running concurrently.
  printf '%s\n' "${verdicts[@]}" | grep -q READY && sleep "$SOAK"
  for i in "${!batch[@]}"; do
    kill -- -"${pids[$i]}" 2>/dev/null
  done
  sleep 3
  for i in "${!batch[@]}"; do
    kill -9 -- -"${pids[$i]}" 2>/dev/null
  done
  sleep 2
  for i in "${!batch[@]}"; do
    report "${batch[$i]}" "${verdicts[$i]}"
  done
}

# The active node first, alone.
declare -a rest=()
for node in "$@"; do
  if [ "$node" = "$ACTIVE" ]; then run_batch "$node"; else rest+=("$node"); fi
done

batch=()
for node in "${rest[@]}"; do
  batch+=("$node")
  if [ "${#batch[@]}" -eq "$JOBS" ]; then run_batch "${batch[@]}"; batch=(); fi
done
[ "${#batch[@]}" -gt 0 ] && run_batch "${batch[@]}"

echo "########## ALL DONE (rc=$rc)"
exit $rc
