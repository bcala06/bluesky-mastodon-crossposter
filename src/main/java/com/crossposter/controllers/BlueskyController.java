package com.crossposter.controllers;
import com.crossposter.services.BlueskySessionClient;
import com.crossposter.services.ServiceRegistry;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

public class BlueskyController {
    private final BlueskySessionClient bluesky = ServiceRegistry.getBlueskyClient();

    @FXML
    private TextField handleField;

    @FXML
    private PasswordField passwordField;

    // Header navigation
    public void openHome(MouseEvent event) {
        System.out.println("[Bluesky] Navigating to Home...");
        SceneManager.switchScene("/fxml/home.fxml", "Home");
    }

    public void openCreatePost(MouseEvent event) {
        System.out.println("[Bluesky] Navigating to Create Post...");
        SceneManager.switchScene("/fxml/create_post.fxml", "Create Post");
    }

    public void openSettings(MouseEvent event) {
        System.out.println("[Bluesky] Navigating to Settings...");
        SceneManager.switchScene("/fxml/settings.fxml", "Settings");
    }

    // Main content action
    public void handleAuthentication(ActionEvent event) {
        System.out.println("[Bluesky] Authenticate clicked");

        String identifier = handleField.getText();
        String appPassword = passwordField.getText();

        if (identifier == null || identifier.isBlank() ||
            appPassword == null || appPassword.isBlank()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText("Missing Information");
            alert.setContentText("Please enter both handle and app password.");
            alert.showAndWait();
            return;
        }

        boolean success = bluesky.login(identifier, appPassword);

        Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        if (success) {
            alert.setHeaderText("Authentication successful!");
            alert.setContentText("Logged in as: " + bluesky.getDid());
            SceneManager.switchScene("/fxml/create_post.fxml", "Create Post");
        } else {
            alert.setHeaderText("Authentication failed!");
            alert.setContentText("Check your handle and app password.");
        }
        alert.showAndWait();
    }
}
