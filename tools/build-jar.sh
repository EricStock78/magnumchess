#!/bin/bash
# Builds an executable MagnumChess jar and smoke-tests it with perft.
#
# usage: tools/build-jar.sh [git-ref] [output.jar]
#
#   no arguments          build the working tree into dist/MagnumChess.jar
#   git-ref               build a branch, tag or commit instead of the working tree
#                         (use "." for the working tree when also giving an output path)
#   output.jar            where to write the jar (default dist/MagnumChess.jar)
#
# examples:
#   tools/build-jar.sh
#   tools/build-jar.sh master engines/base.jar
#   tools/build-jar.sh . engines/dev.jar

set -euo pipefail

REF="${1:-.}"
OUT="${2:-dist/MagnumChess.jar}"

ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
cd "$ROOT"

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# collect the sources: either the working tree or a snapshot of the given git ref
if [ "$REF" = "." ]; then
    SRC="src/magnumchess"
    LABEL="working tree"
else
    git rev-parse --verify --quiet "$REF^{commit}" > /dev/null || { echo "error: unknown git ref '$REF'" >&2; exit 1; }
    git archive "$REF" src/magnumchess | tar -x -C "$WORK"
    SRC="$WORK/src/magnumchess"
    LABEL="$REF ($(git rev-parse --short "$REF"))"
fi

echo "building $LABEL -> $OUT"

# compile for Java 8+ so the jar also runs on older Java installations
mkdir -p "$WORK/classes"
javac --release 8 -nowarn -d "$WORK/classes" "$SRC"/*.java

# initialization.dat must sit next to Board.class (loaded with getResourceAsStream)
cp "$SRC/initialization.dat" "$WORK/classes/magnumchess/"

mkdir -p "$(dirname "$OUT")"
jar --create --file "$OUT" --main-class magnumchess.Main -C "$WORK/classes" .

# smoke test: perft 4 from the start position must be 197281
RESULT="$(printf 'uci\nposition startpos\nperft 4\nquit\n' | java -jar "$OUT" | grep 'Perft value' || true)"
if [ "$RESULT" = "Perft value is 197281" ]; then
    echo "ok: $RESULT"
else
    echo "error: smoke test failed (expected 'Perft value is 197281', got '$RESULT')" >&2
    exit 1
fi
