package chess.model;

import java.util.ArrayList;
import java.util.List;

public class Pawn extends Piece {

    public Pawn(PieceColor color) {
        super(PieceType.PAWN, color);
    }

    @Override
    public List<Move> getPseudoLegalMoves(Board board, int row, int col) {
        List<Move> moves = new ArrayList<>();
        int direction = (color == PieceColor.WHITE) ? -1 : 1;
        int startRow = (color == PieceColor.WHITE) ? 6 : 1;
        int promotionRow = (color == PieceColor.WHITE) ? 0 : 7;

        // One square forward
        int newRow = row + direction;
        if (board.isInBounds(newRow, col) && board.getPiece(newRow, col) == null) {
            if (newRow == promotionRow) {
                moves.add(new Move(row, col, newRow, col, this, MoveType.PROMOTION));
            } else {
                moves.add(new Move(row, col, newRow, col, this));
            }

            // Two squares forward from starting position
            if (row == startRow) {
                int twoRow = row + 2 * direction;
                if (board.isInBounds(twoRow, col) && board.getPiece(twoRow, col) == null) {
                    moves.add(new Move(row, col, twoRow, col, this));
                }
            }
        }

        // Diagonal captures
        int[] captureColOffsets = {-1, 1};
        for (int colOffset : captureColOffsets) {
            int captureCol = col + colOffset;
            if (board.isInBounds(newRow, captureCol)) {
                Piece target = board.getPiece(newRow, captureCol);
                if (target != null && target.getColor() != color) {
                    Move move;
                    if (newRow == promotionRow) {
                        move = new Move(row, col, newRow, captureCol, this, MoveType.PROMOTION);
                    } else {
                        move = new Move(row, col, newRow, captureCol, this);
                    }
                    move.captured = target;
                    moves.add(move);
                }

                // En passant
                int[] epTarget = board.getEnPassantTarget();
                if (epTarget != null && epTarget[0] == newRow && epTarget[1] == captureCol) {
                    Move epMove = new Move(row, col, newRow, captureCol, this, MoveType.EN_PASSANT);
                    // The captured pawn is on the same row as this pawn but at captureCol
                    epMove.captured = board.getPiece(row, captureCol);
                    moves.add(epMove);
                }
            }
        }

        return moves;
    }

    @Override
    public Piece copy() {
        Pawn copy = new Pawn(color);
        copy.hasMoved = this.hasMoved;
        return copy;
    }
}
