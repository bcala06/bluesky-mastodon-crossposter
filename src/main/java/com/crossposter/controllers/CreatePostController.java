package com.crossposter.controllers;
import com.crossposter.services.BlueskySessionClient;
import com.crossposter.services.ServiceRegistry;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;

public class CreatePostController {
    private final BlueskySessionClient bluesky = ServiceRegistry.getBlueskyClient();

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

        if (postToBluesky) {
            boolean success = bluesky.createPost(content);
            Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
            if (success) {
                alert.setContentText("Posted to Bluesky successfully!");
            } else {
                alert.setContentText("Post to Bluesky failed.");
            }
            alert.showAndWait();
        }
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
