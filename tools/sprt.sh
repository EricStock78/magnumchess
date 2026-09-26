#!/bin/bash
# Runs an engine-vs-engine match with fastchess: an SPRT test or a short smoke test.
#
# usage:
#   tools/sprt.sh <base-ref> <dev-ref> [elo0 elo1]    SPRT test (default bounds 0 10)
#   tools/sprt.sh --smoke <base-ref> <dev-ref>        short sanity match, no SPRT
#   tools/sprt.sh --resume <run-folder>               continue an interrupted run
#
#   a ref is a branch, tag or commit, or "." for the working tree (uncommitted edits included)
#
# examples:
#   tools/sprt.sh master .                  does the working tree gain 0..10 Elo over master?
#   tools/sprt.sh master eval-fix -5 0      non-regression test of branch eval-fix
#   tools/sprt.sh --smoke master .          20 quick games to check for crashes and timeouts
#
# settings can be overridden with environment variables, e.g.  CONCURRENCY=6 tools/sprt.sh master .
#
# each run gets its own folder under testing/runs/ holding both jars, the games (games.pgn),
# the fastchess output (output.txt), its resume file (config.json) and the refs tested (info.txt)

set -euo pipefail

TC="${TC:-10+0.1}"
CONCURRENCY="${CONCURRENCY:-4}"
HASH="${HASH:-64}"
JVM_ARGS="${JVM_ARGS:--Xmx256m}"
SMOKE_ROUNDS="${SMOKE_ROUNDS:-10}"       # rounds of 2 games (each opening played with both colours)
MAX_ROUNDS="${MAX_ROUNDS:-20000}"        # upper limit for SPRT, which normally stops much earlier

ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
TESTING="$ROOT/testing"
FASTCHESS="$TESTING/fastchess.exe"
BOOK="$TESTING/books/8moves_v3.pgn"

usage() { sed -n '2,20p' "$0" | sed 's/^# \{0,1\}//'; exit 1; }

# full fastchess output goes to output.txt; the console skips the per-move engine diagnostics
# (Warning/Info/Position/Moves lines) so the progress reports stay readable
quiet() { grep --line-buffered -v -E '^(Warning|Info|Position|Moves);' || true; }

[ -x "$FASTCHESS" ] || { echo "error: $FASTCHESS not found (see the setup steps)" >&2; exit 1; }

# --resume: restart fastchess from the saved state in an existing run folder
if [ "${1:-}" = "--resume" ]; then
    RUN="${2:-}"
    [ -f "$RUN/config.json" ] || { echo "error: no config.json in '$RUN'" >&2; exit 1; }
    cd "$RUN"
    echo "resuming $(pwd)"
    "$FASTCHESS" -config file=config.json stats=true 2>&1 | tee -a output.txt | quiet
    exit 0
fi

MODE="sprt"
if [ "${1:-}" = "--smoke" ]; then
    MODE="smoke"
    shift
fi

[ $# -eq 2 ] || [ $# -eq 4 -a "$MODE" = "sprt" ] || usage
BASE="$1"
DEV="$2"
ELO0="${3:-0}"
ELO1="${4:-10}"

[ -f "$BOOK" ] || { echo "error: opening book $BOOK not found (see the setup steps)" >&2; exit 1; }

# a readable folder name: date_time_mode_base_vs_dev, with "." shown as "worktree"
label() { if [ "$1" = "." ]; then echo "worktree"; else echo "$1" | tr '/' '-'; fi; }
RUN="$TESTING/runs/$(date +%Y-%m-%d_%H%M%S)_${MODE}_$(label "$BASE")_vs_$(label "$DEV")"
mkdir -p "$RUN"

# build both engines; build-jar.sh also runs a perft check on each jar
"$ROOT/tools/build-jar.sh" "$BASE" "$RUN/base.jar"
"$ROOT/tools/build-jar.sh" "$DEV" "$RUN/dev.jar"

# record exactly what is being tested
describe() {
    if [ "$1" = "." ]; then
        echo "working tree on $(git -C "$ROOT" rev-parse --abbrev-ref HEAD) $(git -C "$ROOT" rev-parse --short HEAD)$( [ -n "$(git -C "$ROOT" status --porcelain -- src)" ] && echo ' + uncommitted changes (see dev.diff)')"
    else
        echo "$1 $(git -C "$ROOT" rev-parse --short "$1")"
    fi
}
{
    echo "mode:        $MODE"
    echo "base:        $(describe "$BASE")"
    echo "dev:         $(describe "$DEV")"
    [ "$MODE" = "sprt" ] && echo "sprt bounds: elo0=$ELO0 elo1=$ELO1"
    echo "tc:          $TC   concurrency: $CONCURRENCY   hash: $HASH   jvm: $JVM_ARGS"
    echo "started:     $(date)"
} > "$RUN/info.txt"
[ "$DEV" = "." ] && git -C "$ROOT" diff -- src > "$RUN/dev.diff"
[ "$BASE" = "." ] && git -C "$ROOT" diff -- src > "$RUN/base.diff"
cat "$RUN/info.txt"

# fastchess runs inside the run folder, so config.json and all output land there
cd "$RUN"

ARGS=(
    -engine cmd=java.exe args="$JVM_ARGS -jar dev.jar" name=dev
    -engine cmd=java.exe args="$JVM_ARGS -jar base.jar" name=base
    -each tc="$TC" option.Hash="$HASH"
    -openings file=../../books/8moves_v3.pgn format=pgn order=random
    -repeat
    -concurrency "$CONCURRENCY"
    -pgnout file=games.pgn
)

if [ "$MODE" = "smoke" ]; then
    ARGS+=( -rounds "$SMOKE_ROUNDS" )
else
    ARGS+=(
        -rounds "$MAX_ROUNDS"
        -sprt elo0="$ELO0" elo1="$ELO1" alpha=0.05 beta=0.05 model=normalized
        -resign movecount=3 score=600
        -draw movenumber=40 movecount=8 score=10
    )
fi

echo "running in $RUN"
"$FASTCHESS" "${ARGS[@]}" 2>&1 | tee output.txt | quiet
echo "finished:    $(date)" >> info.txt
echo "results in $RUN"
