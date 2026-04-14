package chess.model;

import java.util.List;

public abstract class Piece {
    protected final PieceType type;
    protected final PieceColor color;
    protected boolean hasMoved;

    protected Piece(PieceType type, PieceColor color) {
        this.type = type;
        this.color = color;
        this.hasMoved = false;
    }

    public abstract List<Move> getPseudoLegalMoves(Board board, int row, int col);

    public int getValue() {
        return switch (type) {
            case PAWN -> 100;
            case KNIGHT -> 320;
            case BISHOP -> 330;
            case ROOK -> 500;
            case QUEEN -> 900;
            case KING -> 20000;
        };
    }

    public String getSymbol() {
        if (color == PieceColor.WHITE) {
            return switch (type) {
                case PAWN -> "♙";
                case ROOK -> "♖";
                case KNIGHT -> "♘";
                case BISHOP -> "♗";
                case QUEEN -> "♕";
                case KING -> "♔";
            };
        } else {
            return switch (type) {
                case PAWN -> "♟";
                case ROOK -> "♜";
                case KNIGHT -> "♞";
                case BISHOP -> "♝";
                case QUEEN -> "♛";
                case KING -> "♚";
            };
        }
    }

    public PieceType getType() { return type; }
    public PieceColor getColor() { return color; }
    public boolean hasMoved() { return hasMoved; }
    public void setHasMoved(boolean hasMoved) { this.hasMoved = hasMoved; }

    public abstract Piece copy();
}
