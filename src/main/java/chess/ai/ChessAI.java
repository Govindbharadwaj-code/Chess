package chess.ai;

import chess.model.*;

import java.util.List;

public class ChessAI {
    private final PieceColor aiColor;
    private static final int MAX_DEPTH = 4;
    private static final int INF = Integer.MAX_VALUE / 2;

    // Piece-square tables (for white; mirror for black)
    private static final int[] PAWN_TABLE = {
        0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
         5,  5, 10, 25, 25, 10,  5,  5,
         0,  0,  0, 20, 20,  0,  0,  0,
         5, -5,-10,  0,  0,-10, -5,  5,
         5, 10, 10,-20,-20, 10, 10,  5,
         0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] KNIGHT_TABLE = {
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    };

    private static final int[] BISHOP_TABLE = {
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    };

    private static final int[] ROOK_TABLE = {
         0,  0,  0,  0,  0,  0,  0,  0,
         5, 10, 10, 10, 10, 10, 10,  5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
         0,  0,  0,  5,  5,  0,  0,  0
    };

    private static final int[] QUEEN_TABLE = {
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
         -5,  0,  5,  5,  5,  5,  0, -5,
          0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    };

    private static final int[] KING_MIDDLE_TABLE = {
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
         20, 20,  0,  0,  0,  0, 20, 20,
         20, 30, 10,  0,  0, 10, 30, 20
    };

    public ChessAI(PieceColor color) {
        this.aiColor = color;
    }

    public Move getBestMove(Board board) {
        List<Move> moves = board.getLegalMoves(aiColor);
        if (moves.isEmpty()) return null;

        Move bestMove = null;
        int bestScore = -INF;

        for (Move move : moves) {
            Piece captured = move.captured;
            int[] prevEP = board.getEnPassantTarget();
            int[] prevEPCopy = prevEP != null ? new int[]{prevEP[0], prevEP[1]} : null;
            boolean prevPieceMoved = move.piece.hasMoved();
            boolean prevRookMoved = false;

            if (move.type == MoveType.CASTLING_KINGSIDE || move.type == MoveType.CASTLING_QUEENSIDE) {
                int rookCol = (move.type == MoveType.CASTLING_KINGSIDE) ? 7 : 0;
                Piece rook = board.getPiece(move.fromRow, rookCol);
                if (rook != null) prevRookMoved = rook.hasMoved();
            }

            board.makeMove(move);
            int score = minimax(board, MAX_DEPTH - 1, -INF, INF, false);
            board.undoMove(move, captured, prevEPCopy, prevPieceMoved, prevRookMoved);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        return bestMove;
    }

    private int minimax(Board board, int depth, int alpha, int beta, boolean isMaximizing) {
        if (depth == 0) {
            return evaluate(board);
        }

        PieceColor currentColor = isMaximizing ? aiColor : aiColor.opposite();
        List<Move> moves = board.getLegalMoves(currentColor);

        if (moves.isEmpty()) {
            if (board.isInCheck(currentColor)) {
                return isMaximizing ? -INF + (MAX_DEPTH - depth) : INF - (MAX_DEPTH - depth);
            }
            return 0; // stalemate
        }

        if (isMaximizing) {
            int maxScore = -INF;
            for (Move move : moves) {
                Piece captured = move.captured;
                int[] prevEP = board.getEnPassantTarget();
                int[] prevEPCopy = prevEP != null ? new int[]{prevEP[0], prevEP[1]} : null;
                boolean prevPieceMoved = move.piece.hasMoved();
                boolean prevRookMoved = false;

                if (move.type == MoveType.CASTLING_KINGSIDE || move.type == MoveType.CASTLING_QUEENSIDE) {
                    int rookCol = (move.type == MoveType.CASTLING_KINGSIDE) ? 7 : 0;
                    Piece rook = board.getPiece(move.fromRow, rookCol);
                    if (rook != null) prevRookMoved = rook.hasMoved();
                }

                board.makeMove(move);
                int score = minimax(board, depth - 1, alpha, beta, false);
                board.undoMove(move, captured, prevEPCopy, prevPieceMoved, prevRookMoved);

                maxScore = Math.max(maxScore, score);
                alpha = Math.max(alpha, score);
                if (beta <= alpha) break;
            }
            return maxScore;
        } else {
            int minScore = INF;
            for (Move move : moves) {
                Piece captured = move.captured;
                int[] prevEP = board.getEnPassantTarget();
                int[] prevEPCopy = prevEP != null ? new int[]{prevEP[0], prevEP[1]} : null;
                boolean prevPieceMoved = move.piece.hasMoved();
                boolean prevRookMoved = false;

                if (move.type == MoveType.CASTLING_KINGSIDE || move.type == MoveType.CASTLING_QUEENSIDE) {
                    int rookCol = (move.type == MoveType.CASTLING_KINGSIDE) ? 7 : 0;
                    Piece rook = board.getPiece(move.fromRow, rookCol);
                    if (rook != null) prevRookMoved = rook.hasMoved();
                }

                board.makeMove(move);
                int score = minimax(board, depth - 1, alpha, beta, true);
                board.undoMove(move, captured, prevEPCopy, prevPieceMoved, prevRookMoved);

                minScore = Math.min(minScore, score);
                beta = Math.min(beta, score);
                if (beta <= alpha) break;
            }
            return minScore;
        }
    }

    private int evaluate(Board board) {
        int score = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board.getPiece(r, c);
                if (piece == null) continue;

                int value = piece.getValue();
                int tableBonus = getPieceSquareBonus(piece, r, c);

                if (piece.getColor() == aiColor) {
                    score += value + tableBonus;
                } else {
                    score -= value + tableBonus;
                }
            }
        }
        return score;
    }

    private int getPieceSquareBonus(Piece piece, int row, int col) {
        // Mirror table index for black (black is at top of board)
        int tableIndex;
        if (piece.getColor() == PieceColor.WHITE) {
            tableIndex = row * 8 + col;
        } else {
            tableIndex = (7 - row) * 8 + col;
        }

        return switch (piece.getType()) {
            case PAWN -> PAWN_TABLE[tableIndex];
            case KNIGHT -> KNIGHT_TABLE[tableIndex];
            case BISHOP -> BISHOP_TABLE[tableIndex];
            case ROOK -> ROOK_TABLE[tableIndex];
            case QUEEN -> QUEEN_TABLE[tableIndex];
            case KING -> KING_MIDDLE_TABLE[tableIndex];
        };
    }
}
