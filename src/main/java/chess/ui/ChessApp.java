package chess.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class ChessApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // Board in the center
        BoardView boardView = new BoardView();
        boardView.setPadding(new Insets(10));
        root.setCenter(boardView);

        // Side panel on the right
        VBox sidePanel = new VBox(12);
        sidePanel.getStyleClass().add("side-panel");
        sidePanel.setAlignment(Pos.TOP_CENTER);
        sidePanel.setPrefWidth(200);

        Label titleLabel = new Label("♔ Chess");
        titleLabel.getStyleClass().add("title-label");

        Label statusLabel = new Label("White's turn");
        statusLabel.getStyleClass().add("status-label");
        statusLabel.setWrapText(true);
        statusLabel.setAlignment(Pos.CENTER);

        Label historyLabel = new Label("Move History");
        historyLabel.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 12px; -fx-font-weight: bold;");

        ListView<String> moveHistoryList = new ListView<>();
        moveHistoryList.getStyleClass().add("move-history");
        moveHistoryList.setPrefHeight(300);

        GameController controller = new GameController(boardView, statusLabel, moveHistoryList);

        Button newGameBtn = new Button("New Game");
        newGameBtn.getStyleClass().add("game-button");
        newGameBtn.setMaxWidth(Double.MAX_VALUE);
        newGameBtn.setOnAction(e -> controller.newGame());

        Button undoBtn = new Button("Undo Move");
        undoBtn.getStyleClass().add("game-button");
        undoBtn.setMaxWidth(Double.MAX_VALUE);
        undoBtn.setOnAction(e -> controller.undoMove());

        Button aiToggleBtn = new Button("Mode: vs AI");
        aiToggleBtn.getStyleClass().add("game-button");
        aiToggleBtn.setMaxWidth(Double.MAX_VALUE);
        aiToggleBtn.setOnAction(e -> {
            controller.toggleAI();
            aiToggleBtn.setText(controller.isVsAI() ? "Mode: vs AI" : "Mode: 2 Players");
        });

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #4a4a6a;");

        sidePanel.getChildren().addAll(
                titleLabel,
                statusLabel,
                sep,
                newGameBtn,
                undoBtn,
                aiToggleBtn,
                historyLabel,
                moveHistoryList
        );

        root.setRight(sidePanel);

        Scene scene = new Scene(root, 800, 600);
        try {
            String css = getClass().getResource("/chess.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Could not load CSS: " + e.getMessage());
        }

        primaryStage.setTitle("Chess");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}
