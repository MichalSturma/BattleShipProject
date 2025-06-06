package cz.vse.klient;

import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AppUtils {
    private static final Logger logger = LogManager.getLogger(AppUtils.class);

    public static void handleExitGame() {
        Thread exitThread = new Thread(() -> {
            try {
                // Odeslání zprávy serveru
                NetworkHandler.sendMessage("EXIT");
                NetworkHandler.flush(); // Ensure the message is sent immediately

                // Zavření aplikace
                Platform.runLater(() -> {
                    Platform.exit();
                    System.exit(0);
                });
            } catch (Exception e) {
                logger.error("Error while exiting the game: ", e);
            }
        });

        exitThread.start();
        try {
            exitThread.join(); // Wait for the thread to finish
        } catch (InterruptedException e) {
            logger.error("Exit thread was interrupted: ", e);
        }
    }
}