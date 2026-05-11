package com.stc.usermanagement;

import com.stc.util.DatabaseHelper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.sql.*;
import java.util.List;

public class SupervisorDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;
    @FXML private Label countLabel;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TableView<PendingEntryRow> entriesTable;
    @FXML private TableColumn<PendingEntryRow, String> employeeCol;
    @FXML private TableColumn<PendingEntryRow, String> projectCol;
    @FXML private TableColumn<PendingEntryRow, String> dateCol;
    @FXML private TableColumn<PendingEntryRow, String> startCol;
    @FXML private TableColumn<PendingEntryRow, String> endCol;
    @FXML private TableColumn<PendingEntryRow, Number> breakCol;
    @FXML private TableColumn<PendingEntryRow, Number> totalCol;
    @FXML private TableColumn<PendingEntryRow, String> commentCol;
    @FXML private TableColumn<PendingEntryRow, String> statusCol;
    @FXML private TextArea rejectionReasonArea;
    @FXML private Button approveBtn;
    @FXML private Button rejectBtn;

    private final ObservableList<PendingEntryRow> rows = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        User user = AuthenticationService.getCurrentUser();
        if (user != null) {
            welcomeLabel.setText("Welcome, " + user.getUsername());
        }

        employeeCol.setCellValueFactory(new PropertyValueFactory<>("employee"));
        projectCol.setCellValueFactory(new PropertyValueFactory<>("project"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        startCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        endCol.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        breakCol.setCellValueFactory(new PropertyValueFactory<>("breakDuration"));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("workingHours"));
        commentCol.setCellValueFactory(new PropertyValueFactory<>("comment"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(value);
                switch (value.toUpperCase()) {
                    case "SUBMITTED" -> setStyle("-fx-text-fill: #f57c00; -fx-font-weight: bold;");
                    case "APPROVED"  -> setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                    case "REJECTED"  -> setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
                    default          -> setStyle("");
                }
            }
        });

        statusFilterCombo.setItems(FXCollections.observableArrayList(
            "SUBMITTED", "APPROVED", "REJECTED", "ALL"
        ));
        statusFilterCombo.setValue("SUBMITTED");

        entriesTable.setItems(rows);
        entriesTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        loadEntries();
    }

    @FXML
    private void handleFilterChange() {
        loadEntries();
    }

    @FXML
    private void handleRefresh() {
        loadEntries();
    }

    private void loadEntries() {
        rows.clear();
        String filter = statusFilterCombo.getValue();

        StringBuilder sql = new StringBuilder("""
            SELECT te.id, te.user_id, te.project_name, te.entry_date,
                   te.start_time, te.end_time, te.break_duration,
                   te.working_hours, te.comment, te.status,
                   u.username
            FROM time_entries te
            JOIN users u ON u.id = te.user_id
            """);
        if (!"ALL".equalsIgnoreCase(filter)) {
            sql.append(" WHERE te.status = ? ");
        }
        sql.append(" ORDER BY te.entry_date DESC, u.username ");

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            if (!"ALL".equalsIgnoreCase(filter)) {
                pstmt.setString(1, filter);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    PendingEntryRow row = new PendingEntryRow();
                    row.setId(rs.getInt("id"));
                    row.setUserId(rs.getInt("user_id"));
                    row.setEmployee(rs.getString("username"));
                    row.setProject(rs.getString("project_name"));
                    Date d = rs.getDate("entry_date");
                    row.setDate(d == null ? "" : d.toString());
                    Time st = rs.getTime("start_time");
                    Time et = rs.getTime("end_time");
                    row.setStartTime(st == null ? "" : st.toString());
                    row.setEndTime(et == null ? "" : et.toString());
                    row.setBreakDuration(rs.getDouble("break_duration"));
                    row.setWorkingHours(rs.getDouble("working_hours"));
                    row.setComment(rs.getString("comment"));
                    row.setStatus(rs.getString("status"));
                    rows.add(row);
                }
            }
            countLabel.setText(rows.size() + " entries");
            statusLabel.setText("");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not load entries:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleApprove() {
        List<PendingEntryRow> selected = entriesTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Selection",
                    "Please select at least one entry to approve.");
            return;
        }
        if (!confirm("Approve " + selected.size() + " entry/entries?")) return;

        String sql = "UPDATE time_entries SET status = 'APPROVED', rejection_reason = NULL WHERE id = ?";
        int updated = batchUpdateStatus(sql, selected);
        if (updated >= 0) {
            statusLabel.setText(updated + " entry/entries approved.");
            statusLabel.setTextFill(javafx.scene.paint.Color.web("#2e7d32"));
            loadEntries();
        }
    }

    @FXML
    private void handleReject() {
        List<PendingEntryRow> selected = entriesTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Selection",
                    "Please select at least one entry to reject.");
            return;
        }
        String reason = rejectionReasonArea.getText() == null ? "" : rejectionReasonArea.getText().trim();
        if (reason.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Reason Required",
                    "Please enter a rejection reason before rejecting.");
            return;
        }
        if (!confirm("Reject " + selected.size() + " entry/entries?")) return;

        String sql = "UPDATE time_entries SET status = 'REJECTED', rejection_reason = ? WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (PendingEntryRow row : selected) {
                pstmt.setString(1, reason);
                pstmt.setInt(2, row.getId());
                pstmt.addBatch();
            }
            int[] results = pstmt.executeBatch();
            int total = 0;
            for (int r : results) if (r >= 0 || r == Statement.SUCCESS_NO_INFO) total++;
            statusLabel.setText(total + " entry/entries rejected.");
            statusLabel.setTextFill(javafx.scene.paint.Color.web("#c62828"));
            rejectionReasonArea.clear();
            loadEntries();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not reject entries:\n" + e.getMessage());
        }
    }

    private int batchUpdateStatus(String sql, List<PendingEntryRow> selected) {
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (PendingEntryRow row : selected) {
                pstmt.setInt(1, row.getId());
                pstmt.addBatch();
            }
            int[] results = pstmt.executeBatch();
            int total = 0;
            for (int r : results) if (r >= 0 || r == Statement.SUCCESS_NO_INFO) total++;
            return total;
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not update entries:\n" + e.getMessage());
            return -1;
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("STC Time Management System");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(null);
        return alert.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static class PendingEntryRow {
        private final SimpleIntegerProperty id = new SimpleIntegerProperty();
        private final SimpleIntegerProperty userId = new SimpleIntegerProperty();
        private final SimpleStringProperty employee = new SimpleStringProperty();
        private final SimpleStringProperty project = new SimpleStringProperty();
        private final SimpleStringProperty date = new SimpleStringProperty();
        private final SimpleStringProperty startTime = new SimpleStringProperty();
        private final SimpleStringProperty endTime = new SimpleStringProperty();
        private final SimpleDoubleProperty breakDuration = new SimpleDoubleProperty();
        private final SimpleDoubleProperty workingHours = new SimpleDoubleProperty();
        private final SimpleStringProperty comment = new SimpleStringProperty();
        private final SimpleStringProperty status = new SimpleStringProperty();

        public int getId() { return id.get(); }
        public void setId(int v) { id.set(v); }
        public int getUserId() { return userId.get(); }
        public void setUserId(int v) { userId.set(v); }
        public String getEmployee() { return employee.get(); }
        public void setEmployee(String v) { employee.set(v); }
        public String getProject() { return project.get(); }
        public void setProject(String v) { project.set(v); }
        public String getDate() { return date.get(); }
        public void setDate(String v) { date.set(v); }
        public String getStartTime() { return startTime.get(); }
        public void setStartTime(String v) { startTime.set(v); }
        public String getEndTime() { return endTime.get(); }
        public void setEndTime(String v) { endTime.set(v); }
        public double getBreakDuration() { return breakDuration.get(); }
        public void setBreakDuration(double v) { breakDuration.set(v); }
        public double getWorkingHours() { return workingHours.get(); }
        public void setWorkingHours(double v) { workingHours.set(v); }
        public String getComment() { return comment.get(); }
        public void setComment(String v) { comment.set(v); }
        public String getStatus() { return status.get(); }
        public void setStatus(String v) { status.set(v); }
    }
}
