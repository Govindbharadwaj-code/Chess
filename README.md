# ♟ Chess

A fully-featured, browser-based Chess game built with **HTML + CSS + JavaScript** — no libraries or frameworks required.

## Features

### Game Rules (complete)
- All piece movements: Pawn, Rook, Knight, Bishop, Queen, King
- Castling (kingside & queenside)
- En passant
- Pawn promotion (choose Queen / Rook / Bishop / Knight)
- Check, Checkmate, and Stalemate detection

### Game Modes
- **1 Player** – Play against an AI opponent
- **2 Players** – Local multiplayer (take turns on the same screen)

### AI Player
- Minimax algorithm with **Alpha-Beta Pruning**
- Three difficulty levels:
  - **Easy** – random move selection (biased toward captures)
  - **Medium** – depth-3 search
  - **Hard** – depth-5 search
- Material evaluation + piece-square position tables (PST) for smarter play

### Premium UI
- Dark / Light mode toggle
- Glassmorphism card design
- Valid-move highlighting (dots for empty squares, rings for captures)
- Last-move highlight
- King-in-check highlight (red square)
- Captured pieces display with material score advantage
- Move history panel (algebraic notation)
- Flip board button
- Undo (single move in 2-player; full round-trip in 1-player)
- Pawn promotion modal
- Game-over modal with Play Again button
- Sound effects (move, capture, check) via Web Audio API
- Responsive layout (mobile-friendly)

## How to Run

Simply open `index.html` in any modern browser — no build step required.

```bash
# Option 1: open directly
open index.html          # macOS
start index.html         # Windows

# Option 2: serve locally (avoids any browser security restrictions)
npx serve .
# or
python3 -m http.server 8080
```

## Project Structure

```
Chess/
├── index.html        ← Main page
├── css/
│   └── style.css     ← Premium dark/light UI
└── js/
    ├── chess.js      ← Complete chess engine (board, rules, evaluation)
    ├── ai.js         ← Minimax + Alpha-Beta Pruning AI
    └── app.js        ← UI controller (rendering, events, sounds)
```

## Technology Stack

| Layer | Technology |
|-------|-----------|
| UI | HTML5, CSS3 (custom properties, glassmorphism, CSS Grid) |
| Logic | Vanilla JavaScript (ES6+) |
| AI | Minimax + Alpha-Beta Pruning + Piece-Square Tables |
| Sound | Web Audio API |
| Fonts | Google Fonts – Inter |

## Build Plan (implemented)

- [x] **Step 1** – Basic board (8×8 grid UI)
- [x] **Step 2** – Pieces placed and rendered
- [x] **Step 3** – Movement rules for all 6 piece types
- [x] **Step 4** – Turn system (White / Black)
- [x] **Step 5** – Check, Checkmate, Stalemate detection + Castling + En passant + Promotion
- [x] **Step 6** – AI (Easy random → Medium/Hard minimax with alpha-beta)
- [x] **Step 7** – UI polish (highlights, animations, sound, dark/light mode, move history)
