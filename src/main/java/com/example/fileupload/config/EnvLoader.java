package com.example.fileupload.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class EnvLoader {

    /**
     * Loads key-value pairs from a .env file into System properties
     * if they are not already set in the environment.
     */
    public static void load() {
        File envFile = new File(".env");
        if (!envFile.exists() || !envFile.isFile()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }

                int equalIndex = line.indexOf('=');
                String key = line.substring(0, equalIndex).trim();
                String value = line.substring(equalIndex + 1).trim();

                // Strip surrounding single or double quotes
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'"))) {
                    if (value.length() >= 2) {
                        value = value.substring(1, value.length() - 1);
                    }
                }

                // Set as system property if not already defined as an environment variable or system property
                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Unable to read .env file: " + e.getMessage());
        }
    }
}
