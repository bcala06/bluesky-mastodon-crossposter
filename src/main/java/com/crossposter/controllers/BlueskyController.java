package com.crossposter.controllers;

import javafx.event.ActionEvent;
import javafx.scene.input.MouseEvent;

public class BlueskyController {

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
        // TODO: Implement authentication logic
    }
}
