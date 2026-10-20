package com.freelance.controller;

import com.freelance.dao.ClientDao;
import com.freelance.dao.ProjectDao;
import com.freelance.model.Client;
import com.freelance.model.Project;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

public class ProjectDialogController {
    private static final Logger logger = LoggerFactory.getLogger(ProjectDialogController.class);

    @FXML private ComboBox<Client> clientComboBox;
    @FXML private TextField projectNameField;
    @FXML private TextField hourlyRateField;

    private Stage dialogStage;
    private Project project;
    private boolean saveClicked = false;
    private final ClientDao clientDao = new ClientDao();
    private final ProjectDao projectDao = new ProjectDao();

    public void setDialogStage(Stage dialogStage, Client preselectedClient) {
        this.dialogStage = dialogStage;
        try {
            List<Client> clients = clientDao.findAll();
            clientComboBox.setItems(FXCollections.observableArrayList(clients));
            if (preselectedClient != null) {
                clientComboBox.getSelectionModel().select(preselectedClient);
            }
        } catch (Exception e) {
            logger.error("Failed to load clients: {}", e.getMessage());
        }
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    public Project getProject() {
        return project;
    }

    @FXML
    private void handleSave() {
        Client client = clientComboBox.getValue();
        String name = projectNameField.getText() != null ? projectNameField.getText().trim() : "";
        String rateText = hourlyRateField.getText() != null ? hourlyRateField.getText().trim() : "";

        if (client == null) {
            showAlert("Invalid Input", "Please select a client.");
            return;
        }
        if (name.isEmpty()) {
            showAlert("Invalid Input", "Please enter a project name.");
            return;
        }

        BigDecimal rate;
        try {
            rate = new BigDecimal(rateText);
            if (rate.compareTo(BigDecimal.ZERO) < 0) {
                showAlert("Invalid Input", "Hourly rate must be non-negative.");
                return;
            }
        } catch (Exception e) {
            showAlert("Invalid Input", "Please enter a valid numeric hourly rate (e.g. 60.00).");
            return;
        }

        try {
            project = new Project(client, name, rate);
            project = projectDao.save(project);
            saveClicked = true;
            dialogStage.close();
        } catch (Exception e) {
            logger.error("Failed to save project: {}", e.getMessage(), e);
            showAlert("Database Error", "Could not save project: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private void showAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Input Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
