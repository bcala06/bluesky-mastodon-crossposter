package com.crossposter.controllers;

import com.crossposter.services.MastodonClient;
import com.crossposter.services.ServiceRegistry;
import com.crossposter.services.AuthSession;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

public class MastodonController {

    private final MastodonClient mastodonClient = ServiceRegistry.getMastodonClient();

    @FXML
    private TextField mastodonHandleField;

    // Navigation
    @FXML
    private void openDashboard(MouseEvent event) {
        System.out.println("[Mastodon] Navigating to Dashboard...");
        SceneManager.switchScene("/fxml/dashboard.fxml", "Dashboard");
    }

    @FXML
    private void openCreatePost(MouseEvent event) {
        System.out.println("[Mastodon] Navigating to Create Post...");
        SceneManager.switchScene("/fxml/create_post.fxml", "Create Post");
    }

    @FXML
    private void openSettings(MouseEvent event) {
        System.out.println("[Mastodon] Navigating to Settings...");
        SceneManager.switchScene("/fxml/settings.fxml", "Settings");
    }

    // Authenticate action
    @FXML
    private void handleAuthentication(ActionEvent event) {
        System.out.println("[Mastodon] Authenticate clicked");

        String userInput = mastodonHandleField.getText();
        if (userInput == null || userInput.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Required", "Please enter your Mastodon handle or instance.");
            return;
        }

        try {
            AuthSession session = mastodonClient.startAuth(userInput);
            ServiceRegistry.setMastodonSession(session);

            showAlert(Alert.AlertType.INFORMATION, "Authentication successful!",
                    "Logged in to instance: " + session.instanceUrl);

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
