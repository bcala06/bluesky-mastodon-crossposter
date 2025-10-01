package com.crossposter.controllers;

import com.crossposter.services.AuthSession;
import com.crossposter.services.BlueskyClient;
import com.crossposter.services.ServiceRegistry;
import com.crossposter.utils.LocalCallbackServer;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;

import java.util.Map;

public class CreatePostController {
    private final BlueskyClient blueskyClient = ServiceRegistry.getBlueskyClient();

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

        boolean allSuccess = true;

        if (postToBluesky) {
            try {
                AuthSession session = ServiceRegistry.getBlueskySession();
                if (session == null || session.accessToken() == null) {
                    showAlert(Alert.AlertType.WARNING, "Bluesky not authenticated", 
                             "Please authenticate with Bluesky first.");
                    allSuccess = false;
                } else {
                    String pdsOrigin = ServiceRegistry.getBlueskyPdsOrigin();
                    Map<String, Object> result = blueskyClient.createPost(session, pdsOrigin, content);
                    System.out.println("Bluesky post result: " + result);
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Posted to Bluesky successfully!");
                }
            } catch (Exception e) {
                System.err.println("Error posting to Bluesky: " + e.getMessage());
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to post to Bluesky: " + e.getMessage());
                allSuccess = false;
            }
        }

        if (postToMastodon) {
            System.out.println("Posting to Mastodon...");
        }

        if (allSuccess && (postToBluesky || postToMastodon)) {
            postContent.clear();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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