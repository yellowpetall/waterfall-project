package com.stc.usermanagement;

public class Employee extends User {
    public Employee(int userId, String username, String password) {
        super(userId, username, password, "Employee");
    }
}