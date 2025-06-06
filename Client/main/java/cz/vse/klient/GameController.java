package cz.vse.klient;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

public class GameController {
    private static final Logger logger = LogManager.getLogger(GameController.class);

    @FXML
    private GridPane playerGrid;
    @FXML
    private GridPane enemyGrid;

    private Set<String> placedShips = new HashSet<>();
    private boolean placementAllowed = false;

    @FXML
    private TextArea chatArea;

    @FXML
    private ComboBox<String> shipSelector;

    private final Map<String, List<int[]>> shipShapes = new HashMap<>();
    private int placedShipCount = 0;
    private boolean isMyTurn = false;

    @FXML
    private Button exitButton;

    private int rotation = 0;

    private void showMessage(String message) {
        Platform.runLater(() -> {
            chatArea.appendText(message + "\n");
        });
    }

    @FXML
    public void initialize() {
        createGrid(playerGrid, true);
        createGrid(enemyGrid, false);
        NetworkHandler.setGameController(this);

        shipShapes.put("Battleship (4)", List.of(new int[]{0, 0}, new int[]{1, 0}, new int[]{2, 0}, new int[]{3, 0}));
        shipShapes.put("Destroyer (3)", List.of(new int[]{0, 0}, new int[]{1, 0}, new int[]{2, 0}));
        shipShapes.put("L-Ship (3)", List.of(new int[]{0, 0}, new int[]{1, 0}, new int[]{1, 1}));
        shipShapes.put("T-Ship (3)", List.of(new int[]{0, -1}, new int[]{0, 0}, new int[]{0, 1}, new int[]{1, 0}));
        shipShapes.put("PatrolBoat (2)", List.of(new int[]{0, 0}, new int[]{1, 0}));

        shipSelector.getItems().addAll(shipShapes.keySet());
        shipSelector.setValue("Battleship (4)");

        // Přidání obsluhy kolečka myši pro otáčení lodí
        playerGrid.setOnScroll(event -> {
            if (event.getDeltaY() > 0) {
                rotation = (rotation + 1) % 4; // Otočení doprava
            } else {
                rotation = (rotation + 3) % 4; // Otočení doleva
            }
            showMessage("Rotace: " + (rotation * 90) + "°");
        });

        showMessage("Game initialized, ships set up.");
        NetworkHandler.sendMessage("READY");
    }

    private void createGrid(GridPane grid, boolean isPlayerGrid) {
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                Button cell = new Button();
                cell.setPrefSize(30, 30);
                cell.getProperties().put("occupied", false); // Add custom property
                int finalRow = row, finalCol = col;

                if (isPlayerGrid) {
                    cell.setOnAction(e -> handleCellClick(finalRow, finalCol, cell));
                    cell.setOnMouseEntered(e -> showPreview(finalRow, finalCol));
                    cell.setOnMouseExited(e -> clearPreview());
                } else {
                    cell.setOnAction(e -> {
                        logger.info("Clicked on enemy grid at: {},{}", finalRow, finalCol);
                        fireAtOpponent(finalRow, finalCol, cell);
                    });
                }

                GridPane.setRowIndex(cell, row);
                GridPane.setColumnIndex(cell, col);
                grid.getChildren().add(cell);
            }
        }
    }

    private void showPreview(int row, int col) {
        if (placedShipCount >= 5) {
            return; // Disable preview if all ships are placed
        }

        String selectedShip = shipSelector.getValue();
        List<int[]> shape = shipShapes.get(selectedShip);

        for (int[] offset : shape) {
            int newRow = row, newCol = col;

            // Aplikace rotace
            switch (rotation) {
                case 1: // 90°
                    newRow = row - offset[1];
                    newCol = col + offset[0];
                    break;
                case 2: // 180°
                    newRow = row - offset[0];
                    newCol = col - offset[1];
                    break;
                case 3: // 270°
                    newRow = row + offset[1];
                    newCol = col - offset[0];
                    break;
                default: // 0°
                    newRow = row + offset[0];
                    newCol = col + offset[1];
            }

            if (newRow >= 0 && newRow < 10 && newCol >= 0 && newCol < 10) {
                for (javafx.scene.Node node : playerGrid.getChildren()) {
                    if (GridPane.getRowIndex(node) == newRow && GridPane.getColumnIndex(node) == newCol) {
                        Button cell = (Button) node;
                        if (!(boolean) cell.getProperties().get("occupied")) { // Skip occupied cells
                            cell.setStyle("-fx-background-color: lightblue;");
                        }
                    }
                }
            }
        }
    }

    private void clearPreview() {
        if (placedShipCount >= 5) {
            return; // Disable preview if all ships are placed
        }

        for (javafx.scene.Node node : playerGrid.getChildren()) {
            Button cell = (Button) node;
            if (!(boolean) cell.getProperties().get("occupied")) { // Check if cell is occupied
                cell.setStyle(null);
            }
        }
    }

    private void handleCellClick(int row, int col, Button cell) {
        if (placedShipCount >= 5) {
            logger.warn("All 5 ships already placed!");
            showMessage("All 5 ships already placed!");
            return;
        }

        String selectedShip = shipSelector.getValue();
        if (placedShips.contains(selectedShip)) {
            logger.warn("Ship {} already placed!", selectedShip);
            showMessage("Ship " + selectedShip + " already placed!");
            return;
        }

        List<int[]> shape = shipShapes.get(selectedShip);
        List<String> shipCoords = new ArrayList<>();

        logger.info("Placing {} at base position: {},{} with rotation: {}", selectedShip, row, col, rotation);

        for (int[] offset : shape) {
            int newRow = row, newCol = col;

            // Aplikace rotace
            switch (rotation) {
                case 1: // 90°
                    newRow = row - offset[1];
                    newCol = col + offset[0];
                    break;
                case 2: // 180°
                    newRow = row - offset[0];
                    newCol = col - offset[1];
                    break;
                case 3: // 270°
                    newRow = row + offset[1];
                    newCol = col - offset[0];
                    break;
                default: // 0°
                    newRow = row + offset[0];
                    newCol = col + offset[1];
            }

            if (newRow < 0 || newRow >= 10 || newCol < 0 || newCol >= 10) {
                logger.warn("Ship {} goes out of bounds!", selectedShip);
                showMessage("Ship " + selectedShip + " goes out of bounds!");
                return;
            }

            shipCoords.add(newRow + "," + newCol);
        }
        String shipPosition = String.join(" ", shipCoords);
        logger.info("Sending ship placement to server: PLACE {} {}", selectedShip, shipPosition);
        NetworkHandler.sendMessage("PLACE " + selectedShip + " " + shipPosition);
    }

    public void enableShipPlacement() {
        Platform.runLater(() -> {
            logger.info("Game started! Ship placement enabled.");
            showMessage("Game started! Ship placement enabled.");
            placementAllowed = true;
        });
    }

    private void fireAtOpponent(int row, int col, Button cell) {
        String move = row + "," + col;
        logger.info("Firing at opponent: {}", move);

        NetworkHandler.sendMessage("FIRE " + move);
        isMyTurn = false;
    }

    public void updateShotResult(String move, boolean hit) {
        Platform.runLater(() -> {
            logger.info("Shot at {} resulted in: {}", move, hit ? "HIT" : "MISS");
            String[] parts = move.split(",");
            int row = Integer.parseInt(parts[0]);
            int col = Integer.parseInt(parts[1]);

            for (javafx.scene.Node node : enemyGrid.getChildren()) {
                if (Objects.equals(GridPane.getRowIndex(node), row) &&
                        Objects.equals(GridPane.getColumnIndex(node), col)) {

                    Button cell = (Button) node;
                    cell.setStyle(hit ? "-fx-background-color: green;" : "-fx-background-color: red;");
                    break;
                }
            }
        });
    }

    public void setTurn(boolean myTurn) {
        isMyTurn = myTurn;
    }

    public void processServerResponse(String response) {
        Platform.runLater(() -> {
            logger.info("Server response: {}", response);

            if (response.startsWith("SUCCESS")) {
                String positions = response.substring(8).trim();

                if (positions.startsWith("PLACE:")) {
                    String[] parts = positions.split(" ", 3);
                    if (parts.length > 1) {
                        positions = parts[2].replaceAll("\\(\\d+/\\d+\\)", "").trim();
                        drawShipOnGrid(positions);
                    }
                } else if (positions.startsWith("HIT:") || positions.startsWith("MISS:")) {
                    updateShotResult(positions.substring(5).trim(), positions.startsWith("HIT:"));
                } else if (positions.startsWith("SUNK:")) {
                    recolorSunkShip(positions.substring(5).trim());
                } else if (positions.equals("Your turn")) {
                    showMessage("It's your turn!");
                    setTurn(true);
                } else if (positions.equals("Opponent's turn")) {
                    showMessage("It's your opponent's turn!");
                    setTurn(false);
                } else if (positions.startsWith("Opponent HIT:") || positions.startsWith("Opponent MISS:")) {
                    markOpponentShot(positions.substring(14).trim(), positions.startsWith("Opponent HIT:"));
                }
            } else if (response.startsWith("INFO:")) {
                String infoMessage = response.substring(5).trim();
                if (infoMessage.contains("You win") || infoMessage.contains("You lose")) {
                    disableGrids();
                    showEndGamePopup(infoMessage.contains("You win") ? "You win!" : "You lose!");
                }
                showMessage(infoMessage);
            }
        });
    }

    private void markOpponentShot(String coord, boolean hit) {
        if (coord == null || coord.isEmpty() || !coord.contains(",")) {
            logger.error("Invalid coordinate format: {}", coord);
            return;
        }

        String[] parts = coord.split(",");
        if (parts.length != 2) {
            logger.error("Invalid coordinate format: {}", coord);
            return;
        }

        try {
            int row = Integer.parseInt(parts[0].trim());
            int col = Integer.parseInt(parts[1].trim());

            for (javafx.scene.Node node : playerGrid.getChildren()) {
                if (Objects.equals(GridPane.getRowIndex(node), row) &&
                        Objects.equals(GridPane.getColumnIndex(node), col)) {

                    Button cell = (Button) node;
                    cell.setText("X");
                    if (hit) {
                        logger.info("Marking hit at {},{}", row, col);
                        cell.setStyle("-fx-background-color: red; -fx-text-fill: red;");
                    } else {
                        logger.info("Marking miss at {},{}", row, col);
                        cell.setStyle("-fx-text-fill: green;");
                    }
                    break;
                }
            }
        } catch (NumberFormatException e) {
            logger.error("Failed to parse coordinates: {}", coord, e);
        }
    }

    private void recolorSunkShip(String positions) {
        for (String pos : positions.split(" ")) {
            String[] parts = pos.split(",");
            int row = Integer.parseInt(parts[0]);
            int col = Integer.parseInt(parts[1]);

            for (javafx.scene.Node node : enemyGrid.getChildren()) {
                if (Objects.equals(GridPane.getRowIndex(node), row) &&
                        Objects.equals(GridPane.getColumnIndex(node), col)) {

                    Button cell = (Button) node;
                    cell.setStyle("-fx-background-color: black;");
                    break;
                }
            }
        }
    }

    private void disableGrids() {
        playerGrid.setDisable(true);
        enemyGrid.setDisable(true);
    }

    private void drawShipOnGrid(String positions) {
        for (String pos : positions.split(" ")) {
            String[] parts = pos.split(",");
            int row = Integer.parseInt(parts[0]);
            int col = Integer.parseInt(parts[1]);

            for (javafx.scene.Node node : playerGrid.getChildren()) {
                if (GridPane.getRowIndex(node) != null && GridPane.getColumnIndex(node) != null &&
                        GridPane.getRowIndex(node) == row && GridPane.getColumnIndex(node) == col) {

                    Button shipCell = (Button) node;
                    shipCell.setStyle("-fx-background-color: blue;");
                    shipCell.getProperties().put("occupied", true); // Mark cell as occupied
                }
            }
        }

        placedShips.add(shipSelector.getValue());
        placedShipCount++;
    }

    @FXML
    public void handleExitGame() {
        AppUtils.handleExitGame();
    }

    private void showEndGamePopup(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(null);
        alert.setContentText(message);

        ButtonType playAgainButton = new ButtonType("Play Again");
        ButtonType exitButton = new ButtonType("Exit", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(playAgainButton, exitButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == playAgainButton) {
            returnToLogin();
        } else {
            handleExitGame();
        }
    }

    private void returnToLogin() {
        try {
            // Reinitialize the client
            NetworkHandler.reinitialize();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/cz/vse/klient/login-view.fxml"));
            Stage stage = (Stage) playerGrid.getScene().getWindow();
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            stage.setTitle("Battleships - Login");
        } catch (IOException e) {
            logger.error("Error loading login screen", e);
        }
    }

    @FXML
    private Label usernameLabel;

    public void setUsername(String username) {
        usernameLabel.setText(username);
    }
}