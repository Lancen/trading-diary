package com.tradingdiary.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads {@code .env} files into Spring Environment before configuration is resolved.
 * <p>
 * Load order: {@code .env} first, then {@code .env.{profile}} overrides.
 * The {@code .env} file is git-ignored; use {@code .env.example} as template.
 * <p>
 * Register via {@code META-INF/spring.factories} or {@code spring.factories}.
 */
public class DotenvConfig implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Logger log = LoggerFactory.getLogger(DotenvConfig.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment env = event.getEnvironment();
        Path projectDir = Paths.get(System.getProperty("user.dir"));

        Map<String, Object> dotenv = new HashMap<>();
        load(projectDir.resolve(".env"), dotenv);

        String[] profiles = env.getActiveProfiles();
        for (String profile : profiles) {
            load(projectDir.resolve(".env." + profile), dotenv);
        }

        if (!dotenv.isEmpty()) {
            env.getPropertySources().addFirst(new MapPropertySource("dotenv", dotenv));
            log.info("Loaded {} variables from .env files", dotenv.size());
        }
    }

    private void load(Path path, Map<String, Object> target) {
        if (!Files.isRegularFile(path)) return;
        try {
            for (String line : Files.readAllLines(path)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                // strip quotes
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                target.put(key, value);
            }
        } catch (IOException e) {
            log.debug("Could not read {}: {}", path, e.getMessage());
        }
    }
}
