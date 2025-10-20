package com.crossposter.controllers;

import com.crossposter.services.AuthSession;
import com.crossposter.services.BlueskyClient;
import com.crossposter.services.MastodonClient;
import com.crossposter.services.ServiceRegistry;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import java.util.Map;
import java.util.Optional;

public class DashboardController {

    private final BlueskyClient blueskyClient = ServiceRegistry.getBlueskyClient();
    private final MastodonClient mastodonClient = ServiceRegistry.getMastodonClient();

    @FXML private Button blueskyButton;
    @FXML private Button mastodonButton;
    @FXML private TextArea postContent;
    @FXML private CheckBox blueskyCheck;
    @FXML private CheckBox mastodonCheck;

    @FXML
    public void initialize() {
        updateButtons();
    }

    private void updateButtons() {
        if (ServiceRegistry.getBlueskySession() != null) {
            blueskyButton.setText("Disconnect");
            blueskyButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-background-radius: 5;");
        } else {
            blueskyButton.setText("Connect");
            blueskyButton.setStyle("-fx-background-color: #556CFF; -fx-text-fill: white; -fx-background-radius: 5;");
        }

        if (ServiceRegistry.getMastodonSession() != null) {
            mastodonButton.setText("Disconnect");
            mastodonButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-background-radius: 5;");
        } else {
            mastodonButton.setText("Connect");
            mastodonButton.setStyle("-fx-background-color: #556CFF; -fx-text-fill: white; -fx-background-radius: 5;");
        }
    }

    @FXML
    public void openBluesky(ActionEvent event) {
        if (ServiceRegistry.getBlueskySession() != null) {
            if (confirmDisconnect("Bluesky")) {
                ServiceRegistry.setBlueskySession(null);
                ServiceRegistry.setBlueskyPdsOrigin(null);
                System.out.println("[Dashboard] Bluesky disconnected.");
                updateButtons();
            }
        } else {
            System.out.println("[Dashboard] Bluesky Connect clicked");
            SceneManager.switchScene("/fxml/bluesky.fxml", "Bluesky Connect");
        }
    }

    @FXML
    public void openMastodon(ActionEvent event) {
        if (ServiceRegistry.getMastodonSession() != null) {
            if (confirmDisconnect("Mastodon")) {
                ServiceRegistry.setMastodonSession(null);
                System.out.println("[Dashboard] Mastodon disconnected.");
                updateButtons();
            }
        } else {
            System.out.println("[Dashboard] Mastodon Connect clicked");
            SceneManager.switchScene("/fxml/mastodon.fxml", "Mastodon Connect");
        }
    }

    private boolean confirmDisconnect(String platform) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Disconnect");
        alert.setHeaderText("Disconnect from " + platform + "?");
        alert.setContentText("Are you sure you want to disconnect your " + platform + " account?");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

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
                if (session == null || session.accessToken == null) {
                    showAlert(Alert.AlertType.WARNING, "Bluesky not authenticated",
                            "Please authenticate with Bluesky first.");
                    allSuccess = false;
                } else {
                    String pdsOrigin = ServiceRegistry.getBlueskyPdsOrigin();
                    if (pdsOrigin == null) {
                        showAlert(Alert.AlertType.WARNING, "Bluesky PDS Origin Missing",
                                "Bluesky session lacks PDS origin. Please re-authenticate.");
                        allSuccess = false;
                    } else {
                        Map<String, Object> result = blueskyClient.createPost(session, pdsOrigin, content);
                        System.out.println("Bluesky post result: " + result);
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Posted to Bluesky successfully!");
                    }
                }
            } catch (Exception e) {
                System.err.println("Error posting to Bluesky: " + e.getMessage());
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to post to Bluesky: " + e.getMessage());
                allSuccess = false;
            }
        }

        if (postToMastodon) {
            try {
                AuthSession session = ServiceRegistry.getMastodonSession();
                if (session == null || session.accessToken == null || session.instanceUrl == null) {
                    showAlert(Alert.AlertType.WARNING, "Mastodon not authenticated",
                            "Please authenticate with Mastodon first.");
                    allSuccess = false;
                } else {
                    Map<String, Object> result = mastodonClient.postStatus(session, content);
                    System.out.println("Mastodon post result: " + result);
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Posted to Mastodon successfully!");
                }
            } catch (Exception e) {
                System.err.println("Error posting to Mastodon: " + e.getMessage());
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to post to Mastodon: " + e.getMessage());
                allSuccess = false;
            }
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

    @FXML
    public void openSettings(MouseEvent event) {
        System.out.println("[Dashboard] Navigating to Settings...");
        SceneManager.switchScene("/fxml/settings.fxml", "Settings");
    }
}
