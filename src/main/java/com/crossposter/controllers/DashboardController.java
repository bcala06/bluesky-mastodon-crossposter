package com.crossposter.controllers;

import com.crossposter.services.AuthSession;
import com.crossposter.services.BlueskyClient;
import com.crossposter.services.MastodonClient;
import com.crossposter.services.ServiceRegistry;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Optional;

public class DashboardController {

    private static final int MAX_CHARS = 280;

    private final BlueskyClient blueskyClient = ServiceRegistry.getBlueskyClient();
    private final MastodonClient mastodonClient = ServiceRegistry.getMastodonClient();

    @FXML private Button blueskyButton;
    @FXML private Button mastodonButton;
    @FXML private TextArea postContent;
    @FXML private CheckBox blueskyCheck;
    @FXML private CheckBox mastodonCheck;
    @FXML private Label charCountLabel;
    @FXML private Button postButton;

    @FXML
     public void initialize() {
        updateButtons();
        setupCharacterCountListener(); 

        final boolean blueskyConnected = ServiceRegistry.getBlueskySession() != null;
        final boolean mastodonConnected = ServiceRegistry.getMastodonSession() != null;

        if (blueskyCheck != null) blueskyCheck.setDisable(!blueskyConnected);
        if (mastodonCheck != null) mastodonCheck.setDisable(!mastodonConnected);

        if (postButton != null) {
            postButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> {
                    boolean b = (blueskyConnected && blueskyCheck.isSelected());
                    boolean m = (mastodonConnected && mastodonCheck.isSelected());
                    return !(b || m); // disable when neither valid target is selected
               },
                blueskyCheck.selectedProperty(),
                mastodonCheck.selectedProperty()
            ));
        }
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
 

    private void setupCharacterCountListener() {
        postContent.textProperty().addListener((observable, oldValue, newValue) -> {
            int currentLength = newValue.length();
          
            if (currentLength > MAX_CHARS) {
                String truncated = newValue.substring(0, MAX_CHARS);
                postContent.setText(truncated);
                currentLength = MAX_CHARS; 
            }

            charCountLabel.setText(currentLength + " / " + MAX_CHARS);


            if (currentLength == MAX_CHARS) {
                charCountLabel.setStyle("-fx-text-fill: red; -fx-font-size: 13px;");
            } else {
                charCountLabel.setStyle("-fx-text-fill: #86868b; -fx-font-size: 13px;");
            }
        });
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
                    showAlert(Alert.AlertType.WARNING, "Bluesky Not Authenticated",
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
                showAlert(Alert.AlertType.ERROR, "Error Posting to Bluesky", e.getMessage());
                allSuccess = false;
            }
        }

        if (postToMastodon) {
            try {
                AuthSession session = ServiceRegistry.getMastodonSession();
                if (session == null || session.accessToken == null || session.instanceUrl == null) {
                    showAlert(Alert.AlertType.WARNING, "Mastodon Not Authenticated",
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
                showAlert(Alert.AlertType.ERROR, "Error Posting to Mastodon", e.getMessage());
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

        if (message.length() < 100) {
            alert.setContentText(message);
        } else {
            alert.setContentText("An error occurred. See details for more information.");

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.write(message);
            String exceptionText = sw.toString();

            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);

            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(new Label("The full error message is:"), 0, 0);
            expContent.add(textArea, 0, 1);

            alert.getDialogPane().setExpandableContent(expContent);
        }

        alert.showAndWait();
    }


    @FXML
    public void openSettings(MouseEvent event) {
        System.out.println("[Dashboard] Navigating to Settings...");
        SceneManager.switchScene("/fxml/settings.fxml", "Settings");
    }
}