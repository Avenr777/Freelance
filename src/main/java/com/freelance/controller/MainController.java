package com.freelance.controller;

import com.freelance.dao.ClientDao;
import com.freelance.dao.InvoiceDao;
import com.freelance.dao.ProjectDao;
import com.freelance.dao.WorkLogDao;
import com.freelance.model.*;
import com.freelance.service.InvoiceService;
import com.freelance.service.PdfExportService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML private TreeView<Object> clientProjectTree;
    @FXML private Label headerTitle;
    @FXML private Label headerSubtitle;
    @FXML private Label uninvoicedTotalLabel;
    @FXML private TabPane mainTabPane;

    // Work Logs Table
    @FXML private TableView<WorkLog> workLogTable;
    @FXML private TableColumn<WorkLog, String> colLogDate;
    @FXML private TableColumn<WorkLog, String> colLogProject;
    @FXML private TableColumn<WorkLog, String> colLogDescription;
    @FXML private TableColumn<WorkLog, String> colLogHours;
    @FXML private TableColumn<WorkLog, String> colLogRate;
    @FXML private TableColumn<WorkLog, String> colLogAmount;

    // Invoices Table
    @FXML private TableView<Invoice> invoiceTable;
    @FXML private TableColumn<Invoice, String> colInvId;
    @FXML private TableColumn<Invoice, String> colInvProject;
    @FXML private TableColumn<Invoice, String> colInvIssueDate;
    @FXML private TableColumn<Invoice, String> colInvDueDate;
    @FXML private TableColumn<Invoice, String> colInvAmount;
    @FXML private TableColumn<Invoice, Object> colInvStatus;

    // DAOs & Services
    private final ClientDao clientDao = new ClientDao();
    private final ProjectDao projectDao = new ProjectDao();
    private final WorkLogDao workLogDao = new WorkLogDao();
    private final InvoiceDao invoiceDao = new InvoiceDao();
    private final InvoiceService invoiceService = new InvoiceService();

    // Data lists
    private final ObservableList<WorkLog> workLogList = FXCollections.observableArrayList();
    private final ObservableList<Invoice> invoiceList = FXCollections.observableArrayList();

    private Client selectedClient = null;
    private Project selectedProject = null;

    @FXML
    public void initialize() {
        setupTables();
        setupTree();
        refreshAllData();
    }

    private void setupTables() {
        // Work Log Columns
        colLogDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getWorkDate().format(DATE_FORMATTER)));
        colLogProject.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getProject() != null ? cell.getValue().getProject().getProjectName() : ""));
        colLogDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colLogHours.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getHoursWorked().toString()));
        colLogRate.setCellValueFactory(cell -> new SimpleStringProperty(
                "₹" + (cell.getValue().getProject() != null ? cell.getValue().getProject().getHourlyRate() : "0.00")));
        colLogAmount.setCellValueFactory(cell -> new SimpleStringProperty(
                "₹" + cell.getValue().calculateAmount().setScale(2, RoundingMode.HALF_UP).toString()));

        workLogTable.setItems(workLogList);

        // Invoice Columns
        colInvId.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFormattedInvoiceNumber()));
        colInvProject.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getProject() != null ? cell.getValue().getProject().getProjectName() : ""));
        colInvIssueDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getIssueDate().format(DATE_FORMATTER)));
        colInvDueDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDueDate().format(DATE_FORMATTER)));
        colInvAmount.setCellValueFactory(cell -> new SimpleStringProperty(
                "₹" + cell.getValue().getTotalAmount().setScale(2, RoundingMode.HALF_UP).toString()));

        colInvStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Invoice inv = getTableRow().getItem();
                    InvoiceStatus status = inv.getEffectiveStatus();
                    Label statusLabel = new Label(status.getDisplayName().toUpperCase());
                    statusLabel.getStyleClass().add("status-chip");
                    switch (status) {
                        case PAID -> statusLabel.getStyleClass().add("status-paid");
                        case OVERDUE -> statusLabel.getStyleClass().add("status-overdue");
                        case SENT -> statusLabel.getStyleClass().add("status-sent");
                        default -> statusLabel.getStyleClass().add("status-draft");
                    }
                    setGraphic(statusLabel);
                    setText(null);
                }
            }
        });

        invoiceTable.setItems(invoiceList);
    }

    private void setupTree() {
        TreeItem<Object> root = new TreeItem<>("Root");
        clientProjectTree.setRoot(root);
        clientProjectTree.setShowRoot(false);

        clientProjectTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Object value = newVal.getValue();
                if (value instanceof Client c) {
                    selectedClient = c;
                    selectedProject = null;
                    headerTitle.setText("Client: " + c.getName());
                    headerSubtitle.setText((c.getEmail() != null ? c.getEmail() : "") + " | All Projects");
                } else if (value instanceof Project p) {
                    selectedProject = p;
                    selectedClient = p.getClient();
                    headerTitle.setText(p.getProjectName());
                    headerSubtitle.setText("Client: " + (p.getClient() != null ? p.getClient().getName() : "") +
                            " | ₹" + p.getHourlyRate() + "/hr");
                }
            } else {
                selectedClient = null;
                selectedProject = null;
                headerTitle.setText("All Clients & Projects");
                headerSubtitle.setText("Select a client or project from the left rail.");
            }
            filterLedgerTables();
        });
    }

    public void refreshAllData() {
        refreshTree();
        filterLedgerTables();
    }

    private void refreshTree() {
        TreeItem<Object> root = clientProjectTree.getRoot();
        root.getChildren().clear();

        try {
            List<Client> clients = clientDao.findAll();
            for (Client client : clients) {
                TreeItem<Object> clientItem = new TreeItem<>(client);
                List<Project> projects = projectDao.findByClientId(client.getClientId());
                for (Project proj : projects) {
                    clientItem.getChildren().add(new TreeItem<>(proj));
                }
                clientItem.setExpanded(true);
                root.getChildren().add(clientItem);
            }
        } catch (Exception e) {
            logger.error("Failed to refresh navigation tree: {}", e.getMessage(), e);
        }
    }

    private void filterLedgerTables() {
        try {
            List<WorkLog> logs;
            List<Invoice> invoices;

            if (selectedProject != null) {
                logs = workLogDao.findByProjectId(selectedProject.getProjectId());
                invoices = invoiceDao.findByProjectId(selectedProject.getProjectId());
            } else if (selectedClient != null) {
                logs = new ArrayList<>();
                invoices = new ArrayList<>();
                List<Project> projs = projectDao.findByClientId(selectedClient.getClientId());
                for (Project p : projs) {
                    logs.addAll(workLogDao.findByProjectId(p.getProjectId()));
                    invoices.addAll(invoiceDao.findByProjectId(p.getProjectId()));
                }
            } else {
                logs = new ArrayList<>();
                invoices = invoiceDao.findAll();
                List<Project> allProjs = projectDao.findAll();
                for (Project p : allProjs) {
                    logs.addAll(workLogDao.findByProjectId(p.getProjectId()));
                }
            }

            workLogList.setAll(logs);
            invoiceList.setAll(invoices);

            // Calculate Uninvoiced Total (Sum of WorkLogs not yet invoiced)
            BigDecimal totalUninvoiced = BigDecimal.ZERO;
            for (WorkLog log : logs) {
                totalUninvoiced = totalUninvoiced.add(log.calculateAmount());
            }
            uninvoicedTotalLabel.setText("₹" + totalUninvoiced.setScale(2, RoundingMode.HALF_UP).toString());

        } catch (Exception e) {
            logger.error("Error filtering tables: {}", e.getMessage(), e);
        }
    }

    // --- Action Handlers ---

    @FXML
    private void handleAddClient() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ClientDialog.fxml"));
            Parent root = loader.load();
            ClientDialogController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Add New Client");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            controller.setDialogStage(stage);
            stage.showAndWait();

            if (controller.isSaveClicked()) {
                refreshAllData();
            }
        } catch (Exception e) {
            logger.error("Failed to open Client Dialog: {}", e.getMessage(), e);
        }
    }

    @FXML
    private void handleAddProject() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProjectDialog.fxml"));
            Parent root = loader.load();
            ProjectDialogController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Add New Project");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            controller.setDialogStage(stage, selectedClient);
            stage.showAndWait();

            if (controller.isSaveClicked()) {
                refreshAllData();
            }
        } catch (Exception e) {
            logger.error("Failed to open Project Dialog: {}", e.getMessage(), e);
        }
    }

    @FXML
    private void handleAddWorkLog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/WorkLogDialog.fxml"));
            Parent root = loader.load();
            WorkLogDialogController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Log Hours");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            controller.setDialogStage(stage, selectedProject);
            stage.showAndWait();

            if (controller.isSaveClicked()) {
                filterLedgerTables();
            }
        } catch (Exception e) {
            logger.error("Failed to open WorkLog Dialog: {}", e.getMessage(), e);
        }
    }

    @FXML
    private void handleDeleteWorkLog() {
        WorkLog selected = workLogTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a work log to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Work Log");
        confirm.setContentText("Are you sure you want to delete this work log entry?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                workLogDao.delete(selected.getLogId());
                filterLedgerTables();
            } catch (Exception e) {
                logger.error("Error deleting work log: {}", e.getMessage(), e);
                showAlert("Error", "Could not delete work log.");
            }
        }
    }

    @FXML
    private void handleGenerateInvoice() {
        Project targetProject = selectedProject;
        if (targetProject == null) {
            // Pick project from list or current selected log
            WorkLog selectedLog = workLogTable.getSelectionModel().getSelectedItem();
            if (selectedLog != null) {
                targetProject = selectedLog.getProject();
            }
        }

        if (targetProject == null) {
            showAlert("Project Required", "Please select a specific Project from the left rail to generate an invoice.");
            return;
        }

        try {
            Invoice invoice = invoiceService.generateInvoice(targetProject, 14);
            filterLedgerTables();
            mainTabPane.getSelectionModel().select(1); // Switch to Invoices Tab

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Invoice Generated");
            info.setHeaderText("Invoice " + invoice.getFormattedInvoiceNumber() + " Created!");
            info.setContentText("Total amount: ₹" + invoice.getTotalAmount() + "\nDue date: " + invoice.getDueDate().format(DATE_FORMATTER));
            info.showAndWait();
        } catch (Exception e) {
            logger.error("Error generating invoice: {}", e.getMessage(), e);
            showAlert("Error", "Could not generate invoice: " + e.getMessage());
        }
    }

    @FXML
    private void handleMarkAsSent() {
        Invoice selected = invoiceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an invoice from the table.");
            return;
        }
        try {
            invoiceService.markAsSent(selected.getInvoiceId());
            filterLedgerTables();
        } catch (Exception e) {
            logger.error("Error updating status: {}", e.getMessage(), e);
        }
    }

    @FXML
    private void handleMarkAsPaid() {
        Invoice selected = invoiceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an invoice from the table.");
            return;
        }
        try {
            invoiceService.markAsPaid(selected.getInvoiceId());
            filterLedgerTables();
        } catch (Exception e) {
            logger.error("Error updating status: {}", e.getMessage(), e);
        }
    }

    @FXML
    private void handleExportPdf() {
        Invoice selected = invoiceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an invoice to export to PDF.");
            return;
        }

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Invoice PDF");
            fileChooser.setInitialFileName("Invoice_" + selected.getFormattedInvoiceNumber().replace("#", "") + ".pdf");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Documents", "*.pdf"));
            
            Stage stage = (Stage) invoiceTable.getScene().getWindow();
            File dest = fileChooser.showSaveDialog(stage);

            if (dest != null) {
                List<WorkLog> logs = workLogDao.findByProjectId(selected.getProject().getProjectId());
                PdfExportService.exportInvoiceToPdf(selected, logs, dest);

                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("PDF Exported");
                info.setHeaderText("PDF Invoice Generated Successfully");
                info.setContentText("Saved to: " + dest.getAbsolutePath());
                info.showAndWait();
            }
        } catch (Exception e) {
            logger.error("Error exporting PDF: {}", e.getMessage(), e);
            showAlert("PDF Export Error", "Could not export PDF: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteInvoice() {
        Invoice selected = invoiceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an invoice to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Invoice " + selected.getFormattedInvoiceNumber());
        confirm.setContentText("Are you sure you want to delete this invoice?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                invoiceDao.delete(selected.getInvoiceId());
                filterLedgerTables();
            } catch (Exception e) {
                logger.error("Error deleting invoice: {}", e.getMessage(), e);
                showAlert("Error", "Could not delete invoice.");
            }
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
