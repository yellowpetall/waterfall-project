package com.stc.usermanagement;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class AuthenticationService {
    private static User currentUser; 

    public boolean authenticate(String username, String password) {
        // Get the file under resources
        try (java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("users.txt");
             BufferedReader br = new BufferedReader(new java.io.InputStreamReader(is))) {
            
            if (is == null) {
                System.err.println("Error: users.txt not found in resources folder!");
                return false;
            }

            String line;
            while ((line = br.readLine()) != null) {
                // split the csv data
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    int id = Integer.parseInt(parts[0].trim());
                    String storedUser = parts[1].trim();
                    String storedPass = parts[2].trim();
                    String role = parts[3].trim().toUpperCase();

                    if (storedUser.equals(username) && storedPass.equals(password)) {
                        currentUser = switch (role) {
                            case "EMPLOYEE" -> new Employee(id, storedUser, storedPass);
                            case "SUPERVISOR" -> new Supervisor(id, storedUser, storedPass);
                            case "HR" -> new HR(id, storedUser, storedPass);
						default -> throw new IllegalArgumentException("Unexpected value: " + role);
                        };
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("File Reading Error: " + e.getMessage());
        }
        return false;
    }

    public static User getCurrentUser() {
        return currentUser;
    }
}