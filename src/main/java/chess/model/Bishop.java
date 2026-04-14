package chess.model;

import java.util.ArrayList;
import java.util.List;

public class Bishop extends Piece {

    public Bishop(PieceColor color) {
        super(PieceType.BISHOP, color);
    }

    @Override
    public List<Move> getPseudoLegalMoves(Board board, int row, int col) {
        List<Move> moves = new ArrayList<>();
        int[][] directions = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

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
        Bishop copy = new Bishop(color);
        copy.hasMoved = this.hasMoved;
        return copy;
    }
}
