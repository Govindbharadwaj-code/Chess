package chess.model;

import java.util.ArrayList;
import java.util.List;

public class Knight extends Piece {

    public Knight(PieceColor color) {
        super(PieceType.KNIGHT, color);
    }

    @Override
    public List<Move> getPseudoLegalMoves(Board board, int row, int col) {
        List<Move> moves = new ArrayList<>();
        int[][] offsets = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
            {1, -2}, {1, 2}, {2, -1}, {2, 1}
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
        return moves;
    }

    @Override
    public Piece copy() {
        Knight copy = new Knight(color);
        copy.hasMoved = this.hasMoved;
        return copy;
    }
}
