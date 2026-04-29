package com.stc.usermanagement;

import com.stc.timeManagement.CustomerProject;
import com.stc.timeManagement.TimeEntry;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.sql.Time;
import java.util.Date;

public class EmployeeDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private ComboBox<String> monthSelectionCombo;
    @FXML private TableView<TimeEntry> timeTable;

    @FXML private TableColumn<TimeEntry,String> projectCol;
    @FXML private TableColumn<TimeEntry,String> dateCol;
    @FXML private TableColumn<TimeEntry,String> startCol;
    @FXML private TableColumn<TimeEntry,String> endCol;
    @FXML private TableColumn<TimeEntry,String> breakCol;
    @FXML private TableColumn<TimeEntry,String> totalCol;
    @FXML private TableColumn<TimeEntry,String> commentCol;

    @FXML private Button addRowBtn;
    @FXML private Button submitBtn;
    @FXML private Label statusLabel;

    private ObservableList<TimeEntry> entries =
            FXCollections.observableArrayList();

    private boolean submitted = false;

    @FXML
    public void initialize() {

        welcomeLabel.setText("Welcome Employee!");

        monthSelectionCombo.getItems().addAll(
                "January","February","March","April","May","June",
                "July","August","September","October","November","December"
        );

        setupColumns();

        timeTable.setItems(entries);
        timeTable.setEditable(true);
    }

    private void setupColumns() {

        projectCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getProject().getProjectName()
                ));

        dateCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getDate().toString()
                ));

        startCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getStartTime().toString()
                ));

        endCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getEndTime().toString()
                ));

        breakCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        String.valueOf(data.getValue().getBreakDuration())
                ));

        totalCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        String.format("%.2f",
                                data.getValue().getWorkingHours())
                ));

        commentCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getComment()
                ));

        makeEditable(projectCol);
        makeEditable(dateCol);
        makeEditable(startCol);
        makeEditable(endCol);
        makeEditable(breakCol);
        makeEditable(commentCol);
    }

    private void makeEditable(TableColumn<TimeEntry,String> col) {

        col.setCellFactory(TextFieldTableCell.forTableColumn());

        col.setOnEditCommit(event -> {

            if(submitted) return;

            TimeEntry row = event.getRowValue();
            String value = event.getNewValue();

            try {

                if(col == projectCol) {
                	row.setProject(
                		    new CustomerProject(
                		        row.getProject().getProjectId(),
                		        value
                		    )
                		);
                }

                else if(col == dateCol) {
                    row.setDate(new Date());
                }

                else if(col == startCol) {
                    row.setStartTime(Time.valueOf(value));
                }

                else if(col == endCol) {
                    row.setEndTime(Time.valueOf(value));
                }

                else if(col == breakCol) {
                    row.setBreakDuration(
                            Double.parseDouble(value));
                }

                else if(col == commentCol) {
                    row.setComment(value);
                }

                timeTable.refresh();

            } catch (Exception e) {
                statusLabel.setText("Invalid input!");
                timeTable.refresh();
            }

        });
    }

    @FXML
    private void handleAddRow() {

        if(submitted) return;

        entries.add(
                new TimeEntry(
                        new Date(),
                        Time.valueOf("08:00:00"),
                        Time.valueOf("17:00:00"),
                        1.0,
                        "",
                        new CustomerProject(1, "Project A")
                )
        );
    }

    @FXML
    private void handleSubmit() {

        try {

            BufferedWriter writer =
                    new BufferedWriter(
                            new FileWriter("src/main/resources/timesheets.txt", true)
                    );

            writer.write("----- Monthly Submission -----");
            writer.newLine();

            writer.write("Month: " +
                    monthSelectionCombo.getValue());
            writer.newLine();

            for(TimeEntry entry : entries) {

                writer.write(
                        entry.getProject().getProjectName()
                        + " , " +
                        entry.getDate()
                        + " , " +
                        entry.getStartTime()
                        + " , " +
                        entry.getEndTime()
                        + " , " +
                        entry.getBreakDuration()
                        + " , " +
                        entry.getWorkingHours()
                        + " , " +
                        entry.getComment()
                );

                writer.newLine();
            }

            writer.newLine();
            writer.newLine();

            writer.close();

            submitted = true;

            timeTable.setEditable(false);
            addRowBtn.setDisable(true);
            submitBtn.setDisable(true);

            statusLabel.setText("Status: Submitted");

        } catch (IOException e) {

            statusLabel.setText("Error saving file!");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleMonthChange() {

        submitted = false;

        addRowBtn.setDisable(false);
        submitBtn.setDisable(false);
        timeTable.setEditable(true);

        entries.clear();

        statusLabel.setText(
                "Status: Draft for " +
                monthSelectionCombo.getValue()
        );
    }

    @FXML
    private void handleLogout() {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/com/stc/usermanagement/login.fxml"
                            )
                    );

            Parent root = loader.load();

            Stage stage =
                    (Stage) addRowBtn.getScene().getWindow();

            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.setTitle("Login");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}