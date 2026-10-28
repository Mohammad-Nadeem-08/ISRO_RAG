package com.spacedoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@SpringBootApplication
public class SpaceDocIntelligenceApplication {

    public static void main(String[] args) {
        loadDotEnvIfPresent();
        SpringApplication.run(SpaceDocIntelligenceApplication.class, args);
    }

    private static void loadDotEnvIfPresent() {
        try {
            File envFile = new File(".env");
            if (envFile.exists() && envFile.isFile()) {
                List<String> lines = Files.readAllLines(envFile.toPath());
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                        int eqIdx = trimmed.indexOf('=');
                        String key = trimmed.substring(0, eqIdx).trim();
                        String val = trimmed.substring(eqIdx + 1).trim();
                        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                            val = val.substring(1, val.length() - 1);
                        }
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, val);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
