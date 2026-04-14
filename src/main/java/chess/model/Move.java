package chess.model;

public class Move {
    public final int fromRow, fromCol, toRow, toCol;
    public final Piece piece;
    public Piece captured;
    public final MoveType type;
    public PieceType promotionPiece;

    public Move(int fromRow, int fromCol, int toRow, int toCol, Piece piece, MoveType type) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.piece = piece;
        this.type = type;
        this.promotionPiece = PieceType.QUEEN;
    }

    public Move(int fromRow, int fromCol, int toRow, int toCol, Piece piece) {
        this(fromRow, fromCol, toRow, toCol, piece, MoveType.NORMAL);
    }

    public int getFromRow() { return fromRow; }
    public int getFromCol() { return fromCol; }
    public int getToRow() { return toRow; }
    public int getToCol() { return toCol; }
    public Piece getPiece() { return piece; }
    public Piece getCaptured() { return captured; }
    public MoveType getType() { return type; }
    public PieceType getPromotionPiece() { return promotionPiece; }

    @Override
    public String toString() {
        return "Move{" + fromRow + "," + fromCol + " -> " + toRow + "," + toCol + " type=" + type + "}";
    }
}
