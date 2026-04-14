'use strict';

/* ============================================================
   ai.js  –  Minimax AI with Alpha-Beta Pruning
   ============================================================
   Difficulty levels map to search depth:
     Easy   → depth 1  (random from top moves)
     Medium → depth 3
     Hard   → depth 4  (+ iterative deepening hint)
   ============================================================ */

class ChessAI {
    /**
     * @param {number} depth  Search depth (1 = easy, 3 = medium, 4-5 = hard)
     */
    constructor(depth = 3) {
        this.depth   = depth;
        this.nodes   = 0; // diagnostics
    }

    /**
     * Return the best move for `color` in the given game state.
     * Returns a move object { fromRow, fromCol, toRow, toCol, type, … }
     * or null if no moves are available.
     */
    getBestMove(game, color) {
        this.nodes = 0;

        const moves = game.getAllLegalMoves(color);
        if (moves.length === 0) return null;

        /* For Easy (depth 1) add randomness so AI isn't perfectly greedy */
        if (this.depth === 1) {
            return this._easyMove(game, color, moves);
        }

        const isMaximising = color === COLOR.WHITE;
        let bestMove  = null;
        let bestScore = isMaximising ? -Infinity : Infinity;
        let alpha = -Infinity, beta = Infinity;

        /* Move ordering: captures / promotions first for better pruning */
        const ordered = this._orderMoves(game, moves);

        for (const move of ordered) {
            const snap = game._snapshot();
            game._applyMove(move, /* advanceTurn= */ true);
            game._updateStatus();

            const score = this._minimax(game, this.depth - 1, alpha, beta, !isMaximising);

            game._restore(snap);
            game._updateStatus();

            if (isMaximising) {
                if (score > bestScore) { bestScore = score; bestMove = move; }
                alpha = Math.max(alpha, bestScore);
            } else {
                if (score < bestScore) { bestScore = score; bestMove = move; }
                beta = Math.min(beta, bestScore);
            }
            /* No top-level pruning so we get the actual best move */
        }

        return bestMove;
    }

    /* ── Private ── */

    _easyMove(game, color, moves) {
        /* Pick a random move, but prefer captures 70 % of the time */
        const captures = moves.filter(m => game.board[m.toRow][m.toCol] !== null);
        if (captures.length > 0 && Math.random() < 0.7) {
            return captures[Math.floor(Math.random() * captures.length)];
        }
        return moves[Math.floor(Math.random() * moves.length)];
    }

    _minimax(game, depth, alpha, beta, isMaximising) {
        this.nodes++;

        /* Terminal / leaf */
        if (depth === 0 || game.gameStatus === 'checkmate' || game.gameStatus === 'stalemate') {
            return game.evaluate();
        }

        const color = isMaximising ? COLOR.WHITE : COLOR.BLACK;
        const moves = game.getAllLegalMoves(color);

        if (moves.length === 0) return game.evaluate();

        const ordered = this._orderMoves(game, moves);

        if (isMaximising) {
            let maxScore = -Infinity;
            for (const move of ordered) {
                const snap = game._snapshot();
                game._applyMove(move, true);
                game._updateStatus();

                const score = this._minimax(game, depth - 1, alpha, beta, false);

                game._restore(snap);
                game._updateStatus();

                maxScore = Math.max(maxScore, score);
                alpha    = Math.max(alpha, score);
                if (beta <= alpha) break; // β cut-off
            }
            return maxScore;
        } else {
            let minScore = Infinity;
            for (const move of ordered) {
                const snap = game._snapshot();
                game._applyMove(move, true);
                game._updateStatus();

                const score = this._minimax(game, depth - 1, alpha, beta, true);

                game._restore(snap);
                game._updateStatus();

                minScore = Math.min(minScore, score);
                beta     = Math.min(beta, score);
                if (beta <= alpha) break; // α cut-off
            }
            return minScore;
        }
    }

    /**
     * Order moves for better alpha-beta pruning:
     *   1. Checkmate / check-giving moves
     *   2. Captures (sorted by MVV-LVA: most valuable victim, least valuable attacker)
     *   3. Promotions
     *   4. Other moves
     */
    _orderMoves(game, moves) {
        const score = (m) => {
            /* Promotions */
            if (m.promotion) return 10000 + PIECE_VALUE[m.promotion];
            const victim = game.board[m.toRow][m.toCol];
            if (victim) {
                const attacker = game.board[m.fromRow][m.fromCol];
                /* MVV-LVA */
                return 1000 + PIECE_VALUE[victim.type] - (attacker ? PIECE_VALUE[attacker.type] / 100 : 0);
            }
            if (m.type === 'enpassant') return 900;
            return 0;
        };
        return [...moves].sort((a, b) => score(b) - score(a));
    }
}
