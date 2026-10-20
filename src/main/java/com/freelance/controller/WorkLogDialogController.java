package com.freelance.controller;

import com.freelance.dao.ProjectDao;
import com.freelance.dao.WorkLogDao;
import com.freelance.model.Project;
import com.freelance.model.WorkLog;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class WorkLogDialogController {
    private static final Logger logger = LoggerFactory.getLogger(WorkLogDialogController.class);

    @FXML private ComboBox<Project> projectComboBox;
    @FXML private DatePicker workDatePicker;
    @FXML private TextField hoursWorkedField;
    @FXML private TextArea descriptionArea;

    private Stage dialogStage;
    private WorkLog workLog;
    private boolean saveClicked = false;
    private final ProjectDao projectDao = new ProjectDao();
    private final WorkLogDao workLogDao = new WorkLogDao();

    public void setDialogStage(Stage dialogStage, Project preselectedProject) {
        this.dialogStage = dialogStage;
        workDatePicker.setValue(LocalDate.now());
        try {
            List<Project> projects = projectDao.findAll();
            projectComboBox.setItems(FXCollections.observableArrayList(projects));
            if (preselectedProject != null) {
                projectComboBox.getSelectionModel().select(preselectedProject);
            }
        } catch (Exception e) {
            logger.error("Failed to load projects: {}", e.getMessage());
        }
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    public WorkLog getWorkLog() {
        return workLog;
    }

    @FXML
    private void handleSave() {
        Project project = projectComboBox.getValue();
        LocalDate workDate = workDatePicker.getValue();
        String hoursText = hoursWorkedField.getText() != null ? hoursWorkedField.getText().trim() : "";
        String description = descriptionArea.getText() != null ? descriptionArea.getText().trim() : "";

        if (project == null) {
            showAlert("Invalid Input", "Please select a project.");
            return;
        }
        if (workDate == null) {
            showAlert("Invalid Input", "Please select a work date.");
            return;
        }

        BigDecimal hours;
        try {
            hours = new BigDecimal(hoursText);
            if (hours.compareTo(BigDecimal.ZERO) <= 0) {
                showAlert("Invalid Input", "Hours worked must be greater than zero.");
                return;
            }
        } catch (Exception e) {
            showAlert("Invalid Input", "Please enter a valid numeric hours value (e.g. 3.5).");
            return;
        }

        try {
            workLog = new WorkLog(project, workDate, hours, description);
            workLog = workLogDao.save(workLog);
            saveClicked = true;
            dialogStage.close();
        } catch (Exception e) {
            logger.error("Failed to save work log: {}", e.getMessage(), e);
            showAlert("Database Error", "Could not save work log: " + e.getMessage());
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
