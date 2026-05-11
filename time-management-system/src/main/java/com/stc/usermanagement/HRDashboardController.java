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
import java.util.ArrayList;
import java.util.List;

public class HRDashboardController {

    private static final List<String> ROLES = List.of("EMPLOYEE", "SUPERVISOR", "HR");
    private static final List<String> STATUSES = List.of("ALL", "DRAFT", "SUBMITTED", "APPROVED", "REJECTED");

    @FXML private Label welcomeLabel;

    // --- User Management tab ---
    @FXML private TextField newUsernameField;
    @FXML private PasswordField newPasswordField;
    @FXML private ComboBox<String> newRoleCombo;
    @FXML private TableView<UserRow> usersTable;
    @FXML private TableColumn<UserRow, Number> userIdCol;
    @FXML private TableColumn<UserRow, String> userNameCol;
    @FXML private TableColumn<UserRow, String> userRoleCol;
    @FXML private TableColumn<UserRow, String> userActionsCol;
    @FXML private Label userStatusLabel;

    // --- Timesheets tab ---
    @FXML private ComboBox<String> tsStatusFilterCombo;
    @FXML private ComboBox<String> tsEmployeeFilterCombo;
    @FXML private Label tsCountLabel;
    @FXML private TableView<TimesheetRow> timesheetsTable;
    @FXML private TableColumn<TimesheetRow, String> tsEmployeeCol;
    @FXML private TableColumn<TimesheetRow, String> tsProjectCol;
    @FXML private TableColumn<TimesheetRow, String> tsDateCol;
    @FXML private TableColumn<TimesheetRow, String> tsStartCol;
    @FXML private TableColumn<TimesheetRow, String> tsEndCol;
    @FXML private TableColumn<TimesheetRow, Number> tsBreakCol;
    @FXML private TableColumn<TimesheetRow, Number> tsTotalCol;
    @FXML private TableColumn<TimesheetRow, String> tsStatusCol;
    @FXML private TableColumn<TimesheetRow, String> tsReasonCol;

    // --- Flex Time tab ---
    @FXML private TextField targetHoursField;
    @FXML private TableView<FlexRow> flexTable;
    @FXML private TableColumn<FlexRow, String> flexEmployeeCol;
    @FXML private TableColumn<FlexRow, Number> flexActualCol;
    @FXML private TableColumn<FlexRow, Number> flexTargetCol;
    @FXML private TableColumn<FlexRow, Number> flexBalanceCol;

    private final ObservableList<UserRow> userRows = FXCollections.observableArrayList();
    private final ObservableList<TimesheetRow> timesheetRows = FXCollections.observableArrayList();
    private final ObservableList<FlexRow> flexRows = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        User user = AuthenticationService.getCurrentUser();
        if (user != null) {
            welcomeLabel.setText("Welcome, " + user.getUsername());
        }

        setupUserTab();
        setupTimesheetTab();
        setupFlexTab();

        loadUsers();
        loadTimesheets();
        loadFlexSummary();
    }

    // ============================================================
    //  USER MANAGEMENT
    // ============================================================

    private void setupUserTab() {
        newRoleCombo.setItems(FXCollections.observableArrayList(ROLES));
        newRoleCombo.setValue("EMPLOYEE");

        userIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        userNameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        userRoleCol.setCellValueFactory(new PropertyValueFactory<>("role"));

        userActionsCol.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(ROLES));
            private final Button applyBtn = new Button("Apply");
            private final HBoxLike box = new HBoxLike(combo, applyBtn);
            {
                applyBtn.setOnAction(e -> {
                    UserRow row = getTableView().getItems().get(getIndex());
                    String newRole = combo.getValue();
                    if (newRole != null && !newRole.equals(row.getRole())) {
                        if (changeUserRole(row.getId(), newRole)) {
                            row.setRole(newRole);
                            userStatusLabel.setText("Role updated for " + row.getUsername() + " → " + newRole);
                        }
                    }
                });
            }
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                combo.setValue(getTableView().getItems().get(getIndex()).getRole());
                setGraphic(box);
            }
        });

        usersTable.setItems(userRows);
    }

    private void loadUsers() {
        userRows.clear();
        String sql = "SELECT id, username, role FROM users ORDER BY id";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            List<String> usernames = new ArrayList<>();
            usernames.add("ALL");
            while (rs.next()) {
                UserRow row = new UserRow();
                row.setId(rs.getInt("id"));
                row.setUsername(rs.getString("username"));
                row.setRole(rs.getString("role"));
                userRows.add(row);
                usernames.add(row.getUsername());
            }
            tsEmployeeFilterCombo.setItems(FXCollections.observableArrayList(usernames));
            if (tsEmployeeFilterCombo.getValue() == null) tsEmployeeFilterCombo.setValue("ALL");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "DB Error", "Could not load users:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleAddUser() {
        String username = newUsernameField.getText() == null ? "" : newUsernameField.getText().trim();
        String password = newPasswordField.getText() == null ? "" : newPasswordField.getText();
        String role = newRoleCombo.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            showAlert(Alert.AlertType.WARNING, "Missing Fields",
                    "Username, password and role are required.");
            return;
        }

        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role);
            pstmt.executeUpdate();
            newUsernameField.clear();
            newPasswordField.clear();
            userStatusLabel.setText("User added: " + username + " (" + role + ")");
            loadUsers();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "DB Error",
                    "Could not add user:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteUser() {
        UserRow selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection",
                    "Select a user to delete.");
            return;
        }
        User current = AuthenticationService.getCurrentUser();
        if (current != null && current.getUserId() == selected.getId()) {
            showAlert(Alert.AlertType.WARNING, "Not Allowed",
                    "You cannot delete the user you are logged in as.");
            return;
        }
        if (!confirm("Delete user '" + selected.getUsername() + "'? This also removes their time entries.")) return;

        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, selected.getId());
            pstmt.executeUpdate();
            userStatusLabel.setText("Deleted: " + selected.getUsername());
            loadUsers();
            loadTimesheets();
            loadFlexSummary();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "DB Error",
                    "Could not delete user:\n" + e.getMessage());
        }
    }

    private boolean changeUserRole(int userId, String newRole) {
        String sql = "UPDATE users SET role = ? WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newRole);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "DB Error",
                    "Could not change role:\n" + e.getMessage());
            return false;
        }
    }

    // ============================================================
    //  ALL TIMESHEETS
    // ============================================================

    private void setupTimesheetTab() {
        tsStatusFilterCombo.setItems(FXCollections.observableArrayList(STATUSES));
        tsStatusFilterCombo.setValue("ALL");

        tsEmployeeCol.setCellValueFactory(new PropertyValueFactory<>("employee"));
        tsProjectCol.setCellValueFactory(new PropertyValueFactory<>("project"));
        tsDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        tsStartCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        tsEndCol.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        tsBreakCol.setCellValueFactory(new PropertyValueFactory<>("breakDuration"));
        tsTotalCol.setCellValueFactory(new PropertyValueFactory<>("workingHours"));
        tsStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        tsReasonCol.setCellValueFactory(new PropertyValueFactory<>("rejectionReason"));

        tsStatusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) { setText(null); setStyle(""); return; }
                setText(value);
                switch (value.toUpperCase()) {
                    case "DRAFT"     -> setStyle("-fx-text-fill: #757575;");
                    case "SUBMITTED" -> setStyle("-fx-text-fill: #f57c00; -fx-font-weight: bold;");
                    case "APPROVED"  -> setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                    case "REJECTED"  -> setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
                    default          -> setStyle("");
                }
            }
        });

        timesheetsTable.setItems(timesheetRows);
    }

    @FXML
    private void handleTimesheetFilter() { loadTimesheets(); }
    @FXML
    private void handleTimesheetRefresh() { loadTimesheets(); }

    private void loadTimesheets() {
        timesheetRows.clear();
        String statusFilter = tsStatusFilterCombo.getValue();
        String employeeFilter = tsEmployeeFilterCombo.getValue();

        StringBuilder sql = new StringBuilder("""
            SELECT te.id, te.project_name, te.entry_date, te.start_time, te.end_time,
                   te.break_duration, te.working_hours, te.comment, te.status,
                   te.rejection_reason, u.username
            FROM time_entries te
            JOIN users u ON u.id = te.user_id
            WHERE 1=1
            """);
        List<Object> params = new ArrayList<>();
        if (statusFilter != null && !"ALL".equals(statusFilter)) {
            sql.append(" AND te.status = ? ");
            params.add(statusFilter);
        }
        if (employeeFilter != null && !"ALL".equals(employeeFilter)) {
            sql.append(" AND u.username = ? ");
            params.add(employeeFilter);
        }
        sql.append(" ORDER BY te.entry_date DESC, u.username ");

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    TimesheetRow row = new TimesheetRow();
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
                    row.setStatus(rs.getString("status"));
                    String reason = rs.getString("rejection_reason");
                    row.setRejectionReason(reason == null ? "" : reason);
                    timesheetRows.add(row);
                }
            }
            tsCountLabel.setText(timesheetRows.size() + " entries");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "DB Error",
                    "Could not load timesheets:\n" + e.getMessage());
        }
    }

    // ============================================================
    //  FLEX TIME SUMMARY
    // ============================================================

    private void setupFlexTab() {
        flexEmployeeCol.setCellValueFactory(new PropertyValueFactory<>("employee"));
        flexActualCol.setCellValueFactory(new PropertyValueFactory<>("actualHours"));
        flexTargetCol.setCellValueFactory(new PropertyValueFactory<>("targetHours"));
        flexBalanceCol.setCellValueFactory(new PropertyValueFactory<>("balance"));

        flexBalanceCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) { setText(null); setStyle(""); return; }
                double v = value.doubleValue();
                setText(String.format("%+.2f", v));
                if (v > 0)      setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                else if (v < 0) setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
                else            setStyle("-fx-text-fill: #757575;");
            }
        });

        flexTable.setItems(flexRows);
    }

    @FXML
    private void handleFlexRefresh() { loadFlexSummary(); }

    private void loadFlexSummary() {
        flexRows.clear();
        double target;
        try {
            target = Double.parseDouble(targetHoursField.getText().trim());
        } catch (NumberFormatException e) {
            target = 160.0;
        }

        String sql = """
            SELECT u.username,
                   COALESCE(SUM(CASE WHEN te.status = 'APPROVED' THEN te.working_hours ELSE 0 END), 0) AS actual
            FROM users u
            LEFT JOIN time_entries te ON te.user_id = u.id
            WHERE u.role = 'EMPLOYEE'
            GROUP BY u.username
            ORDER BY u.username
            """;
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                FlexRow row = new FlexRow();
                row.setEmployee(rs.getString("username"));
                double actual = rs.getDouble("actual");
                row.setActualHours(actual);
                row.setTargetHours(target);
                row.setBalance(actual - target);
                flexRows.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "DB Error",
                    "Could not load flex summary:\n" + e.getMessage());
        }
    }

    // ============================================================
    //  COMMON
    // ============================================================

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("STC Time Management System");
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
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

    // ----- Row models -----

    public static class UserRow {
        private final SimpleIntegerProperty id = new SimpleIntegerProperty();
        private final SimpleStringProperty username = new SimpleStringProperty();
        private final SimpleStringProperty role = new SimpleStringProperty();
        public int getId() { return id.get(); }
        public void setId(int v) { id.set(v); }
        public String getUsername() { return username.get(); }
        public void setUsername(String v) { username.set(v); }
        public String getRole() { return role.get(); }
        public void setRole(String v) { role.set(v); }
    }

    public static class TimesheetRow {
        private final SimpleStringProperty employee = new SimpleStringProperty();
        private final SimpleStringProperty project = new SimpleStringProperty();
        private final SimpleStringProperty date = new SimpleStringProperty();
        private final SimpleStringProperty startTime = new SimpleStringProperty();
        private final SimpleStringProperty endTime = new SimpleStringProperty();
        private final SimpleDoubleProperty breakDuration = new SimpleDoubleProperty();
        private final SimpleDoubleProperty workingHours = new SimpleDoubleProperty();
        private final SimpleStringProperty status = new SimpleStringProperty();
        private final SimpleStringProperty rejectionReason = new SimpleStringProperty();
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
        public String getStatus() { return status.get(); }
        public void setStatus(String v) { status.set(v); }
        public String getRejectionReason() { return rejectionReason.get(); }
        public void setRejectionReason(String v) { rejectionReason.set(v); }
    }

    public static class FlexRow {
        private final SimpleStringProperty employee = new SimpleStringProperty();
        private final SimpleDoubleProperty actualHours = new SimpleDoubleProperty();
        private final SimpleDoubleProperty targetHours = new SimpleDoubleProperty();
        private final SimpleDoubleProperty balance = new SimpleDoubleProperty();
        public String getEmployee() { return employee.get(); }
        public void setEmployee(String v) { employee.set(v); }
        public double getActualHours() { return actualHours.get(); }
        public void setActualHours(double v) { actualHours.set(v); }
        public double getTargetHours() { return targetHours.get(); }
        public void setTargetHours(double v) { targetHours.set(v); }
        public double getBalance() { return balance.get(); }
        public void setBalance(double v) { balance.set(v); }
    }

    /** Tiny inline HBox helper to avoid importing javafx.scene.layout.HBox in row cells. */
    private static class HBoxLike extends javafx.scene.layout.HBox {
        HBoxLike(javafx.scene.Node... nodes) {
            super(8.0);
            getChildren().addAll(nodes);
        }
    }
}
