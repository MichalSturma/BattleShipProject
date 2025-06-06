package cz.vse.klient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;

public class NetworkHandler {
    private static final Logger logger = LogManager.getLogger(NetworkHandler.class);
    private static PrintWriter out;
    private static BufferedReader in;
    private static Socket socket;
    private static GameController gameController;
    private static boolean gameStarted = false;
    private static boolean listening = false;

    public static void connectToServer() {
        try {
            socket = new Socket("localhost", 12345);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            listenForServerResponses();
            logger.info("Connected to server");

        } catch (IOException e) {
            logger.error("Failed to connect to server", e);
        }
    }

    public static void sendUsername(String username) {
        if (out != null) {
            out.println("LOGIN: " + username);
            logger.info("Sent LOGIN: {}", username);
        } else {
            logger.warn("Output stream is null, cannot send username");
        }
    }

    public static void sendMessage(String message) {
        if (out != null) {
            out.println(message);
            out.flush();
        } else {
            logger.warn("Output stream is null, cannot send message");
        }
    }

    public static void setGameController(GameController controller) {
        gameController = controller;
        logger.info("GameController initialized!");

        if (gameStarted) {
            logger.info("Game already started! Enabling ship placement now.");
            gameController.enableShipPlacement();
        }
    }

    public static void listenForServerResponses() {
        if (listening) {
            logger.warn("Already listening for server responses!");
            return;
        }
        listening = true;

        new Thread(() -> {
            try {
                String response;
                while ((response = in.readLine()) != null) {
                    if (gameController != null) {
                        gameController.processServerResponse(response);
                    } else {
                        logger.warn("Warning: gameController is NULL!");
                    }
                }
            } catch (IOException e) {
                logger.error("Error in server response listener", e);
            }
        }).start();
    }
    public static void flush() {
        if (out != null) {
            out.flush();
        } else {
            logger.warn("Output stream is null, cannot flush");
        }
    }
    public static void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void reinitialize() {
        try {
            // Close existing connections
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }

            // Reset flags
            gameStarted = false;
            listening = false;

            // Reconnect to the server
            connectToServer();
            logger.info("Reinitialized connection to server");

        } catch (IOException e) {
            logger.error("Failed to reinitialize connection", e);
        }
    }
}
