'use strict';

/* ============================================================
   chess.js  –  Complete Chess Engine
   ============================================================
   Board orientation:
     row 0 = rank 8  (black's back rank, top of board)
     row 7 = rank 1  (white's back rank, bottom of board)
     col 0 = file a,  col 7 = file h
   White pieces start at rows 6–7, black at rows 0–1.
   White pawns move UP   (row decreases, dir = -1).
   Black pawns move DOWN (row increases, dir = +1).
   ============================================================ */

const PIECE = {
    PAWN:   'pawn',
    ROOK:   'rook',
    KNIGHT: 'knight',
    BISHOP: 'bishop',
    QUEEN:  'queen',
    KING:   'king',
};

const COLOR = { WHITE: 'white', BLACK: 'black' };

/* Material values (centipawns) */
const PIECE_VALUE = {
    pawn:   100,
    knight: 320,
    bishop: 330,
    rook:   500,
    queen:  900,
    king:   20000,
};

/* ── Position bonus tables (from White's perspective, row 0 = rank 8) ── */
const PST = {};

PST.pawn = [
    [ 0,  0,  0,  0,  0,  0,  0,  0],
    [50, 50, 50, 50, 50, 50, 50, 50],
    [10, 10, 20, 30, 30, 20, 10, 10],
    [ 5,  5, 10, 25, 25, 10,  5,  5],
    [ 0,  0,  0, 20, 20,  0,  0,  0],
    [ 5, -5,-10,  0,  0,-10, -5,  5],
    [ 5, 10, 10,-20,-20, 10, 10,  5],
    [ 0,  0,  0,  0,  0,  0,  0,  0],
];

PST.knight = [
    [-50,-40,-30,-30,-30,-30,-40,-50],
    [-40,-20,  0,  0,  0,  0,-20,-40],
    [-30,  0, 10, 15, 15, 10,  0,-30],
    [-30,  5, 15, 20, 20, 15,  5,-30],
    [-30,  0, 15, 20, 20, 15,  0,-30],
    [-30,  5, 10, 15, 15, 10,  5,-30],
    [-40,-20,  0,  5,  5,  0,-20,-40],
    [-50,-40,-30,-30,-30,-30,-40,-50],
];

PST.bishop = [
    [-20,-10,-10,-10,-10,-10,-10,-20],
    [-10,  0,  0,  0,  0,  0,  0,-10],
    [-10,  0,  5, 10, 10,  5,  0,-10],
    [-10,  5,  5, 10, 10,  5,  5,-10],
    [-10,  0, 10, 10, 10, 10,  0,-10],
    [-10, 10, 10, 10, 10, 10, 10,-10],
    [-10,  5,  0,  0,  0,  0,  5,-10],
    [-20,-10,-10,-10,-10,-10,-10,-20],
];

PST.rook = [
    [ 0,  0,  0,  0,  0,  0,  0,  0],
    [ 5, 10, 10, 10, 10, 10, 10,  5],
    [-5,  0,  0,  0,  0,  0,  0, -5],
    [-5,  0,  0,  0,  0,  0,  0, -5],
    [-5,  0,  0,  0,  0,  0,  0, -5],
    [-5,  0,  0,  0,  0,  0,  0, -5],
    [-5,  0,  0,  0,  0,  0,  0, -5],
    [ 0,  0,  0,  5,  5,  0,  0,  0],
];

PST.queen = [
    [-20,-10,-10, -5, -5,-10,-10,-20],
    [-10,  0,  0,  0,  0,  0,  0,-10],
    [-10,  0,  5,  5,  5,  5,  0,-10],
    [ -5,  0,  5,  5,  5,  5,  0, -5],
    [  0,  0,  5,  5,  5,  5,  0, -5],
    [-10,  5,  5,  5,  5,  5,  0,-10],
    [-10,  0,  5,  0,  0,  0,  0,-10],
    [-20,-10,-10, -5, -5,-10,-10,-20],
];

PST.king_mid = [
    [-30,-40,-40,-50,-50,-40,-40,-30],
    [-30,-40,-40,-50,-50,-40,-40,-30],
    [-30,-40,-40,-50,-50,-40,-40,-30],
    [-30,-40,-40,-50,-50,-40,-40,-30],
    [-20,-30,-30,-40,-40,-30,-30,-20],
    [-10,-20,-20,-20,-20,-20,-20,-10],
    [ 20, 20,  0,  0,  0,  0, 20, 20],
    [ 20, 30, 10,  0,  0, 10, 30, 20],
];

/* ── Helper: flip a PST row index for Black ── */
function flipRow(r) { return 7 - r; }

/* ============================================================
   ChessGame class
   ============================================================ */
class ChessGame {
    constructor() {
        this.reset();
    }

    /* ── Public API ── */

    reset() {
        this.board          = this._initBoard();
        this.currentTurn    = COLOR.WHITE;
        this.enPassantTarget = null;  // { row, col } of capturable pawn position
        this.moveHistory    = [];     // array of { move, captured, prevEP, prevHalf }
        this.capturedPieces = { white: [], black: [] }; // pieces captured of this color
        this.gameStatus     = 'playing'; // 'playing'|'check'|'checkmate'|'stalemate'
        this.halfMoveClock  = 0;
        this.fullMove       = 1;
    }

    /* Returns array of legal moves { fromRow,fromCol,toRow,toCol,type,rookFromCol?,promotion? }
       type: 'normal' | 'castle' | 'enpassant'                                      */
    getValidMoves(row, col) {
        const piece = this.board[row][col];
        if (!piece || piece.color !== this.currentTurn) return [];
        return this._legalMoves(row, col);
    }

    /* Returns 'promotion' if promotion choice is needed (caller should call
       makeMove again with promotionPiece set), otherwise returns the move object
       or null if the move is illegal.                                              */
    makeMove(fromRow, fromCol, toRow, toCol, promotionPiece = null) {
        const legal = this._legalMoves(fromRow, fromCol);
        const move  = legal.find(m => m.toRow === toRow && m.toCol === toCol);
        if (!move) return null;

        /* Pawn promotion – need a piece choice */
        const piece     = this.board[fromRow][fromCol];
        const backRank  = piece.color === COLOR.WHITE ? 0 : 7;
        const isPromo   = piece.type === PIECE.PAWN && toRow === backRank;
        if (isPromo && !promotionPiece) return 'promotion';

        const finalMove = { ...move };
        if (isPromo) finalMove.promotion = promotionPiece;

        /* Execute */
        const captured = this._applyMove(finalMove);

        this.moveHistory.push({
            move:     finalMove,
            captured,
            notation: this._toAlgebraic(finalMove, captured),
        });

        if (captured) this.capturedPieces[captured.color].push(captured);
        if (piece.type !== PIECE.PAWN && !captured) {
            this.halfMoveClock++;
        } else {
            this.halfMoveClock = 0;
        }
        if (this.currentTurn === COLOR.WHITE) this.fullMove++;

        this._advanceTurn();
        this._updateStatus();
        return finalMove;
    }

    undoMove() {
        if (this.moveHistory.length === 0) return false;
        const { move, captured } = this.moveHistory.pop();
        this._unapplyMove(move, captured);

        /* Restore captured list */
        if (captured) {
            const arr = this.capturedPieces[captured.color];
            const idx = arr.findLastIndex(p => p.type === captured.type);
            if (idx !== -1) arr.splice(idx, 1);
        }

        this._advanceTurn(); // toggles back
        this._updateStatus();
        return true;
    }

    isInCheck(color) {
        const pos = this._findKing(color);
        if (!pos) return false;
        const opp = color === COLOR.WHITE ? COLOR.BLACK : COLOR.WHITE;
        return this._isAttacked(pos.row, pos.col, opp);
    }

    getAllLegalMoves(color) {
        const saved = this.currentTurn;
        this.currentTurn = color;
        const moves = [];
        for (let r = 0; r < 8; r++) {
            for (let c = 0; c < 8; c++) {
                const p = this.board[r][c];
                if (p && p.color === color) {
                    moves.push(...this._legalMoves(r, c));
                }
            }
        }
        this.currentTurn = saved;
        return moves;
    }

    /* ── Internal helpers ── */

    _initBoard() {
        const b     = Array.from({ length: 8 }, () => Array(8).fill(null));
        const order = [PIECE.ROOK, PIECE.KNIGHT, PIECE.BISHOP, PIECE.QUEEN,
                       PIECE.KING, PIECE.BISHOP, PIECE.KNIGHT, PIECE.ROOK];
        order.forEach((type, c) => {
            b[0][c] = { type, color: COLOR.BLACK, hasMoved: false };
            b[7][c] = { type, color: COLOR.WHITE, hasMoved: false };
        });
        for (let c = 0; c < 8; c++) {
            b[1][c] = { type: PIECE.PAWN, color: COLOR.BLACK, hasMoved: false };
            b[6][c] = { type: PIECE.PAWN, color: COLOR.WHITE, hasMoved: false };
        }
        return b;
    }

    _advanceTurn() {
        this.currentTurn = this.currentTurn === COLOR.WHITE ? COLOR.BLACK : COLOR.WHITE;
    }

    _updateStatus() {
        const c       = this.currentTurn;
        const inCheck = this.isInCheck(c);
        const hasMoves = this._hasAnyLegal(c);

        if (!hasMoves) {
            this.gameStatus = inCheck ? 'checkmate' : 'stalemate';
        } else if (inCheck) {
            this.gameStatus = 'check';
        } else {
            this.gameStatus = 'playing';
        }
    }

    _hasAnyLegal(color) {
        const saved = this.currentTurn;
        this.currentTurn = color;
        for (let r = 0; r < 8; r++) {
            for (let c = 0; c < 8; c++) {
                const p = this.board[r][c];
                if (p && p.color === color && this._legalMoves(r, c).length > 0) {
                    this.currentTurn = saved;
                    return true;
                }
            }
        }
        this.currentTurn = saved;
        return false;
    }

    /* Generate legal moves = pseudo-legal minus those leaving king in check */
    _legalMoves(row, col) {
        return this._pseudoLegal(row, col).filter(m => !this._leavesKingInCheck(m));
    }

    _leavesKingInCheck(move) {
        const snap = this._snapshot();
        this._applyMove(move, /* advanceTurn= */ false);
        const inCheck = this.isInCheck(snap.currentTurn);
        this._restore(snap);
        return inCheck;
    }

    /* ── Pseudo-legal move generators ── */

    _pseudoLegal(row, col) {
        const p = this.board[row][col];
        if (!p) return [];
        switch (p.type) {
            case PIECE.PAWN:   return this._pawnMoves(row, col);
            case PIECE.ROOK:   return this._slide(row, col, [[0,1],[0,-1],[1,0],[-1,0]]);
            case PIECE.KNIGHT: return this._knightMoves(row, col);
            case PIECE.BISHOP: return this._slide(row, col, [[1,1],[1,-1],[-1,1],[-1,-1]]);
            case PIECE.QUEEN:  return this._slide(row, col, [[0,1],[0,-1],[1,0],[-1,0],[1,1],[1,-1],[-1,1],[-1,-1]]);
            case PIECE.KING:   return this._kingMoves(row, col);
            default: return [];
        }
    }

    _pawnMoves(row, col) {
        const piece   = this.board[row][col];
        const dir     = piece.color === COLOR.WHITE ? -1 : 1;
        const start   = piece.color === COLOR.WHITE ? 6 : 1;
        const moves   = [];
        const mk      = (tr, tc, type = 'normal') =>
            ({ fromRow: row, fromCol: col, toRow: tr, toCol: tc, type });

        /* Forward 1 */
        if (this._inBounds(row + dir, col) && !this.board[row + dir][col]) {
            moves.push(mk(row + dir, col));
            /* Forward 2 from start */
            if (row === start && !this.board[row + 2 * dir][col]) {
                moves.push(mk(row + 2 * dir, col));
            }
        }

        /* Captures */
        for (const dc of [-1, 1]) {
            const nr = row + dir, nc = col + dc;
            if (!this._inBounds(nr, nc)) continue;
            const target = this.board[nr][nc];
            if (target && target.color !== piece.color) {
                moves.push(mk(nr, nc));
            }
            /* En passant */
            if (this.enPassantTarget &&
                this.enPassantTarget.row === nr &&
                this.enPassantTarget.col === nc) {
                moves.push(mk(nr, nc, 'enpassant'));
            }
        }
        return moves;
    }

    _knightMoves(row, col) {
        const piece = this.board[row][col];
        const offsets = [[-2,-1],[-2,1],[-1,-2],[-1,2],[1,-2],[1,2],[2,-1],[2,1]];
        return offsets
            .map(([dr, dc]) => [row + dr, col + dc])
            .filter(([r, c]) => this._inBounds(r, c))
            .filter(([r, c]) => {
                const t = this.board[r][c];
                return !t || t.color !== piece.color;
            })
            .map(([r, c]) => ({ fromRow: row, fromCol: col, toRow: r, toCol: c, type: 'normal' }));
    }

    _slide(row, col, dirs) {
        const piece = this.board[row][col];
        const moves = [];
        for (const [dr, dc] of dirs) {
            let r = row + dr, c = col + dc;
            while (this._inBounds(r, c)) {
                const t = this.board[r][c];
                if (t) {
                    if (t.color !== piece.color) {
                        moves.push({ fromRow: row, fromCol: col, toRow: r, toCol: c, type: 'normal' });
                    }
                    break;
                }
                moves.push({ fromRow: row, fromCol: col, toRow: r, toCol: c, type: 'normal' });
                r += dr; c += dc;
            }
        }
        return moves;
    }

    _kingMoves(row, col) {
        const piece = this.board[row][col];
        const moves = [];
        const opp   = piece.color === COLOR.WHITE ? COLOR.BLACK : COLOR.WHITE;

        /* Normal one-step moves */
        for (let dr = -1; dr <= 1; dr++) {
            for (let dc = -1; dc <= 1; dc++) {
                if (dr === 0 && dc === 0) continue;
                const r = row + dr, c = col + dc;
                if (!this._inBounds(r, c)) continue;
                const t = this.board[r][c];
                if (!t || t.color !== piece.color) {
                    moves.push({ fromRow: row, fromCol: col, toRow: r, toCol: c, type: 'normal' });
                }
            }
        }

        /* Castling */
        if (!piece.hasMoved && !this._isAttacked(row, col, opp)) {
            /* Kingside */
            const rKS = this.board[row][7];
            if (rKS && rKS.type === PIECE.ROOK && !rKS.hasMoved &&
                !this.board[row][5] && !this.board[row][6] &&
                !this._isAttacked(row, 5, opp) && !this._isAttacked(row, 6, opp)) {
                moves.push({ fromRow: row, fromCol: col, toRow: row, toCol: 6, type: 'castle', rookFromCol: 7, rookToCol: 5 });
            }
            /* Queenside */
            const rQS = this.board[row][0];
            if (rQS && rQS.type === PIECE.ROOK && !rQS.hasMoved &&
                !this.board[row][1] && !this.board[row][2] && !this.board[row][3] &&
                !this._isAttacked(row, 3, opp) && !this._isAttacked(row, 2, opp)) {
                moves.push({ fromRow: row, fromCol: col, toRow: row, toCol: 2, type: 'castle', rookFromCol: 0, rookToCol: 3 });
            }
        }

        return moves;
    }

    /* Is square (r,c) attacked by any piece of byColor? */
    _isAttacked(r, c, byColor) {
        for (let br = 0; br < 8; br++) {
            for (let bc = 0; bc < 8; bc++) {
                const p = this.board[br][bc];
                if (p && p.color === byColor) {
                    /* Use pseudo-legal generation but skip castling to avoid infinite recursion */
                    const attacks = p.type === PIECE.KING
                        ? this._kingAttacks(br, bc)
                        : this._pseudoLegal(br, bc);
                    if (attacks.some(m => m.toRow === r && m.toCol === c)) return true;
                }
            }
        }
        return false;
    }

    /* King moves without castling (used by _isAttacked to avoid recursion) */
    _kingAttacks(row, col) {
        const piece = this.board[row][col];
        const moves = [];
        for (let dr = -1; dr <= 1; dr++) {
            for (let dc = -1; dc <= 1; dc++) {
                if (dr === 0 && dc === 0) continue;
                const r = row + dr, c = col + dc;
                if (!this._inBounds(r, c)) continue;
                const t = this.board[r][c];
                if (!t || t.color !== piece.color) {
                    moves.push({ fromRow: row, fromCol: col, toRow: r, toCol: c, type: 'normal' });
                }
            }
        }
        return moves;
    }

    /* ── Move execution ── */

    _applyMove(move, advanceTurn = false) {
        const piece    = this.board[move.fromRow][move.fromCol];
        const captured = this.board[move.toRow][move.toCol];
        const prevEP   = this.enPassantTarget;

        if (move.type === 'castle') {
            /* Move king */
            this.board[move.toRow][move.toCol]     = { ...piece, hasMoved: true };
            this.board[move.fromRow][move.fromCol]  = null;
            /* Move rook */
            const rook = this.board[move.fromRow][move.rookFromCol];
            this.board[move.fromRow][move.rookToCol]   = { ...rook, hasMoved: true };
            this.board[move.fromRow][move.rookFromCol] = null;
            this.enPassantTarget = null;

        } else if (move.type === 'enpassant') {
            this.board[move.toRow][move.toCol]    = { ...piece, hasMoved: true };
            this.board[move.fromRow][move.fromCol] = null;
            /* Remove the captured pawn (it's on the same row as the moving pawn) */
            this.board[move.fromRow][move.toCol]   = null;
            this.enPassantTarget = null;

        } else if (move.promotion) {
            this.board[move.toRow][move.toCol]    = { type: move.promotion, color: piece.color, hasMoved: true };
            this.board[move.fromRow][move.fromCol] = null;
            this.enPassantTarget = null;

        } else {
            this.board[move.toRow][move.toCol]    = { ...piece, hasMoved: true };
            this.board[move.fromRow][move.fromCol] = null;

            /* Set en-passant target for 2-square pawn push */
            if (piece.type === PIECE.PAWN && Math.abs(move.toRow - move.fromRow) === 2) {
                this.enPassantTarget = {
                    row: (move.fromRow + move.toRow) / 2,
                    col: move.fromCol,
                };
            } else {
                this.enPassantTarget = null;
            }
        }

        if (advanceTurn) this._advanceTurn();

        /* Return actual captured piece (en-passant captured pawn is on different square) */
        if (move.type === 'enpassant') {
            return { type: PIECE.PAWN, color: piece.color === COLOR.WHITE ? COLOR.BLACK : COLOR.WHITE };
        }
        return captured;
    }

    _unapplyMove(move, captured) {
        const piece = this.board[move.toRow][move.toCol];

        if (move.type === 'castle') {
            /* Restore king */
            const origPiece = { ...piece, hasMoved: false };
            this.board[move.fromRow][move.fromCol] = origPiece;
            this.board[move.toRow][move.toCol]     = null;
            /* Restore rook */
            const rook = this.board[move.fromRow][move.rookToCol];
            this.board[move.fromRow][move.rookFromCol] = { ...rook, hasMoved: false };
            this.board[move.fromRow][move.rookToCol]   = null;

        } else if (move.type === 'enpassant') {
            const dir = piece.color === COLOR.WHITE ? 1 : -1; // reverse
            this.board[move.fromRow][move.fromCol] = { ...piece, hasMoved: move.fromRow !== (piece.color === COLOR.WHITE ? 6 : 1) };
            this.board[move.toRow][move.toCol]     = null;
            /* Restore the captured pawn */
            this.board[move.fromRow][move.toCol] = captured;

        } else if (move.promotion) {
            /* Restore pawn */
            this.board[move.fromRow][move.fromCol] = { type: PIECE.PAWN, color: piece.color, hasMoved: true };
            this.board[move.toRow][move.toCol]     = captured || null;

        } else {
            const wasFirstMove = !piece.hasMoved ||
                (piece.type === PIECE.PAWN &&
                    ((piece.color === COLOR.WHITE && move.fromRow === 6) ||
                     (piece.color === COLOR.BLACK && move.fromRow === 1)));
            this.board[move.fromRow][move.fromCol] = { ...piece, hasMoved: !wasFirstMove };
            this.board[move.toRow][move.toCol]     = captured || null;
        }

        /* Restore en-passant target from previous history entry (if any) */
        const prev = this.moveHistory[this.moveHistory.length - 1];
        if (prev) {
            const pp = this.board[prev.move.fromRow][prev.move.fromCol] ||
                       this.board[prev.move.toRow][prev.move.toCol];
            if (pp && pp.type === PIECE.PAWN &&
                Math.abs(prev.move.toRow - prev.move.fromRow) === 2) {
                this.enPassantTarget = {
                    row: (prev.move.fromRow + prev.move.toRow) / 2,
                    col: prev.move.fromCol,
                };
            } else {
                this.enPassantTarget = null;
            }
        } else {
            this.enPassantTarget = null;
        }
    }

    /* ── Snapshot (for check-testing) ── */

    _snapshot() {
        return {
            board:           this.board.map(row => row.map(cell => cell ? { ...cell } : null)),
            currentTurn:     this.currentTurn,
            enPassantTarget: this.enPassantTarget ? { ...this.enPassantTarget } : null,
            gameStatus:      this.gameStatus,
        };
    }

    _restore(snap) {
        this.board           = snap.board;
        this.currentTurn     = snap.currentTurn;
        this.enPassantTarget = snap.enPassantTarget;
        this.gameStatus      = snap.gameStatus;
    }

    /* ── Utilities ── */

    _inBounds(r, c) { return r >= 0 && r < 8 && c >= 0 && c < 8; }

    _findKing(color) {
        for (let r = 0; r < 8; r++) {
            for (let c = 0; c < 8; c++) {
                const p = this.board[r][c];
                if (p && p.type === PIECE.KING && p.color === color) return { row: r, col: c };
            }
        }
        return null;
    }

    /* Simple algebraic notation for move history display */
    _toAlgebraic(move, captured) {
        const files = 'abcdefgh';
        const piece = this.board[move.toRow][move.toCol] || this.board[move.fromRow][move.fromCol];
        if (!piece) return '';

        if (move.type === 'castle') {
            return move.toCol === 6 ? 'O-O' : 'O-O-O';
        }

        const symbols = { pawn: '', rook: 'R', knight: 'N', bishop: 'B', queen: 'Q', king: 'K' };
        let note = symbols[piece.type] || '';
        if (piece.type === PIECE.PAWN && (captured || move.type === 'enpassant')) {
            note = files[move.fromCol];
        }
        note += (captured || move.type === 'enpassant') ? 'x' : '';
        note += files[move.toCol] + (8 - move.toRow);
        if (move.promotion) note += '=' + symbols[move.promotion].toUpperCase();
        return note;
    }

    /* ── Evaluation (used by AI) ── */
    evaluate() {
        if (this.gameStatus === 'checkmate') {
            return this.currentTurn === COLOR.WHITE ? -99999 : 99999;
        }
        if (this.gameStatus === 'stalemate') return 0;

        let score = 0;
        for (let r = 0; r < 8; r++) {
            for (let c = 0; c < 8; c++) {
                const p = this.board[r][c];
                if (!p) continue;
                const val = PIECE_VALUE[p.type];
                const pstTable = PST[p.type === PIECE.KING ? 'king_mid' : p.type];
                const row = p.color === COLOR.WHITE ? r : flipRow(r);
                const pst = pstTable ? pstTable[row][c] : 0;
                score += p.color === COLOR.WHITE ? (val + pst) : -(val + pst);
            }
        }
        return score;
    }
}

/* Make accessible globally */
if (typeof module !== 'undefined') module.exports = { ChessGame, PIECE, COLOR, PIECE_VALUE };
