package com.stc.usermanagement;

import com.stc.util.DatabaseHelper; // Yeni paketimizden import ediyoruz
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthenticationService {

    private static User currentUser;

    public static boolean login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
            	if (rs.next()) {
            	    int id = rs.getInt("id");
            	    String roleFromDB = rs.getString("role"); // DB'den rolü oku
            	    String pass = rs.getString("password");

            	    // Rol bilgisine göre ilgili alt sınıfı instantiate ediyoruz
            	    if ("EMPLOYEE".equalsIgnoreCase(roleFromDB)) {
            	        currentUser = new Employee(id, username, pass);
            	    } else if ("SUPERVISOR".equalsIgnoreCase(roleFromDB)) {
            	        currentUser = new Supervisor(id, username, pass);
            	    } else if ("HR".equalsIgnoreCase(roleFromDB)) {
            	        currentUser = new HR(id, username, pass);
            	    }
            	    
            	    return true;
            	}
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static User getCurrentUser() {
        return currentUser;
    }
}