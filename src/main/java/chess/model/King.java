package chess.model;

import java.util.ArrayList;
import java.util.List;

public class King extends Piece {

    public King(PieceColor color) {
        super(PieceType.KING, color);
    }

    @Override
    public List<Move> getPseudoLegalMoves(Board board, int row, int col) {
        List<Move> moves = new ArrayList<>();

        // Normal king moves (one square in any direction)
        int[][] offsets = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1},  {1, 0},  {1, 1}
        };

        for (int[] offset : offsets) {
            int r = row + offset[0];
            int c = col + offset[1];
            if (board.isInBounds(r, c)) {
                Piece target = board.getPiece(r, c);
                if (target == null) {
                    moves.add(new Move(row, col, r, c, this));
                } else if (target.getColor() != color) {
                    Move move = new Move(row, col, r, c, this);
                    move.captured = target;
                    moves.add(move);
                }
            }
        }

        // Castling
        if (!hasMoved && !board.isInCheck(color)) {
            // Kingside castling
            Piece kingsideRook = board.getPiece(row, 7);
            if (kingsideRook != null && kingsideRook.getType() == PieceType.ROOK
                    && kingsideRook.getColor() == color && !kingsideRook.hasMoved()) {
                // Check squares between king and rook are empty
                if (board.getPiece(row, 5) == null && board.getPiece(row, 6) == null) {
                    // Check king doesn't pass through or land on attacked square
                    if (!board.isSquareAttacked(row, 5, color.opposite())
                            && !board.isSquareAttacked(row, 6, color.opposite())) {
                        moves.add(new Move(row, col, row, 6, this, MoveType.CASTLING_KINGSIDE));
                    }
                }
            }

            // Queenside castling
            Piece queensideRook = board.getPiece(row, 0);
            if (queensideRook != null && queensideRook.getType() == PieceType.ROOK
                    && queensideRook.getColor() == color && !queensideRook.hasMoved()) {
                // Check squares between king and rook are empty
                if (board.getPiece(row, 1) == null && board.getPiece(row, 2) == null
                        && board.getPiece(row, 3) == null) {
                    // Check king doesn't pass through or land on attacked square
                    if (!board.isSquareAttacked(row, 3, color.opposite())
                            && !board.isSquareAttacked(row, 2, color.opposite())) {
                        moves.add(new Move(row, col, row, 2, this, MoveType.CASTLING_QUEENSIDE));
                    }
                }
            }
        }

        return moves;
    }

    @Override
    public Piece copy() {
        King copy = new King(color);
        copy.hasMoved = this.hasMoved;
        return copy;
    }
}
