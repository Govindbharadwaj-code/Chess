package chess.model;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private Board board;
    private PieceColor currentTurn;
    private List<Move> moveHistory;
    private boolean gameOver;
    private String result;

    // State snapshots for undo
    private static class MoveSnapshot {
        final Move move;
        final Piece capturedPiece;
        final int[] prevEnPassantTarget;
        final boolean prevPieceHasMoved;
        final boolean prevRookHasMoved;

        MoveSnapshot(Move move, Piece capturedPiece, int[] prevEnPassantTarget,
                     boolean prevPieceHasMoved, boolean prevRookHasMoved) {
            this.move = move;
            this.capturedPiece = capturedPiece;
            this.prevEnPassantTarget = prevEnPassantTarget;
            this.prevPieceHasMoved = prevPieceHasMoved;
            this.prevRookHasMoved = prevRookHasMoved;
        }
    }

    private List<MoveSnapshot> snapshots;

    public GameState() {
        board = new Board();
        board.initialize();
        currentTurn = PieceColor.WHITE;
        moveHistory = new ArrayList<>();
        snapshots = new ArrayList<>();
        gameOver = false;
        result = "";
    }

    public void makeMove(Move move) {
        if (gameOver) return;

        // Save state before move
        Piece capturedPiece = move.captured;
        int[] prevEnPassant = board.getEnPassantTarget();
        int[] prevEnPassantCopy = prevEnPassant != null ? new int[]{prevEnPassant[0], prevEnPassant[1]} : null;
        boolean prevPieceHasMoved = move.piece.hasMoved();
        boolean prevRookHasMoved = false;

        if (move.type == MoveType.CASTLING_KINGSIDE || move.type == MoveType.CASTLING_QUEENSIDE) {
            int rookCol = (move.type == MoveType.CASTLING_KINGSIDE) ? 7 : 0;
            Piece rook = board.getPiece(move.fromRow, rookCol);
            if (rook != null) prevRookHasMoved = rook.hasMoved();
        }

        snapshots.add(new MoveSnapshot(move, capturedPiece, prevEnPassantCopy, prevPieceHasMoved, prevRookHasMoved));

        board.makeMove(move);
        moveHistory.add(move);
        currentTurn = currentTurn.opposite();

        // Check for game over conditions
        List<Move> nextMoves = board.getLegalMoves(currentTurn);
        if (nextMoves.isEmpty()) {
            gameOver = true;
            if (board.isInCheck(currentTurn)) {
                result = (currentTurn == PieceColor.WHITE) ? "Black wins by checkmate!" : "White wins by checkmate!";
            } else {
                result = "Draw by stalemate!";
            }
        }
    }

    public boolean canUndo() {
        return !snapshots.isEmpty();
    }

    public void undoLastMove() {
        if (snapshots.isEmpty()) return;

        MoveSnapshot snapshot = snapshots.remove(snapshots.size() - 1);
        moveHistory.remove(moveHistory.size() - 1);

        board.undoMove(snapshot.move, snapshot.capturedPiece, snapshot.prevEnPassantTarget,
                snapshot.prevPieceHasMoved, snapshot.prevRookHasMoved);

        currentTurn = currentTurn.opposite();
        gameOver = false;
        result = "";
    }

    public boolean isCheckmate() {
        if (board.isInCheck(currentTurn)) {
            return board.getLegalMoves(currentTurn).isEmpty();
        }
        return false;
    }

    public boolean isStalemate() {
        if (!board.isInCheck(currentTurn)) {
            return board.getLegalMoves(currentTurn).isEmpty();
        }
        return false;
    }

    public Board getBoard() { return board; }
    public PieceColor getCurrentTurn() { return currentTurn; }
    public List<Move> getMoveHistory() { return moveHistory; }
    public boolean isGameOver() { return gameOver; }
    public String getResult() { return result; }
}
