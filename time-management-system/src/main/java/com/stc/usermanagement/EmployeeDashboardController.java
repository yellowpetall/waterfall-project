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
    @FXML private TableColumn<TimeEntry, String> dateCol;
    @FXML private TableColumn<TimeEntry, Time> startCol;
    @FXML private TableColumn<TimeEntry, Time> endCol;
    @FXML private TableColumn<TimeEntry, Double> breakCol;
    @FXML private TableColumn<TimeEntry, Double> totalCol;
    @FXML private TableColumn<TimeEntry, String> commentCol;

    @FXML private Button addRowBtn;
    @FXML private Button submitBtn;

    private ObservableList<TimeEntry> masterData = FXCollections.observableArrayList();

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

        // Start Time & End Time
        StringConverter<Time> timeConverter = new StringConverter<>() {
            @Override
            public String toString(Time time) {
                return time == null ? "" : time.toString();
            }

            @Override
            public Time fromString(String string) {
                return Time.valueOf(string);
            }
        };

        startCol.setCellFactory(TextFieldTableCell.forTableColumn(timeConverter));
        endCol.setCellFactory(TextFieldTableCell.forTableColumn(timeConverter));


        // Break Duration
        breakCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        breakCol.setOnEditCommit(event -> event.getRowValue().setBreakDuration(event.getNewValue()));
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

        String sql = """
            SELECT *
            FROM time_entries
            WHERE user_id = ?
            AND TRIM(UPPER(TO_CHAR(entry_date, 'MONTH'))) = ?
            ORDER BY entry_date
            """;

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1,
                    AuthenticationService.getCurrentUser().getUserId());

            pstmt.setString(2, monthName.toUpperCase());

            ResultSet rs = pstmt.executeQuery();
            
            

            while (rs.next()) {
            	
            	if ("SUBMITTED".equals(rs.getString("status"))) {
            	    disableEditing();
            	}

                // Project nesnesi oluştur
                CustomerProject project =
                        new CustomerProject(rs.getString("project_name"));

                // TimeEntry oluştur
                TimeEntry entry = new TimeEntry(
                        rs.getDate("entry_date"),
                        rs.getTime("start_time"),
                        rs.getTime("end_time"),
                        rs.getDouble("break_duration"),
                        rs.getString("comment"),
                        project
                );

                // DB id'sini sete et
                entry.setId(rs.getInt("id"));


                masterData.add(entry);
            }

            timeTable.setItems(masterData);

        } catch (SQLException e) {

            e.printStackTrace();

            showAlert(
                    "Database Error",
                    "Veriler yüklenirken hata oluştu:\n" + e.getMessage()
            );
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
            SET status = 'SUBMITTED'
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