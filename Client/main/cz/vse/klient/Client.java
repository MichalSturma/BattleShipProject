package cz.vse.klient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



public class Client extends Application {
    private static final Logger logger = LogManager.getLogger(Client.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Connect to the server when the application starts
        NetworkHandler.connectToServer();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/cz/vse/klient/login-view.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Battleships - Login");
        primaryStage.show();
        LoginController loginController = loader.getController();
        primaryStage.setOnCloseRequest(event -> loginController.handleExitGame());
    }

    public static void main(String[] args) {
        launch(args);
    }
}