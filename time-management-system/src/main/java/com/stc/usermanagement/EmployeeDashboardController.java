package com.stc.usermanagement;

import com.stc.timeManagement.CustomerProject;
import com.stc.timeManagement.TimeEntry;
import com.stc.util.DatabaseHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;

import java.sql.*;
import java.time.LocalDate;

public class EmployeeDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private ComboBox<String> monthSelectionCombo;
    @FXML private TableView<TimeEntry> timeTable;
    
    // Sütunlar - projectCol tipini Object yaparak karmaşayı önlüyoruz
    @FXML private TableColumn<TimeEntry, Object> projectCol; 
    @FXML private TableColumn<TimeEntry, java.util.Date> dateCol;
    @FXML private TableColumn<TimeEntry, Time> startCol;
    @FXML private TableColumn<TimeEntry, Time> endCol;
    @FXML private TableColumn<TimeEntry, Double> breakCol;
    @FXML private TableColumn<TimeEntry, Double> totalCol;
    @FXML private TableColumn<TimeEntry, String> commentCol;

    @FXML private Button addRowBtn;
    @FXML private Button submitBtn;
    @FXML private Label statusLabel;
    @FXML private Label totalHoursLabel;
    @FXML private Label flexBalanceLabel;
    @FXML private Label reasonLabel;

    private static final double TARGET_HOURS_PER_MONTH = 160.0;

    private ObservableList<TimeEntry> masterData = FXCollections.observableArrayList();

    private String currentAggregateStatus = "DRAFT";
    private String currentLastReason = null;

    @FXML
    public void initialize() {
        User user = AuthenticationService.getCurrentUser();
        if (user != null) {
            welcomeLabel.setText("Welcome, " + user.getUsername());
        }

        // TableView sütun eşleştirmeleri
        projectCol.setCellValueFactory(new PropertyValueFactory<>("project"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        startCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        endCol.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        breakCol.setCellValueFactory(new PropertyValueFactory<>("breakDuration"));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("workingHours"));
        commentCol.setCellValueFactory(new PropertyValueFactory<>("comment"));

        setupEditableColumns();

        monthSelectionCombo.setItems(FXCollections.observableArrayList(
            "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", 
            "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"
        ));
        
        String currentMonth = LocalDate.now().getMonth().name();
        monthSelectionCombo.setValue(currentMonth);

        handleMonthChange();
    }

    private void setupEditableColumns() {
        // --- PROJECT SÜTUNU DÜZENLEME (HATA BURADAYDI) ---
        projectCol.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<Object>() {
            @Override
            public String toString(Object object) {
                if (object == null) return "";
                if (object instanceof CustomerProject) {
                    return ((CustomerProject) object).getName();
                }
                return object.toString();
            }

            @Override
            public Object fromString(String string) {
                // Hücreye yeni isim yazıldığında yeni proje nesnesi oluşturur
                return new CustomerProject(string);
            }
        }));
        
        projectCol.setOnEditCommit(event -> {
            // Edit bitince nesneyi TimeEntry içine setle
            event.getRowValue().setProject((CustomerProject) event.getNewValue());
        });

        // Comment Sütunu
        commentCol.setCellFactory(TextFieldTableCell.forTableColumn());
        commentCol.setOnEditCommit(event -> event.getRowValue().setComment(event.getNewValue()));

        // Date Sütunu (yyyy-MM-dd)
        StringConverter<java.util.Date> dateConverter = new StringConverter<>() {
            private final java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
            @Override
            public String toString(java.util.Date d) {
                return d == null ? "" : fmt.format(d);
            }
            @Override
            public java.util.Date fromString(String s) {
                if (s == null || s.trim().isEmpty()) return null;
                try { return fmt.parse(s.trim()); }
                catch (java.text.ParseException ex) { return null; }
            }
        };
        dateCol.setCellFactory(TextFieldTableCell.forTableColumn(dateConverter));
        dateCol.setOnEditCommit(event -> {
            java.util.Date d = event.getNewValue();
            if (d == null) {
                timeTable.refresh();
                showAlert("Invalid Date", "Use yyyy-MM-dd format (e.g. 2026-05-11).");
                return;
            }
            event.getRowValue().setDate(d);
            timeTable.refresh();
        });

        // Start Time & End Time
        StringConverter<Time> timeConverter = new StringConverter<>() {
            @Override
            public String toString(Time time) {
                return time == null ? "" : time.toString();
            }

            @Override
            public Time fromString(String string) {
                return parseTimeSafe(string);
            }
        };

        startCol.setCellFactory(TextFieldTableCell.forTableColumn(timeConverter));
        endCol.setCellFactory(TextFieldTableCell.forTableColumn(timeConverter));

        startCol.setOnEditCommit(event -> {
            Time t = event.getNewValue();
            if (t == null) {
                timeTable.refresh();
                showAlert("Invalid Time", "Use HH:MM or HH:MM:SS (e.g. 08:00 or 08:30:00).");
                return;
            }
            event.getRowValue().setStartTime(t);
            timeTable.refresh();
            recomputeSummary();
        });

        endCol.setOnEditCommit(event -> {
            Time t = event.getNewValue();
            if (t == null) {
                timeTable.refresh();
                showAlert("Invalid Time", "Use HH:MM or HH:MM:SS (e.g. 17:00 or 17:30:00).");
                return;
            }
            event.getRowValue().setEndTime(t);
            timeTable.refresh();
            recomputeSummary();
        });


        // Break Duration
        breakCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        breakCol.setOnEditCommit(event -> {
            event.getRowValue().setBreakDuration(event.getNewValue());
            timeTable.refresh();
            recomputeSummary();
        });
    }

    // ... handleMonthChange ve enableEditing metodları aynı kalabilir ...

    @FXML
    private void handleMonthChange() {
        String selectedMonth = monthSelectionCombo.getValue();
        String currentMonth = LocalDate.now().getMonth().name();
        boolean isCurrentMonth = selectedMonth.equals(currentMonth);
        enableEditing(isCurrentMonth);
        loadDataFromDatabase(selectedMonth);
    }

    private void enableEditing(boolean active) {
        timeTable.setEditable(active);
        addRowBtn.setDisable(!active);
        submitBtn.setDisable(!active);
    }

    private void loadDataFromDatabase(String monthName) {

        masterData.clear();
        currentAggregateStatus = "DRAFT";
        currentLastReason = null;
        updateSummary("DRAFT", 0.0, null);

        String sql = """
            SELECT *
            FROM time_entries
            WHERE user_id = ?
            AND TRIM(UPPER(TO_CHAR(entry_date, 'MONTH'))) = ?
            ORDER BY entry_date
            """;

        String aggregateStatus = "DRAFT";
        String lastRejectionReason = null;
        double totalHours = 0.0;

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1,
                    AuthenticationService.getCurrentUser().getUserId());

            pstmt.setString(2, monthName.toUpperCase());

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {

                String rowStatus = rs.getString("status");
                aggregateStatus = aggregateStatuses(aggregateStatus, rowStatus);

                String reason = rs.getString("rejection_reason");
                if ("REJECTED".equalsIgnoreCase(rowStatus) && reason != null && !reason.isBlank()) {
                    lastRejectionReason = reason;
                }

                CustomerProject project =
                        new CustomerProject(rs.getString("project_name"));

                TimeEntry entry = new TimeEntry(
                        rs.getDate("entry_date"),
                        rs.getTime("start_time"),
                        rs.getTime("end_time"),
                        rs.getDouble("break_duration"),
                        rs.getString("comment"),
                        project
                );

                entry.setId(rs.getInt("id"));
                totalHours += entry.getWorkingHours();

                masterData.add(entry);
            }

            timeTable.setItems(masterData);

            // Sadece tamamen onaylanmış aylar kilitlensin.
            // DRAFT / SUBMITTED / REJECTED → kullanıcı düzenleyip yeniden gönderebilir.
            String currentMonth = LocalDate.now().getMonth().name();
            boolean isCurrentMonth = monthName.equalsIgnoreCase(currentMonth);
            if ("APPROVED".equals(aggregateStatus) || !isCurrentMonth) {
                disableEditing();
            } else {
                enableEditing(true);
            }

            this.currentAggregateStatus = aggregateStatus;
            this.currentLastReason = lastRejectionReason;
            updateSummary(aggregateStatus, totalHours, lastRejectionReason);

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    "Database Error",
                    "Veriler yüklenirken hata oluştu:\n" + e.getMessage()
            );
        }
    }

    private String aggregateStatuses(String current, String incoming) {
        if (incoming == null) return current;
        String inc = incoming.toUpperCase();
        if ("REJECTED".equals(inc)) return "REJECTED";
        if ("REJECTED".equals(current)) return "REJECTED";
        if ("SUBMITTED".equals(inc) && !"REJECTED".equals(current)) return "SUBMITTED";
        if ("APPROVED".equals(inc) && !"SUBMITTED".equals(current) && !"REJECTED".equals(current)) return "APPROVED";
        if ("DRAFT".equals(current)) return inc;
        return current;
    }

    private void updateSummary(String status, double totalHours, String lastRejectionReason) {
        statusLabel.setText("Status: " + status);
        switch (status) {
            case "DRAFT"     -> statusLabel.setTextFill(javafx.scene.paint.Color.web("#757575"));
            case "SUBMITTED" -> statusLabel.setTextFill(javafx.scene.paint.Color.web("#f57c00"));
            case "APPROVED"  -> statusLabel.setTextFill(javafx.scene.paint.Color.web("#2e7d32"));
            case "REJECTED"  -> statusLabel.setTextFill(javafx.scene.paint.Color.web("#c62828"));
            default          -> statusLabel.setTextFill(javafx.scene.paint.Color.web("#757575"));
        }

        totalHoursLabel.setText(String.format("%.2f h", totalHours));

        double balance = totalHours - TARGET_HOURS_PER_MONTH;
        flexBalanceLabel.setText(String.format("%+.2f h (target %.0f)", balance, TARGET_HOURS_PER_MONTH));
        if (balance > 0)      flexBalanceLabel.setTextFill(javafx.scene.paint.Color.web("#2e7d32"));
        else if (balance < 0) flexBalanceLabel.setTextFill(javafx.scene.paint.Color.web("#c62828"));
        else                  flexBalanceLabel.setTextFill(javafx.scene.paint.Color.web("#757575"));

        if (lastRejectionReason == null || lastRejectionReason.isBlank()) {
            reasonLabel.setText("");
        } else {
            reasonLabel.setText("Rejection reason: " + lastRejectionReason);
        }
    }

    @FXML
    private void handleAddRow() {
        CustomerProject defaultProject = new CustomerProject("New Project");
        TimeEntry newEntry = new TimeEntry(
            new java.util.Date(),
            Time.valueOf("09:00:00"),
            Time.valueOf("17:00:00"),
            1.0,
            "",
            defaultProject
        );
        masterData.add(newEntry);
        timeTable.scrollTo(newEntry); // Yeni eklenen satıra kaydır
        recomputeSummary();
    }

    private void recomputeSummary() {
        double total = masterData.stream().mapToDouble(TimeEntry::getWorkingHours).sum();
        updateSummary(currentAggregateStatus, total, currentLastReason);
    }

    @FXML
    private void handleSubmit() {

        String insertSql = """
            INSERT INTO time_entries
            (user_id, project_name, entry_date, start_time, end_time,
             break_duration, working_hours, comment, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        String updateSql = """
            UPDATE time_entries
            SET status = 'SUBMITTED', rejection_reason = NULL
            WHERE id = ?
            """;

        try (Connection conn = DatabaseHelper.getConnection();

             PreparedStatement insertStmt = conn.prepareStatement(insertSql);

             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            for (TimeEntry entry : masterData) {

                // Eğer kayıt zaten DB'deyse:
                // sadece status güncelle
                if (entry.getId() != 0) {

                    updateStmt.setInt(1, entry.getId());
                    updateStmt.addBatch();

                } else {

                    // Yeni kayıt -> INSERT
                    insertStmt.setInt(1,
                            AuthenticationService.getCurrentUser().getUserId());

                    String pName = (entry.getProject() != null)
                            ? entry.getProject().getName()
                            : "Unknown";

                    insertStmt.setString(2, pName);

                    insertStmt.setDate(3,
                            new java.sql.Date(entry.getDate().getTime()));

                    insertStmt.setTime(4, entry.getStartTime());
                    insertStmt.setTime(5, entry.getEndTime());

                    insertStmt.setDouble(6, entry.getBreakDuration());
                    insertStmt.setDouble(7, entry.getWorkingHours());

                    insertStmt.setString(8, entry.getComment());

                    insertStmt.setString(9, "SUBMITTED");

                    insertStmt.addBatch();
                }
            }

            insertStmt.executeBatch();
            updateStmt.executeBatch();

            disableEditing();

            showAlert("Success",
                    "Timesheet başarıyla submitted edildi.");

            loadDataFromDatabase(monthSelectionCombo.getValue());

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert("Error",
                    "Gönderim hatası: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("Login.fxml"));
            javafx.stage.Stage stage = (javafx.stage.Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(loader.load()));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private Time parseTimeSafe(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (s.isEmpty()) return null;
        if (s.matches("\\d{1,2}:\\d{2}")) s = (s.length() == 4 ? "0" + s : s) + ":00";
        else if (s.matches("\\d{1,2}:\\d{2}:\\d{2}") && s.length() == 7) s = "0" + s;
        try { return Time.valueOf(s); }
        catch (IllegalArgumentException ex) { return null; }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private void disableEditing() {

        timeTable.setEditable(false);

        addRowBtn.setDisable(true);
        submitBtn.setDisable(true);

        projectCol.setEditable(false);
        dateCol.setEditable(false);
        startCol.setEditable(false);
        endCol.setEditable(false);
        breakCol.setEditable(false);
        commentCol.setEditable(false);
    }
}