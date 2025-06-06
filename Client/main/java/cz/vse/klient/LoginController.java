package cz.vse.klient;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoginController {
    private static final Logger logger = LogManager.getLogger(LoginController.class);

    @FXML
    private TextField usernameField;

    @FXML
    private void handleStartGame() {
        String username = usernameField.getText();

        if (username != null && !username.isEmpty()) {
            NetworkHandler.sendUsername(username);
            logger.info("Sent username: {}", username);
            startGame(username);
        } else {
            logger.warn("Username cannot be empty!");
        }
    }

    private void startGame(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/cz/vse/klient/game-view.fxml"));
            Scene scene = new Scene(loader.load());
            GameController gameController = loader.getController();
            gameController.setUsername(username);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Battleships - Game");
            stage.show();
            stage.setOnCloseRequest(event -> handleExitGame());
            logger.debug("Game started successfully.");
        } catch (Exception e) {
            logger.error("Error while starting the game: ", e);
        }
    }

    @FXML
    public void handleExitGame() {
        AppUtils.handleExitGame();
    }
}