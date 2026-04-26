package com.stc.usermanagement;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private void handleLogin() {
        String user = usernameField.getText();
        String pass = passwordField.getText();
        
        System.out.println("Giriş yapılıyor: " + user);
        
        
        if(user.equals("admin") && pass.equals("1234")) {
            System.out.println("Giriş Başarılı!");
        } else {
            System.out.println("Hatalı Giriş!");
        }
    }
}