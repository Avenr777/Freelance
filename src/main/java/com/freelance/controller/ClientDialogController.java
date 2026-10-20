package com.freelance.controller;

import com.freelance.dao.ClientDao;
import com.freelance.model.Client;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientDialogController {
    private static final Logger logger = LoggerFactory.getLogger(ClientDialogController.class);

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;

    private Stage dialogStage;
    private Client client;
    private boolean saveClicked = false;
    private final ClientDao clientDao = new ClientDao();

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    public Client getClient() {
        return client;
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText() != null ? nameField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        String phone = phoneField.getText() != null ? phoneField.getText().trim() : "";

        if (name.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText("Client Name Required");
            alert.setContentText("Please enter a valid client name.");
            alert.showAndWait();
            return;
        }

        try {
            client = new Client(name, email, phone);
            client = clientDao.save(client);
            saveClicked = true;
            dialogStage.close();
        } catch (Exception e) {
            logger.error("Failed to save client: {}", e.getMessage(), e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Could not save client");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }
}
