package chess.model;

import java.util.ArrayList;
import java.util.List;

public class Rook extends Piece {

    public Rook(PieceColor color) {
        super(PieceType.ROOK, color);
    }

    @Override
    public List<Move> getPseudoLegalMoves(Board board, int row, int col) {
        List<Move> moves = new ArrayList<>();
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

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
        Rook copy = new Rook(color);
        copy.hasMoved = this.hasMoved;
        return copy;
    }
}
