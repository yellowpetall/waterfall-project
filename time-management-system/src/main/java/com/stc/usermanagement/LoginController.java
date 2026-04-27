package com.stc.usermanagement;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    // Get the Authentication Service object which will carry out log in functionality.
    private AuthenticationService authService = new AuthenticationService();

    @FXML
    private void handleLogin() {
        String user = usernameField.getText();
        String pass = passwordField.getText();
        
        System.out.println("Login attempt: " + user);
        
        
        boolean isAuthenticated = authService.authenticate(user, pass);
        
        if (isAuthenticated) {
            // Get the user object of the person who has logged in.
            User loggedInUser = AuthenticationService.getCurrentUser();
            
            System.out.println("Login Successful!");
            System.out.println("User ID: " + loggedInUser.getUserId());
            System.out.println("Employee: " + loggedInUser.getClass().getSimpleName());
            
            // Go to the user dash board
            showInfoAlert("Successful", "Welcome, " + user + "!");
            
            try {
                // Load new FXML file
            	String dashboardName = "";
            	if(loggedInUser instanceof Employee){
            		dashboardName = "EmployeeDashboard.fxml";
            	}else if(loggedInUser instanceof Supervisor) {
            		dashboardName = "SupervisorDashboard.fxml";
            	}else {
            		dashboardName = "HRDashboard.fxml";
            	}
            	
                FXMLLoader loader = new FXMLLoader(getClass().getResource(dashboardName));
                Parent root = loader.load();

                // Find the current stage
                Stage stage = (Stage) usernameField.getScene().getWindow();

                // Put new stage in the screen
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.show();

            } catch (IOException e) {
                e.printStackTrace();
                showErrorAlert("Error", "New screen couldn't be loaded!");
            }
            
        } else {
            System.out.println("Login Failed!");
            showErrorAlert("Login Failed", "Username or password is incorrect.");
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}