'use strict';

/* ============================================================
   app.js  –  UI Controller
   Renders the board, handles clicks, coordinates AI moves,
   and manages all modal / sidebar interactions.
   ============================================================ */

/* ── Unicode piece symbols ── */
const SYMBOLS = {
    white: { king: '♔', queen: '♕', rook: '♖', bishop: '♗', knight: '♘', pawn: '♙' },
    black: { king: '♚', queen: '♛', rook: '♜', bishop: '♝', knight: '♞', pawn: '♟' },
};

/* ── Web-Audio sound effects ── */
const AudioCtx = window.AudioContext || window.webkitAudioContext;
let _actx = null;
function _getActx() {
    if (!_actx) _actx = new AudioCtx();
    return _actx;
}

function playSound(type) {
    try {
        const actx  = _getActx();
        const osc   = actx.createOscillator();
        const gain  = actx.createGain();
        osc.connect(gain);
        gain.connect(actx.destination);

        if (type === 'move') {
            osc.frequency.value = 700;
            gain.gain.setValueAtTime(0.08, actx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.001, actx.currentTime + 0.12);
            osc.start();
            osc.stop(actx.currentTime + 0.12);
        } else if (type === 'capture') {
            osc.type = 'sawtooth';
            osc.frequency.value = 280;
            gain.gain.setValueAtTime(0.12, actx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.001, actx.currentTime + 0.22);
            osc.start();
            osc.stop(actx.currentTime + 0.22);
        } else if (type === 'check') {
            osc.type = 'square';
            osc.frequency.value = 500;
            gain.gain.setValueAtTime(0.10, actx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.001, actx.currentTime + 0.3);
            osc.start();
            osc.stop(actx.currentTime + 0.3);
        }
    } catch (_) { /* Audio not supported – silently ignore */ }
}

/* ============================================================
   App State
   ============================================================ */
let game       = new ChessGame();
let ai         = new ChessAI(3);
let mode       = '1p';          // '1p' | '2p'
let aiColor    = COLOR.BLACK;
let flipped    = false;         // board orientation
let selected   = null;          // { row, col } currently selected square
let validMoves = [];            // valid moves for selected piece
let lastMove   = null;          // { fromRow, fromCol, toRow, toCol }
let pendingPromotion = null;    // { fromRow, fromCol, toRow, toCol } awaiting user choice

/* ── DOM refs ── */
const boardEl         = document.getElementById('chess-board');
const turnText        = document.getElementById('turn-text');
const turnDot         = document.getElementById('turn-dot');
const statusMsg       = document.getElementById('status-message');
const movesList       = document.getElementById('moves-list');
const capturedByWhite = document.getElementById('captured-by-white');
const capturedByBlack = document.getElementById('captured-by-black');
const scoreDiffTop    = document.getElementById('score-diff-top');
const scoreDiffBot    = document.getElementById('score-diff-bottom');
const promoOverlay    = document.getElementById('promotion-overlay');
const promoChoices    = document.getElementById('promotion-choices');
const gameoverOverlay = document.getElementById('gameover-overlay');
const gameoverTitle   = document.getElementById('gameover-title');
const gameoverMsg     = document.getElementById('gameover-message');
const diffPanel       = document.getElementById('difficulty-panel');
const rankLabels      = document.getElementById('rank-labels');
const fileLabels      = document.getElementById('file-labels');

/* ============================================================
   Render helpers
   ============================================================ */

/** Render the full board from scratch */
function renderBoard() {
    boardEl.innerHTML = '';

    for (let displayRow = 0; displayRow < 8; displayRow++) {
        for (let displayCol = 0; displayCol < 8; displayCol++) {
            const row = flipped ? 7 - displayRow : displayRow;
            const col = flipped ? 7 - displayCol : displayCol;

            const sq = document.createElement('div');
            sq.className = 'square ' + ((row + col) % 2 === 0 ? 'light' : 'dark');
            sq.dataset.row = row;
            sq.dataset.col = col;

            /* Highlights */
            if (lastMove) {
                if ((row === lastMove.fromRow && col === lastMove.fromCol) ||
                    (row === lastMove.toRow   && col === lastMove.toCol)) {
                    sq.classList.add('last-move');
                }
            }

            if (selected && selected.row === row && selected.col === col) {
                sq.classList.add('selected');
            }

            /* Valid move indicators */
            const isValid = validMoves.find(m => m.toRow === row && m.toCol === col);
            if (isValid) {
                const hasEnemy = game.board[row][col] ||
                                 (isValid.type === 'enpassant');
                sq.classList.add(hasEnemy ? 'valid-capture' : 'valid-move');
            }

            /* King in check */
            const piece = game.board[row][col];
            if (piece && piece.type === PIECE.KING && game.gameStatus === 'check' &&
                piece.color === game.currentTurn) {
                sq.classList.add('in-check');
            }

            /* Piece */
            if (piece) {
                const span = document.createElement('span');
                span.className = 'piece';
                span.textContent = SYMBOLS[piece.color][piece.type];
                sq.appendChild(span);
            }

            sq.addEventListener('click', onSquareClick);
            boardEl.appendChild(sq);
        }
    }

    renderLabels();
    renderSidebar();
}

function renderLabels() {
    /* Rank labels (8..1 left side) */
    rankLabels.innerHTML = '';
    for (let displayRow = 0; displayRow < 8; displayRow++) {
        const rank = flipped ? displayRow + 1 : 8 - displayRow;
        const span = document.createElement('span');
        span.textContent = rank;
        rankLabels.appendChild(span);
    }

    /* File labels (a..h bottom) */
    fileLabels.innerHTML = '';
    for (let displayCol = 0; displayCol < 8; displayCol++) {
        const file = 'abcdefgh'[flipped ? 7 - displayCol : displayCol];
        const span = document.createElement('span');
        span.textContent = file;
        fileLabels.appendChild(span);
    }
}

function renderSidebar() {
    /* Turn indicator */
    const isWhite = game.currentTurn === COLOR.WHITE;
    turnDot.className = 'turn-dot ' + game.currentTurn;
    turnText.textContent = isWhite ? "White's Turn" : "Black's Turn";

    /* Status message */
    statusMsg.textContent =
        game.gameStatus === 'check'     ? '⚠️ Check!'         :
        game.gameStatus === 'checkmate' ? '🏁 Checkmate!'     :
        game.gameStatus === 'stalemate' ? '🤝 Stalemate'      : '';

    /* Captured pieces */
    renderCaptured();
    renderMoveHistory();
}

function renderCaptured() {
    /* capturedPieces.black = pieces of Black that have been captured (by White) */
    const order = [PIECE.QUEEN, PIECE.ROOK, PIECE.BISHOP, PIECE.KNIGHT, PIECE.PAWN];

    function buildStr(pieces) {
        return order.flatMap(type =>
            pieces.filter(p => p.type === type).map(p => SYMBOLS[p.color][p.type])
        ).join('');
    }

    capturedByWhite.textContent = buildStr(game.capturedPieces.black); // Black pieces White captured
    capturedByBlack.textContent = buildStr(game.capturedPieces.white); // White pieces Black captured

    /* Score advantage */
    const val = t => PIECE_VALUE[t] || 0;
    const whiteScore = game.capturedPieces.black.reduce((s, p) => s + val(p.type), 0);
    const blackScore = game.capturedPieces.white.reduce((s, p) => s + val(p.type), 0);
    const diff = (whiteScore - blackScore) / 100; // in pawns

    scoreDiffTop.textContent = diff < 0 ? `+${Math.abs(diff).toFixed(0)}` : '';
    scoreDiffBot.textContent = diff > 0 ? `+${diff.toFixed(0)}`           : '';
}

function renderMoveHistory() {
    movesList.innerHTML = '';
    game.moveHistory.forEach((entry, idx) => {
        if (idx % 2 === 0) {
            const num = document.createElement('span');
            num.className = 'move-entry move-num';
            num.textContent = (idx / 2 + 1) + '.';
            movesList.appendChild(num);
        }
        const span = document.createElement('span');
        span.className = 'move-entry';
        span.textContent = entry.notation || '?';
        movesList.appendChild(span);
    });
    /* Auto-scroll to bottom */
    movesList.scrollTop = movesList.scrollHeight;
}

/* Briefly animate the destination square after a move */
function animateLand(row, col) {
    const sq = boardEl.querySelector(`[data-row="${row}"][data-col="${col}"]`);
    if (!sq) return;
    const pieceEl = sq.querySelector('.piece');
    if (pieceEl) {
        pieceEl.classList.remove('piece-land');
        void pieceEl.offsetWidth; // reflow
        pieceEl.classList.add('piece-land');
    }
}

/* ============================================================
   Click handler
   ============================================================ */
function onSquareClick(e) {
    if (game.gameStatus === 'checkmate' || game.gameStatus === 'stalemate') return;
    /* During AI's turn in 1p mode, ignore clicks */
    if (mode === '1p' && game.currentTurn === aiColor) return;

    const row = parseInt(e.currentTarget.dataset.row);
    const col = parseInt(e.currentTarget.dataset.col);
    const piece = game.board[row][col];

    /* If we have a selection and clicked a valid target → make the move */
    if (selected) {
        const move = validMoves.find(m => m.toRow === row && m.toCol === col);
        if (move) {
            attemptMove(selected.row, selected.col, row, col);
            return;
        }
    }

    /* Select or deselect */
    if (piece && piece.color === game.currentTurn) {
        if (selected && selected.row === row && selected.col === col) {
            /* Deselect */
            selected   = null;
            validMoves = [];
        } else {
            selected   = { row, col };
            validMoves = game.getValidMoves(row, col);
        }
    } else {
        selected   = null;
        validMoves = [];
    }

    renderBoard();
}

/* ============================================================
   Move execution
   ============================================================ */
function attemptMove(fromRow, fromCol, toRow, toCol, promotion = null) {
    const result = game.makeMove(fromRow, fromCol, toRow, toCol, promotion);

    if (result === 'promotion') {
        /* Show promotion dialog */
        pendingPromotion = { fromRow, fromCol, toRow, toCol };
        showPromotionModal(game.board[fromRow][fromCol].color);
        return;
    }

    if (!result) return; // illegal

    selected   = null;
    validMoves = [];
    lastMove   = { fromRow, fromCol, toRow, toCol };

    renderBoard();
    animateLand(toRow, toCol);

    /* Sound */
    if (result.type === 'castle') {
        playSound('move');
    } else if (game.capturedPieces.white.length + game.capturedPieces.black.length >
               (game.moveHistory.length > 1 ?
                game.moveHistory.slice(0, -1).reduce((s, e) => s + (e.captured ? 1 : 0), 0) : 0)) {
        playSound('capture');
    } else {
        playSound('move');
    }

    if (game.gameStatus === 'check') playSound('check');

    /* Game over? */
    if (game.gameStatus === 'checkmate' || game.gameStatus === 'stalemate') {
        setTimeout(showGameOver, 600);
        return;
    }

    /* AI move */
    if (mode === '1p' && game.currentTurn === aiColor) {
        scheduleAI();
    }
}

function scheduleAI() {
    renderBoard(); // show updated board before AI thinks
    setTimeout(() => {
        const move = ai.getBestMove(game, aiColor);
        if (!move) return;

        /* Execute AI move (no promotion UI needed for AI – auto-queen) */
        const piece = game.board[move.fromRow][move.fromCol];
        const backRank = aiColor === COLOR.WHITE ? 0 : 7;
        const isPromo  = piece && piece.type === PIECE.PAWN && move.toRow === backRank;

        if (isPromo) {
            game.makeMove(move.fromRow, move.fromCol, move.toRow, move.toCol, PIECE.QUEEN);
        } else {
            game.makeMove(move.fromRow, move.fromCol, move.toRow, move.toCol);
        }

        lastMove = { fromRow: move.fromRow, fromCol: move.fromCol, toRow: move.toRow, toCol: move.toCol };

        playSound(game.board[move.toRow][move.toCol] && game.capturedPieces.black.length + game.capturedPieces.white.length > 0
            ? 'capture' : 'move');
        if (game.gameStatus === 'check') playSound('check');

        renderBoard();
        animateLand(move.toRow, move.toCol);

        if (game.gameStatus === 'checkmate' || game.gameStatus === 'stalemate') {
            setTimeout(showGameOver, 600);
        }
    }, 100); // small delay so browser repaints first
}

/* ============================================================
   Promotion modal
   ============================================================ */
function showPromotionModal(color) {
    promoChoices.innerHTML = '';
    const choices = [PIECE.QUEEN, PIECE.ROOK, PIECE.BISHOP, PIECE.KNIGHT];
    choices.forEach(type => {
        const btn = document.createElement('div');
        btn.className = 'promo-piece';
        btn.textContent = SYMBOLS[color][type];
        btn.title = type;
        btn.addEventListener('click', () => {
            promoOverlay.classList.add('hidden');
            const { fromRow, fromCol, toRow, toCol } = pendingPromotion;
            pendingPromotion = null;
            attemptMove(fromRow, fromCol, toRow, toCol, type);
        });
        promoChoices.appendChild(btn);
    });
    promoOverlay.classList.remove('hidden');
}

/* ============================================================
   Game-over modal
   ============================================================ */
function showGameOver() {
    if (game.gameStatus === 'checkmate') {
        const winner = game.currentTurn === COLOR.WHITE ? 'Black' : 'White';
        gameoverTitle.textContent = `${winner} wins!`;
        gameoverMsg.textContent   = 'Checkmate 🏁';
    } else {
        gameoverTitle.textContent = 'Draw';
        gameoverMsg.textContent   = 'Stalemate 🤝';
    }
    gameoverOverlay.classList.remove('hidden');
}

/* ============================================================
   Button event listeners
   ============================================================ */

/* Mode buttons */
document.querySelectorAll('.mode-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('.mode-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        mode = btn.dataset.mode;
        diffPanel.style.display = mode === '1p' ? '' : 'none';
        resetGame();
    });
});

/* Difficulty */
document.querySelectorAll('.diff-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('.diff-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        ai = new ChessAI(parseInt(btn.dataset.level));
    });
});

/* New game */
document.getElementById('btn-new-game').addEventListener('click', resetGame);

/* Undo */
document.getElementById('btn-undo').addEventListener('click', () => {
    /* In 1p mode undo both player and AI move */
    if (mode === '1p') {
        game.undoMove();
        game.undoMove();
    } else {
        game.undoMove();
    }
    selected   = null;
    validMoves = [];
    lastMove   = game.moveHistory.length > 0
        ? { ...game.moveHistory[game.moveHistory.length - 1].move,
            fromRow: game.moveHistory[game.moveHistory.length - 1].move.fromRow,
            fromCol: game.moveHistory[game.moveHistory.length - 1].move.fromCol,
            toRow:   game.moveHistory[game.moveHistory.length - 1].move.toRow,
            toCol:   game.moveHistory[game.moveHistory.length - 1].move.toCol }
        : null;
    renderBoard();
});

/* Flip board */
document.getElementById('btn-flip').addEventListener('click', () => {
    flipped = !flipped;
    renderBoard();
});

/* Theme toggle */
document.getElementById('btn-theme').addEventListener('click', () => {
    document.body.classList.toggle('light-mode');
    document.getElementById('btn-theme').textContent =
        document.body.classList.contains('light-mode') ? '☀️' : '🌙';
});

/* Play again */
document.getElementById('btn-play-again').addEventListener('click', () => {
    gameoverOverlay.classList.add('hidden');
    resetGame();
});

/* ============================================================
   Reset / init
   ============================================================ */
function resetGame() {
    game.reset();
    selected   = null;
    validMoves = [];
    lastMove   = null;
    pendingPromotion = null;
    gameoverOverlay.classList.add('hidden');
    promoOverlay.classList.add('hidden');
    renderBoard();

    /* If AI plays White, kick off AI move immediately */
    if (mode === '1p' && aiColor === COLOR.WHITE) {
        scheduleAI();
    }
}

/* ── Bootstrap ── */
diffPanel.style.display = mode === '1p' ? '' : 'none';
renderBoard();
