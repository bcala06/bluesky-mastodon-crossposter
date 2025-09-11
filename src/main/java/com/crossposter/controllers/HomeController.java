package com.crossposter.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;

public class HomeController {

    @FXML
    public void openHome(MouseEvent event) {
        System.out.println("Navigating to Home...");
        SceneManager.switchScene("/fxml/home.fxml", "Home");
    }

    @FXML
    public void openCreatePost(MouseEvent event) {
        System.out.println("[Home] Create Post clicked");
        SceneManager.switchScene("/fxml/create_post.fxml", "Create Post");
    }

    @FXML
    public void openBluesky(ActionEvent event) {
        System.out.println("[Home] BlueSky Connect clicked");
        SceneManager.switchScene("/fxml/bluesky.fxml", "BlueSky Connect");
    }

    @FXML
    public void openMastodon(ActionEvent event) {
        System.out.println("[Home] Mastodon Connect clicked");
        SceneManager.switchScene("/fxml/mastodon.fxml", "Mastodon Connect");
    }

    @FXML
    public void openSettings(MouseEvent event) {
        System.out.println("[Home] Settings clicked");
        SceneManager.switchScene("/fxml/settings.fxml", "Settings");
    }
}
