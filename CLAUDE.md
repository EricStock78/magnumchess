# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Magnum Chess is a UCI chess engine written in Java (single package `magnumchess`, no external dependencies). It started as a school project and is a hobby engine. It uses magic bitboard move generation, alpha-beta search with a transposition table, and a hand-tuned evaluation. `wiki/ProjectGoals.wiki` lists the goals: find the bugs that cost playing strength (in the evaluation especially), make UCI support complete enough for tournaments (PV output, ponder), and try other algorithms (MTD(f), parallel search).

## Build & run

This is a NetBeans/Ant project (`build.xml`, `nbproject/`, `main.class=magnumchess.Main`, source/target 1.7, run JVM args `-Xms256m -Xmx1024m`). Ant is not installed on this machine, so build with plain `javac` (JDK 17 is on PATH):

```sh
javac -d build/classes src/magnumchess/*.java
cp src/magnumchess/initialization.dat build/classes/magnumchess/   # required: loaded via getResourceAsStream
java -ea -cp build/classes magnumchess.Main
```

`initialization.dat` must sit next to the compiled `Board.class`. Without it, board initialization fails. `-ea` turns on the engine's `assert` checks.

`dist/MagnumChess.jar` and `MagnumChess.exe` (a launch4j wrapper) are committed build outputs. `Magnum.bat` runs the jar. Rebuild them only when asked. `polyglot.ini-Windows` is the config for running the engine through polyglot.

## Testing

There are no unit tests. Correctness is checked through commands on stdin. These commands work only after `uci` has been sent, because `Main.getCmd()` handles just `uci`, `setvalue`, `RandomTest`, and `quit` before that point:

```sh
printf 'uci\nposition startpos\nperft 4\nquit\n' | java -ea -cp build/classes magnumchess.Main
# expected: "Perft value is 197281"
```

- `perft N`: counts leaf nodes. Compare the count with standard perft results. Also use `position fen <FEN>` to test tricky positions. Two caveats:
  - Underpromotions are generated only when `Engine.PERFT_ENABLED` is `true`. It is `false` by default, so perft counts are too low for any position where a promotion is possible.
  - With `-ea`, every `position fen` fails an assertion straight away, because `Board.acceptFen` builds a hash that doesn't match `generateHash()`.
- `divide N`: prints the node count under each root move, to find the move whose count is wrong.
- `eval_dump_white` / `eval_dump_black`: print the individual evaluation terms for the current position.
- `perft` actually calls `PerftDebug`, which also checks that the evaluation is symmetric. At every node it flips the board (`Board.FlipPosition()`), compares the two scores, and prints the eval terms when they differ. Any output besides the node count therefore points to an asymmetry bug in the evaluation. It also makes perft slower than raw move generation (`Perft` is the pure version).
- `RandomTest` (sent before `uci`) plays random games to stress-test the engine.
- `setvalue ...` / `Main.SetClopParams`: sets evaluation parameters at runtime, for CLOP tuning.

## Architecture

State is global and mutable. `Board` is a singleton (`Board.getInstance()`). `Main` holds static references to the `Engine`, `Evaluation2`, `SEE`, and `HistoryWriter`, and many classes cache the board in static fields. Nothing is thread-safe, so a parallel search would require restructuring this.

- **`Main`**: reads UCI commands from stdin (`getCmd` → `uci` loop), parses `position`, `go` (depth/movetime/infinite/wtime/btime/winc/binc/movestogo), and `setoption` (Hash, Evaluation Table, Pawn Table sizes). Any uncaught exception is written to a file named `error` in the working directory, together with the last move list.
- **`Board`**: bitboards, the `piece_in_square[]` array, Zobrist hashing, make/unmake, FEN parsing, and attack generation. Magic numbers, shifts, masks, and the sliding-attack tables are read from `initialization.dat` rather than computed at startup. The inner class `CheckInfo` helps with generating checking moves.
- **`Engine`**: search. `search()` does iterative deepening at the root and handles time management (`GotoTimeState`, `timeLeft`). `Max()` is the main alpha-beta/PVS routine (null move, IID, extensions, excluded-move/singular search). `Quies()` is quiescence search; it probes the hash and generates checking moves at `QUIES_CHECK_MAX_DEPTH`. It also contains the staged move generators (`getMoves`, `getCaptures`, `getCheckEscapes`, `getCheckingMoves`), move sorting, `verifyMove` (legality check for hash/killer moves), and perft/divide.
- **`Evaluation2`**: static evaluation (material, piece-square tables, pawn structure, passed pawns, king safety, special endgames such as KBNK and KPK via `Bitbase`). Its scores are cached in eval and pawn hash tables (instances of `TransTable`).
- **`TransTable`**: one class used for three tables. As the main TT (type 0), each bucket has 4 slots packed into `int`s: the bucket index comes from the upper 32 bits of the Zobrist key, and the lower 32 bits are stored as the lock. As the pawn hash (type 1) and the eval hash (type 2), it is a single-entry, always-replace table that stores the full 64-bit key.
- **`SEE`**: static exchange evaluation, used for capture ordering and pruning.
- **`HistoryWriter`**: applies the `position ... moves` list to the board and converts internal moves to UCI move strings.
- **`MoveFunctions`**: move encoding in an `int`: bits 0–5 are from, 6–11 are to, 12–15 are the move type (`Global.ORDINARY_MOVE`, `ORDINARY_CAPTURE`, castles, promotions, …). Bits 16+ carry the killer piece or the root-draw flag, and bit 24 marks a mate killer.
- **`Global`**: all constants. Colours are `COLOUR_WHITE=0` / `COLOUR_BLACK=1`. Piece-type order is ROOK=0, KNIGHT, BISHOP, QUEEN, KING, PAWN=5; a coloured piece index is `type + 6*side`, so `piece % 6` gives the type. Squares run 0–63 with a1=0.

### MagnumDataWriter (separate project)

`MagnumDataWriter/` is a separate NetBeans project that generates `initialization.dat` (magic bitboard tables and the KPK bitbase data). It has its own copy of `Board`/`Global`, which do not share code with the engine. If you change the data format, change both `MagnumDataWriter` and `Board.InitializeDataFromFile()`, then copy the regenerated `.dat` file into `src/magnumchess/`.

## Conventions

- Source files carry a GPLv3 header and use a mix of tabs and spaces. Match the surrounding code.
- Commit messages are bullet-style lines starting with `-` (e.g. `-added hashing to q search`).
