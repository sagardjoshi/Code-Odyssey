package com.css.challenge.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private final Properties properties;

    private static AppConfig appConfig = null;

    public static AppConfig getInstance() {
        if(appConfig == null) appConfig = new AppConfig();
        return appConfig;
    }
    private AppConfig() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find application.properties");
            }
            properties.load(input);
        } catch (IOException ex) {
            throw new RuntimeException("Error loading application.properties", ex);
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

}
