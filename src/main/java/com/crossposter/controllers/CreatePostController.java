package com.crossposter.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;

public class CreatePostController {

    @FXML
    private TextArea postContent;

    @FXML
    private CheckBox blueskyCheck;

    @FXML
    private CheckBox mastodonCheck;

    @FXML
    public void handlePost() {
        String content = postContent.getText();
        boolean postToBluesky = blueskyCheck.isSelected();
        boolean postToMastodon = mastodonCheck.isSelected();

        System.out.println("Submitting post:");
        System.out.println("Content: " + content);
        System.out.println("Bluesky: " + postToBluesky);
        System.out.println("Mastodon: " + postToMastodon);
    }

    // Shared header
    @FXML
    public void openHome(MouseEvent event) {
        System.out.println("Navigating to Home...");
        SceneManager.switchScene("/fxml/home.fxml", "Home");
    }

    @FXML
    public void openCreatePost(MouseEvent event) {
        System.out.println("Already on Create Post page.");
    }

    @FXML
    public void openSettings(MouseEvent event) {
        System.out.println("Navigating to Settings...");
        SceneManager.switchScene("/fxml/settings.fxml", "Settings");
    }
}
