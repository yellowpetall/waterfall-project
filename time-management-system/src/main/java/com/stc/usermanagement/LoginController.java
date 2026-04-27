package com.stc.usermanagement;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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
        
        System.out.println("Giriş denemesi: " + user);
        
        
        boolean isAuthenticated = authService.authenticate(user, pass);
        
        if (isAuthenticated) {
            // Get the user object of the person who has logged in.
            User loggedInUser = AuthenticationService.getCurrentUser();
            
            System.out.println("Giriş Başarılı!");
            System.out.println("Kullanıcı ID: " + loggedInUser.getUserId());
            System.out.println("Rol: " + loggedInUser.getClass().getSimpleName());
            
            // Go to the user dash board
            showInfoAlert("Başarılı", "Hoş geldiniz, " + user + "!");
            
        } else {
            System.out.println("Hatalı Giriş!");
            showErrorAlert("Giriş Başarısız", "Kullanıcı adı veya şifre hatalı.");
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