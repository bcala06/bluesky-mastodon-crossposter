package com.crossposter.controllers;

import com.crossposter.services.ServiceRegistry;
import com.crossposter.services.BlueskyClient;
import com.crossposter.services.AuthSession;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseEvent;

public class BlueskyController {

    private final BlueskyClient blueskyClient = ServiceRegistry.getBlueskyClient();

    // Navigation
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

    // Authenticate action
    @FXML
    private void handleStartOAuth(ActionEvent event) {
        System.out.println("[Bluesky] Authenticate clicked");

        try {
            String pdsOrigin = "https://bsky.social";
            AuthSession session = blueskyClient.startAuth(pdsOrigin);

            ServiceRegistry.setBlueskySession(session);
            ServiceRegistry.setBlueskyPdsOrigin(pdsOrigin);

            showAlert(Alert.AlertType.INFORMATION, "Authentication successful!",
                    "Logged in as DID: " + session.did());

            SceneManager.switchScene("/fxml/dashboard.fxml", "Dashboard");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Authentication failed!", "Error: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
