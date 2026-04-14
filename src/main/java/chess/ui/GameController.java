package chess.ui;

import chess.ai.ChessAI;
import chess.model.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.ArrayList;
import java.util.List;

public class GameController {
    private GameState gameState;
    private ChessAI ai;
    private boolean vsAI;
    private int[] selectedSquare;
    private List<Move> validMovesForSelected;
    private BoardView boardView;
    private Label statusLabel;
    private ListView<String> moveHistoryList;

    private int[] lastMoveFrom;
    private int[] lastMoveTo;
    private boolean aiThinking;

    public GameController(BoardView boardView, Label statusLabel, ListView<String> moveHistoryList) {
        this.boardView = boardView;
        this.statusLabel = statusLabel;
        this.moveHistoryList = moveHistoryList;
        this.vsAI = true;
        newGame();

        boardView.setOnSquareClick(this::onSquareClick);
    }

    public void onSquareClick(int row, int col) {
        if (gameState.isGameOver() || aiThinking) return;
        if (vsAI && gameState.getCurrentTurn() == PieceColor.BLACK) return;

        Board board = gameState.getBoard();
        Piece clickedPiece = board.getPiece(row, col);

        if (selectedSquare == null) {
            // Select a piece belonging to current player
            if (clickedPiece != null && clickedPiece.getColor() == gameState.getCurrentTurn()) {
                selectedSquare = new int[]{row, col};
                validMovesForSelected = board.getLegalMovesForPiece(row, col);
            }
        } else {
            // Try to make the selected move
            Move moveToMake = findMove(row, col);
            if (moveToMake != null) {
                executeMove(moveToMake);
            } else if (clickedPiece != null && clickedPiece.getColor() == gameState.getCurrentTurn()) {
                // Select different piece
                selectedSquare = new int[]{row, col};
                validMovesForSelected = board.getLegalMovesForPiece(row, col);
            } else {
                // Deselect
                selectedSquare = null;
                validMovesForSelected = null;
            }
        }

        updateUI();
    }

    private Move findMove(int toRow, int toCol) {
        if (validMovesForSelected == null) return null;
        for (Move m : validMovesForSelected) {
            if (m.toRow == toRow && m.toCol == toCol) {
                return m;
            }
        }
        return null;
    }

    private void executeMove(Move move) {
        lastMoveFrom = new int[]{move.fromRow, move.fromCol};
        lastMoveTo = new int[]{move.toRow, move.toCol};

        gameState.makeMove(move);
        selectedSquare = null;
        validMovesForSelected = null;

        String notation = moveToAlgebraicNotation(move);
        moveHistoryList.getItems().add(gameState.getMoveHistory().size() + ". " + notation);
        moveHistoryList.scrollTo(moveHistoryList.getItems().size() - 1);

        if (vsAI && !gameState.isGameOver() && gameState.getCurrentTurn() == PieceColor.BLACK) {
            triggerAIMove();
        }
    }

    private void triggerAIMove() {
        aiThinking = true;
        statusLabel.setText("AI is thinking...");

        Board boardCopy = gameState.getBoard().copy();
        Task<Move> aiTask = new Task<>() {
            @Override
            protected Move call() {
                return ai.getBestMove(boardCopy);
            }
        };

        aiTask.setOnSucceeded(e -> {
            Move aiMove = aiTask.getValue();
            aiThinking = false;
            if (aiMove != null && !gameState.isGameOver()) {
                // Find the matching move on the actual board (not the copy)
                Move actualMove = findActualMove(aiMove);
                if (actualMove != null) {
                    lastMoveFrom = new int[]{actualMove.fromRow, actualMove.fromCol};
                    lastMoveTo = new int[]{actualMove.toRow, actualMove.toCol};

                    gameState.makeMove(actualMove);
                    String notation = moveToAlgebraicNotation(actualMove);
                    moveHistoryList.getItems().add(gameState.getMoveHistory().size() + ". " + notation);
                    moveHistoryList.scrollTo(moveHistoryList.getItems().size() - 1);
                }
            }
            updateUI();
        });

        aiTask.setOnFailed(e -> {
            aiThinking = false;
            updateUI();
        });

        Thread thread = new Thread(aiTask);
        thread.setDaemon(true);
        thread.start();
    }

    private Move findActualMove(Move aiMove) {
        List<Move> legalMoves = gameState.getBoard().getLegalMoves(PieceColor.BLACK);
        for (Move m : legalMoves) {
            if (m.fromRow == aiMove.fromRow && m.fromCol == aiMove.fromCol
                    && m.toRow == aiMove.toRow && m.toCol == aiMove.toCol
                    && m.type == aiMove.type) {
                return m;
            }
        }
        // Fallback: match by positions only
        if (!legalMoves.isEmpty()) {
            for (Move m : legalMoves) {
                if (m.fromRow == aiMove.fromRow && m.fromCol == aiMove.fromCol
                        && m.toRow == aiMove.toRow && m.toCol == aiMove.toCol) {
                    return m;
                }
            }
        }
        return null;
    }

    public void newGame() {
        gameState = new GameState();
        ai = new ChessAI(PieceColor.BLACK);
        selectedSquare = null;
        validMovesForSelected = null;
        lastMoveFrom = null;
        lastMoveTo = null;
        aiThinking = false;
        if (moveHistoryList != null) moveHistoryList.getItems().clear();
        updateUI();
    }

    public void undoMove() {
        if (aiThinking) return;
        if (gameState.canUndo()) {
            gameState.undoLastMove();
            // In AI mode, also undo the player's move so it's back to WHITE's turn
            if (vsAI && gameState.canUndo() && gameState.getCurrentTurn() != PieceColor.WHITE) {
                gameState.undoLastMove();
            }

            selectedSquare = null;
            validMovesForSelected = null;

            List<Move> history = gameState.getMoveHistory();
            if (!history.isEmpty()) {
                Move last = history.get(history.size() - 1);
                lastMoveFrom = new int[]{last.fromRow, last.fromCol};
                lastMoveTo = new int[]{last.toRow, last.toCol};
            } else {
                lastMoveFrom = null;
                lastMoveTo = null;
            }

            // Sync move history list
            moveHistoryList.getItems().clear();
            for (int i = 0; i < history.size(); i++) {
                moveHistoryList.getItems().add((i + 1) + ". " + moveToAlgebraicNotation(history.get(i)));
            }

            updateUI();
        }
    }

    public void toggleAI() {
        vsAI = !vsAI;
        updateUI();
        // If it's now AI's turn, trigger AI move
        if (vsAI && !gameState.isGameOver() && gameState.getCurrentTurn() == PieceColor.BLACK) {
            triggerAIMove();
        }
    }

    public boolean isVsAI() {
        return vsAI;
    }

    private void updateUI() {
        boardView.refresh(gameState, selectedSquare, validMovesForSelected, lastMoveFrom, lastMoveTo);

        if (gameState.isGameOver()) {
            statusLabel.setText(gameState.getResult());
        } else if (aiThinking) {
            statusLabel.setText("AI is thinking...");
        } else {
            PieceColor turn = gameState.getCurrentTurn();
            boolean inCheck = gameState.getBoard().isInCheck(turn);
            String turnStr = (turn == PieceColor.WHITE ? "White" : "Black") + "'s turn";
            if (inCheck) turnStr += " (Check!)";
            if (vsAI) {
                turnStr += turn == PieceColor.WHITE ? " [You]" : " [AI]";
            }
            statusLabel.setText(turnStr);
        }
    }

    private String moveToAlgebraicNotation(Move move) {
        if (move.type == MoveType.CASTLING_KINGSIDE) return "O-O";
        if (move.type == MoveType.CASTLING_QUEENSIDE) return "O-O-O";

        String from = colToFile(move.fromCol) + (8 - move.fromRow);
        String to = colToFile(move.toCol) + (8 - move.toRow);
        String pieceSymbol = "";
        if (move.piece.getType() != PieceType.PAWN) {
            pieceSymbol = move.piece.getSymbol();
        }

        String capture = move.captured != null ? "x" : "-";
        String result = pieceSymbol + from + capture + to;

        if (move.type == MoveType.PROMOTION) {
            result += "=Q";
        }
        if (move.type == MoveType.EN_PASSANT) {
            result += " e.p.";
        }
        return result;
    }

    private String colToFile(int col) {
        return String.valueOf((char) ('a' + col));
    }
}
