# ♔ Chess

A fully playable JavaFX Chess game with a minimax AI opponent.

## Features

- **Human vs Human** or **Human vs AI** modes
- **Minimax AI** with Alpha-Beta pruning at depth 4
- **Full chess rules**: castling, en passant, pawn promotion
- **Visual highlights**: selected piece, valid moves, last move, king in check
- **Move history** sidebar with algebraic notation
- **Undo** move support

## Requirements

- Java 17+
- Maven 3.6+

## How to Run

```bash
mvn javafx:run
```

Or compile and run:

```bash
mvn compile
mvn javafx:run
```

## Project Structure

```
src/main/java/chess/
├── Main.java
├── model/
│   ├── PieceType.java       # Enum: PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING
│   ├── PieceColor.java      # Enum: WHITE, BLACK
│   ├── MoveType.java        # Enum: NORMAL, CASTLING, EN_PASSANT, PROMOTION
│   ├── Move.java            # Move data class
│   ├── Piece.java           # Abstract piece base class
│   ├── Pawn/Rook/Knight/Bishop/Queen/King.java
│   ├── Board.java           # Board state, legal move generation, make/undo move
│   └── GameState.java       # Game state management
├── ai/
│   └── ChessAI.java         # Minimax with Alpha-Beta pruning + piece-square tables
└── ui/
    ├── ChessApp.java         # JavaFX Application entry point
    ├── BoardView.java        # 8x8 board grid with highlights
    └── GameController.java   # Click handling, AI triggering, UI updates
```

## Board Coordinates

`board[row][col]` where `row=0` is rank 8 (black back rank), `row=7` is rank 1 (white back rank).
White pieces start at rows 6–7, black at rows 0–1.
