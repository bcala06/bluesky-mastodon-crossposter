package com.crossposter.controllers;

import com.crossposter.services.ServiceRegistry;
import com.crossposter.services.BlueskyClient;
import com.crossposter.services.AuthSession;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.PrintWriter;
import java.io.StringWriter;

public class BlueskyController {

    private final BlueskyClient blueskyClient = ServiceRegistry.getBlueskyClient();

    @FXML
    private void openDashboard(MouseEvent event) {
        System.out.println("[Bluesky] Navigating to Dashboard...");
        SceneManager.switchScene("/fxml/dashboard.fxml", "Dashboard");
    }

    @FXML
    private void openCreatePost(MouseEvent event) {
        System.out.println("[Bluesky] Navigating to Create Post...");
        SceneManager.switchScene("/fxml/create_post.fxml", "Create Post");
    }

    @FXML
    private void openSettings(MouseEvent event) {
        System.out.println("[Bluesky] Navigating to Settings...");
        SceneManager.switchScene("/fxml/settings.fxml", "Settings");
    }

    @FXML
    private void handleStartOAuth(ActionEvent event) {
        System.out.println("[Bluesky] Authenticate clicked");

        try {
            String pdsOrigin = "https://bsky.social";
            AuthSession session = blueskyClient.startAuth(pdsOrigin);

            ServiceRegistry.setBlueskySession(session);
            ServiceRegistry.setBlueskyPdsOrigin(session.pdsEndpoint);

            showAlert(Alert.AlertType.INFORMATION, "Authentication successful!", "Logged in as DID: " + session.did());

            SceneManager.switchScene("/fxml/dashboard.fxml", "Dashboard");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Authentication failed!", "Error: " + e.getMessage());
        }
    }

    // error handling update
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);

        if (message == null) {
            message = "An unexpected error occurred.";
        }

        if (message.length() < 100) {
            alert.setContentText(message);
        } else {
            alert.setContentText("An error occurred. See details for more information.");

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.write(message);
            String exceptionText = sw.toString();

            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);

            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(new Label("The full error message is:"), 0, 0);
            expContent.add(textArea, 0, 1);

            alert.getDialogPane().setExpandableContent(expContent);
        }

        alert.showAndWait();
    }
}