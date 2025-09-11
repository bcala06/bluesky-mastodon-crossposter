package com.crossposter.controllers;

import javafx.event.ActionEvent;
import javafx.scene.input.MouseEvent;

public class MastodonController {

    // Header navigation
    public void openHome(MouseEvent event) {
        System.out.println("[Mastodon] Navigating to Home...");
        SceneManager.switchScene("/fxml/home.fxml", "Home");
    }

    public void openCreatePost(MouseEvent event) {
        System.out.println("[Mastodon] Navigating to Create Post...");
        SceneManager.switchScene("/fxml/create_post.fxml", "Create Post");
    }

    public void openSettings(MouseEvent event) {
        System.out.println("[Mastodon] Navigating to Settings...");
        SceneManager.switchScene("/fxml/settings.fxml", "Settings");
    }

    // Main content action
    public void handleAuthentication(ActionEvent event) {
        System.out.println("[Mastodon] Authenticate clicked");
        // TODO: Implement authentication logic
    }
}
