package com.stc.usermanagement;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // 1. Boş alan kontrolü (Validation)
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Eksik Bilgi", "Lütfen kullanıcı adı ve şifre giriniz.");
            return;
        }

        // 2. AuthenticationService üzerinden Veritabanı Kontrolü
        // Bu metot artık JDBC kullanarak DB'ye soruyor
        if (AuthenticationService.login(username, password)) {
            System.out.println("Giriş başarılı, yönlendiriliyor...");
            navigateToDashboard();
        } else {
            showAlert(Alert.AlertType.ERROR, "Giriş Başarısız", "Kullanıcı adı veya şifre hatalı!");
        }
    }

    private void navigateToDashboard() {
        try {
            User currentUser = AuthenticationService.getCurrentUser();
            String fxmlFile = "";

            // Nesnenin tipine göre yönlendirme yapıyoruz
            if (currentUser instanceof Employee) {
                fxmlFile = "EmployeeDashboard.fxml";
            } else if (currentUser instanceof Supervisor) {
                fxmlFile = "SupervisorDashboard.fxml";
            } else if (currentUser instanceof HR) {
                fxmlFile = "HRDashboard.fxml"; // HR için yeni ekran
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("STC System - " + currentUser.getClass().getSimpleName());
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Sistem Hatası", "Ekran yüklenemedi!");
        }
    }


    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}