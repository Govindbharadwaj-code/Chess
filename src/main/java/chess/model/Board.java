package chess.model;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private Piece[][] squares;
    private int[] enPassantTarget; // {row, col} of the square where capturing pawn lands, or null

    public Board() {
        squares = new Piece[8][8];
    }

    public void initialize() {
        squares = new Piece[8][8];
        enPassantTarget = null;

        // Black pieces (row 0)
        squares[0][0] = new Rook(PieceColor.BLACK);
        squares[0][1] = new Knight(PieceColor.BLACK);
        squares[0][2] = new Bishop(PieceColor.BLACK);
        squares[0][3] = new Queen(PieceColor.BLACK);
        squares[0][4] = new King(PieceColor.BLACK);
        squares[0][5] = new Bishop(PieceColor.BLACK);
        squares[0][6] = new Knight(PieceColor.BLACK);
        squares[0][7] = new Rook(PieceColor.BLACK);

        // Black pawns (row 1)
        for (int c = 0; c < 8; c++) {
            squares[1][c] = new Pawn(PieceColor.BLACK);
        }

        // White pawns (row 6)
        for (int c = 0; c < 8; c++) {
            squares[6][c] = new Pawn(PieceColor.WHITE);
        }

        // White pieces (row 7)
        squares[7][0] = new Rook(PieceColor.WHITE);
        squares[7][1] = new Knight(PieceColor.WHITE);
        squares[7][2] = new Bishop(PieceColor.WHITE);
        squares[7][3] = new Queen(PieceColor.WHITE);
        squares[7][4] = new King(PieceColor.WHITE);
        squares[7][5] = new Bishop(PieceColor.WHITE);
        squares[7][6] = new Knight(PieceColor.WHITE);
        squares[7][7] = new Rook(PieceColor.WHITE);
    }

    public List<Move> getLegalMoves(PieceColor color) {
        List<Move> legalMoves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = squares[r][c];
                if (piece != null && piece.getColor() == color) {
                    legalMoves.addAll(getLegalMovesForPiece(r, c));
                }
            }
        }
        return legalMoves;
    }

    public List<Move> getLegalMovesForPiece(int row, int col) {
        List<Move> legal = new ArrayList<>();
        Piece piece = squares[row][col];
        if (piece == null) return legal;

        List<Move> pseudoLegal = piece.getPseudoLegalMoves(this, row, col);
        for (Move move : pseudoLegal) {
            if (isLegalMove(move)) {
                legal.add(move);
            }
        }
        return legal;
    }

    private boolean isLegalMove(Move move) {
        // Make the move on this board (temporarily), check if own king is in check, then undo
        Piece capturedPiece = move.captured;
        int[] prevEnPassant = enPassantTarget != null ? new int[]{enPassantTarget[0], enPassantTarget[1]} : null;
        boolean prevPieceHasMoved = move.piece.hasMoved();
        boolean prevRookHasMoved = false;

        Piece rook = null;
        int rookRow = -1, rookFromCol = -1, rookToCol = -1;
        if (move.type == MoveType.CASTLING_KINGSIDE || move.type == MoveType.CASTLING_QUEENSIDE) {
            rookRow = move.fromRow;
            rookFromCol = (move.type == MoveType.CASTLING_KINGSIDE) ? 7 : 0;
            rook = squares[rookRow][rookFromCol];
            if (rook != null) prevRookHasMoved = rook.hasMoved();
        }

        makeMoveInternal(move);
        boolean inCheck = isInCheck(move.piece.getColor());
        undoMove(move, capturedPiece, prevEnPassant, prevPieceHasMoved, prevRookHasMoved);

        return !inCheck;
    }

    public void makeMove(Move move) {
        makeMoveInternal(move);
    }

    private void makeMoveInternal(Move move) {
        int fromRow = move.fromRow, fromCol = move.fromCol;
        int toRow = move.toRow, toCol = move.toCol;

        enPassantTarget = null; // reset en passant

        switch (move.type) {
            case NORMAL -> {
                // Check if pawn moved 2 squares (set en passant target)
                if (move.piece.getType() == PieceType.PAWN && Math.abs(toRow - fromRow) == 2) {
                    int epRow = (fromRow + toRow) / 2;
                    enPassantTarget = new int[]{epRow, fromCol};
                }
                squares[toRow][toCol] = squares[fromRow][fromCol];
                squares[fromRow][fromCol] = null;
                squares[toRow][toCol].setHasMoved(true);
            }
            case EN_PASSANT -> {
                squares[toRow][toCol] = squares[fromRow][fromCol];
                squares[fromRow][fromCol] = null;
                squares[toRow][toCol].setHasMoved(true);
                // Remove the captured pawn (same row as from, same col as to)
                squares[fromRow][toCol] = null;
            }
            case CASTLING_KINGSIDE -> {
                // Move king
                squares[toRow][toCol] = squares[fromRow][fromCol];
                squares[fromRow][fromCol] = null;
                squares[toRow][toCol].setHasMoved(true);
                // Move rook from col 7 to col 5
                squares[fromRow][5] = squares[fromRow][7];
                squares[fromRow][7] = null;
                if (squares[fromRow][5] != null) squares[fromRow][5].setHasMoved(true);
            }
            case CASTLING_QUEENSIDE -> {
                // Move king
                squares[toRow][toCol] = squares[fromRow][fromCol];
                squares[fromRow][fromCol] = null;
                squares[toRow][toCol].setHasMoved(true);
                // Move rook from col 0 to col 3
                squares[fromRow][3] = squares[fromRow][0];
                squares[fromRow][0] = null;
                if (squares[fromRow][3] != null) squares[fromRow][3].setHasMoved(true);
            }
            case PROMOTION -> {
                PieceType promType = move.promotionPiece != null ? move.promotionPiece : PieceType.QUEEN;
                Piece promoted = createPiece(promType, move.piece.getColor());
                promoted.setHasMoved(true);
                squares[toRow][toCol] = promoted;
                squares[fromRow][fromCol] = null;
            }
        }
    }

    private Piece createPiece(PieceType type, PieceColor color) {
        return switch (type) {
            case PAWN -> new Pawn(color);
            case ROOK -> new Rook(color);
            case KNIGHT -> new Knight(color);
            case BISHOP -> new Bishop(color);
            case QUEEN -> new Queen(color);
            case KING -> new King(color);
        };
    }

    public void undoMove(Move move, Piece capturedPiece, int[] prevEnPassantTarget,
                         boolean prevPieceHasMoved, boolean prevRookHasMoved) {
        int fromRow = move.fromRow, fromCol = move.fromCol;
        int toRow = move.toRow, toCol = move.toCol;

        enPassantTarget = prevEnPassantTarget;

        switch (move.type) {
            case NORMAL, PROMOTION -> {
                squares[fromRow][fromCol] = move.piece;
                move.piece.setHasMoved(prevPieceHasMoved);
                squares[toRow][toCol] = capturedPiece;
            }
            case EN_PASSANT -> {
                squares[fromRow][fromCol] = move.piece;
                move.piece.setHasMoved(prevPieceHasMoved);
                squares[toRow][toCol] = null;
                // Restore captured pawn
                squares[fromRow][toCol] = capturedPiece;
            }
            case CASTLING_KINGSIDE -> {
                // Restore king
                squares[fromRow][fromCol] = move.piece;
                move.piece.setHasMoved(prevPieceHasMoved);
                squares[toRow][toCol] = null;
                // Restore rook
                squares[fromRow][7] = squares[fromRow][5];
                squares[fromRow][5] = null;
                if (squares[fromRow][7] != null) squares[fromRow][7].setHasMoved(prevRookHasMoved);
            }
            case CASTLING_QUEENSIDE -> {
                // Restore king
                squares[fromRow][fromCol] = move.piece;
                move.piece.setHasMoved(prevPieceHasMoved);
                squares[toRow][toCol] = null;
                // Restore rook
                squares[fromRow][0] = squares[fromRow][3];
                squares[fromRow][3] = null;
                if (squares[fromRow][0] != null) squares[fromRow][0].setHasMoved(prevRookHasMoved);
            }
        }
    }

    public boolean isInCheck(PieceColor color) {
        int[] kingPos = findKing(color);
        if (kingPos == null) return false;
        return isSquareAttacked(kingPos[0], kingPos[1], color.opposite());
    }

    public boolean isSquareAttacked(int row, int col, PieceColor attackerColor) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = squares[r][c];
                if (piece != null && piece.getColor() == attackerColor) {
                    List<Move> moves = piece.getPseudoLegalMoves(this, r, c);
                    for (Move move : moves) {
                        if (move.toRow == row && move.toCol == col) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public int[] findKing(PieceColor color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = squares[r][c];
                if (piece != null && piece.getType() == PieceType.KING && piece.getColor() == color) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    public Piece getPiece(int row, int col) {
        return squares[row][col];
    }

    public void setPiece(int row, int col, Piece piece) {
        squares[row][col] = piece;
    }

    public boolean isInBounds(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    public int[] getEnPassantTarget() {
        return enPassantTarget;
    }

    public void setEnPassantTarget(int[] target) {
        this.enPassantTarget = target;
    }

    public Board copy() {
        Board copy = new Board();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (squares[r][c] != null) {
                    copy.squares[r][c] = squares[r][c].copy();
                }
            }
        }
        if (enPassantTarget != null) {
            copy.enPassantTarget = new int[]{enPassantTarget[0], enPassantTarget[1]};
        }
        return copy;
    }
}
