package chess.model;

import java.util.ArrayList;
import java.util.List;

public class Queen extends Piece {

    public Queen(PieceColor color) {
        super(PieceType.QUEEN, color);
    }

    @Override
    public List<Move> getPseudoLegalMoves(Board board, int row, int col) {
        List<Move> moves = new ArrayList<>();
        // Queen = Rook + Bishop moves
        int[][] directions = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1},
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
        };

        for (int[] dir : directions) {
            int r = row + dir[0];
            int c = col + dir[1];
            while (board.isInBounds(r, c)) {
                Piece target = board.getPiece(r, c);
                if (target == null) {
                    moves.add(new Move(row, col, r, c, this));
                } else {
                    if (target.getColor() != color) {
                        Move move = new Move(row, col, r, c, this);
                        move.captured = target;
                        moves.add(move);
                    }
                    break;
                }
                r += dir[0];
                c += dir[1];
            }
        }
        return moves;
    }

    @Override
    public Piece copy() {
        Queen copy = new Queen(color);
        copy.hasMoved = this.hasMoved;
        return copy;
    }
}
