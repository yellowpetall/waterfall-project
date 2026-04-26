package com.stc.usermanagement;

public abstract class User {
    private int userId;
    private String username;
    private String password;
    private String role; // "Employee", "Supervisor", "HR"

    public User(int userId, String username, String password, String role) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // Giriş işlemi için dökümanda belirtilen metod
    public boolean login(String username, String password) {
        return this.username.equals(username) && this.password.equals(password);
    }

    // Getter ve Setter metodlarını buraya ekleyebilirsin
    public String getRole() {
        return role;
    }
}
