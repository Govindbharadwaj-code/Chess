package chess.ui;

import chess.model.*;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.function.BiConsumer;

public class BoardView extends GridPane {
    private static final int CELL_SIZE = 72;
    private static final String LIGHT_COLOR = "#f0d9b5";
    private static final String DARK_COLOR = "#b58863";

    private StackPane[][] cells;
    private BiConsumer<Integer, Integer> onSquareClick;

    public BoardView() {
        cells = new StackPane[8][8];
        setAlignment(Pos.CENTER);
        buildBoard();
    }

    private void buildBoard() {
        // Add file labels (a-h) at the top and bottom
        for (int col = 0; col < 8; col++) {
            String fileLetter = String.valueOf((char) ('a' + col));

            Label topLabel = new Label(fileLetter);
            topLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
            topLabel.setAlignment(Pos.CENTER);
            topLabel.setMinSize(CELL_SIZE, 18);
            add(topLabel, col + 1, 0);

            Label bottomLabel = new Label(fileLetter);
            bottomLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
            bottomLabel.setAlignment(Pos.CENTER);
            bottomLabel.setMinSize(CELL_SIZE, 18);
            add(bottomLabel, col + 1, 9);
        }

        for (int row = 0; row < 8; row++) {
            String rankNumber = String.valueOf(8 - row);

            Label leftLabel = new Label(rankNumber);
            leftLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
            leftLabel.setAlignment(Pos.CENTER);
            leftLabel.setMinSize(18, CELL_SIZE);
            add(leftLabel, 0, row + 1);

            Label rightLabel = new Label(rankNumber);
            rightLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
            rightLabel.setAlignment(Pos.CENTER);
            rightLabel.setMinSize(18, CELL_SIZE);
            add(rightLabel, 9, row + 1);

            for (int col = 0; col < 8; col++) {
                StackPane cell = new StackPane();
                cell.setMinSize(CELL_SIZE, CELL_SIZE);
                cell.setMaxSize(CELL_SIZE, CELL_SIZE);

                boolean isLight = (row + col) % 2 == 0;
                cell.setStyle("-fx-background-color: " + (isLight ? LIGHT_COLOR : DARK_COLOR) + ";");

                cells[row][col] = cell;
                final int r = row, c = col;
                cell.setOnMouseClicked(e -> {
                    if (onSquareClick != null) onSquareClick.accept(r, c);
                });

                add(cell, col + 1, row + 1);
            }
        }
    }

    public void refresh(GameState state, int[] selectedSquare, List<Move> validMoves,
                        int[] lastMoveFrom, int[] lastMoveTo) {
        Board board = state.getBoard();
        PieceColor currentTurn = state.getCurrentTurn();
        int[] kingPos = board.findKing(currentTurn);
        boolean inCheck = board.isInCheck(currentTurn);

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                StackPane cell = cells[row][col];
                cell.getChildren().clear();

                // Determine background color
                String bgColor = getCellColor(row, col, selectedSquare, validMoves,
                        lastMoveFrom, lastMoveTo, kingPos, inCheck);
                cell.setStyle("-fx-background-color: " + bgColor + ";");

                // Add piece
                Piece piece = board.getPiece(row, col);
                if (piece != null) {
                    Label pieceLabel = new Label(piece.getSymbol());
                    pieceLabel.getStyleClass().add("piece-label");
                    if (piece.getColor() == PieceColor.WHITE) {
                        pieceLabel.getStyleClass().add("piece-white");
                    } else {
                        pieceLabel.getStyleClass().add("piece-black");
                    }
                    pieceLabel.setMouseTransparent(true);
                    cell.getChildren().add(pieceLabel);
                } else if (isValidMoveTarget(row, col, validMoves)) {
                    // Show a dot for valid empty square moves
                    Circle dot = new Circle(10, Color.web("#00c800", 0.5));
                    dot.setMouseTransparent(true);
                    cell.getChildren().add(dot);
                }
            }
        }
    }

    private String getCellColor(int row, int col, int[] selectedSquare, List<Move> validMoves,
                                 int[] lastMoveFrom, int[] lastMoveTo, int[] kingPos, boolean inCheck) {
        boolean isLight = (row + col) % 2 == 0;
        String baseColor = isLight ? LIGHT_COLOR : DARK_COLOR;

        // King in check highlight
        if (inCheck && kingPos != null && kingPos[0] == row && kingPos[1] == col) {
            return "#e74c3ccc";
        }

        // Selected square highlight
        if (selectedSquare != null && selectedSquare[0] == row && selectedSquare[1] == col) {
            return "#f6f669cc";
        }

        // Valid move destination highlight (for captures - square with piece)
        if (isValidMoveTarget(row, col, validMoves)) {
            // Check if there's a capture move here
            if (isCaptureTarget(row, col, validMoves)) {
                return isLight ? "#f0d9b590" : "#b5886390";
            }
            return baseColor; // will have dot overlay
        }

        // Last move highlight
        if (lastMoveFrom != null && lastMoveFrom[0] == row && lastMoveFrom[1] == col) {
            return "#cdd52680";
        }
        if (lastMoveTo != null && lastMoveTo[0] == row && lastMoveTo[1] == col) {
            return "#cdd52680";
        }

        return baseColor;
    }

    private boolean isValidMoveTarget(int row, int col, List<Move> validMoves) {
        if (validMoves == null) return false;
        for (Move m : validMoves) {
            if (m.toRow == row && m.toCol == col) return true;
        }
        return false;
    }

    private boolean isCaptureTarget(int row, int col, List<Move> validMoves) {
        if (validMoves == null) return false;
        for (Move m : validMoves) {
            if (m.toRow == row && m.toCol == col && m.captured != null) return true;
        }
        return false;
    }

    public void setOnSquareClick(BiConsumer<Integer, Integer> handler) {
        this.onSquareClick = handler;
    }
}
