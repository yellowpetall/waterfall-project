package com.stc.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseHelper {
    // Veritabanı bağlantı bilgileri
    private static final String URL = "jdbc:postgresql://localhost:5432/stc";
    private static final String USER = "postgres";
    private static final String PASS = "newpassword";

    /**
     * Veritabanı bağlantısını oluşturur ve döndürür.
     * @return Connection nesnesi
     * @throws SQLException Bağlantı hatası durumunda fırlatılır
     */
    public static Connection getConnection() throws SQLException {
        try {
            // PostgreSQL sürücüsünü belleğe yükle
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Hata: PostgreSQL Driver kütüphanesi projenizde eksik!");
            e.printStackTrace();
            throw new SQLException("Driver bulunamadı.");
        }
        
        // Bağlantıyı aç ve döndür
        return DriverManager.getConnection(URL, USER, PASS);
    }
}