package com.example.app.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {
    private static final String DB_HOST = System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_NAME = System.getenv().getOrDefault("DB_NAME", "midgar37");
    private static final String USER = System.getenv().getOrDefault("DB_USER", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");
    private static final MyDatabase INSTANCE = new MyDatabase();
    private static String lastConnectionError = "";

    private static Connection connection = null;

    private MyDatabase() {}

    public static Connection getConnection() {
        try {
            if (connection != null && connection.isClosed()) {
                connection = null;
            }
        } catch (SQLException e) {
            connection = null;
        }

        if (connection == null) {
            connection = openConnection();
        }
        return connection;
    }

    public static String getLastConnectionError() {
        return lastConnectionError;
    }

    private static Connection openConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("❌ Driver MySQL non trouvé : " + e.getMessage());
            return null;
        }

        String envPort = System.getenv("DB_PORT");
        String[] ports = (envPort != null && !envPort.isBlank())
                ? new String[]{envPort.trim()}
                : new String[]{"3306", "4306"};

        SQLException lastError = null;

        for (String port : ports) {
            String url = "jdbc:mysql://" + DB_HOST + ":" + port + "/" + DB_NAME;
            try {
                Connection conn = DriverManager.getConnection(url, USER, PASSWORD);
                System.out.println("✅ Connexion à la base '" + DB_NAME + "' réussie sur MySQL:" + port + " !");
                lastConnectionError = "";
                return conn;
            } catch (SQLException e) {
                lastError = e;
            }
        }

        lastConnectionError = lastError != null ? lastError.getMessage() : "inconnue";
        System.out.println("❌ Erreur de connexion MySQL : " + lastConnectionError);
        System.out.println("   Assurez-vous que:");
        System.out.println("   - MySQL est en cours d'exécution (3306 ou 4306)");
        System.out.println("   - La base de données '" + DB_NAME + "' existe");
        System.out.println("   - L'utilisateur '" + USER + "' a accès à cette base");

        return null;
    }


    // Méthode singleton
    public static MyDatabase getInstance() {
        return INSTANCE;
    }

    // Méthode pour tester la connexion
    public static void main(String[] args) {
        getConnection();
    }
}