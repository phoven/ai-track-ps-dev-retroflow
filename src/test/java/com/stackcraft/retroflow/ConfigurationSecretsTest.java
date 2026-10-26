package com.stackcraft.retroflow;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Standards §5.1: passwords, secrets, tokens and API keys never appear in application.properties.
 * A sensitive property may only be a {@code ${ENV_VAR}} placeholder (optionally with a default),
 * so the real value comes from the environment.
 */
class ConfigurationSecretsTest {

    private static final String CONFIG_FILE = "application.properties";
    private static final List<String> SENSITIVE_KEY_PARTS = List.of("password", "secret", "token", "api-key", "apikey");
    private static final Pattern ENVIRONMENT_PLACEHOLDER = Pattern.compile("^\\$\\{[A-Z0-9_]+(:[^}]*)?}$");

    @Test
    void applicationProperties_withSensitiveKeys_onlyContainEnvironmentPlaceholders() throws IOException {
        // Arrange
        Properties properties = load(CONFIG_FILE);

        // Act
        List<String> literalSecrets = properties.stringPropertyNames().stream()
                .filter(ConfigurationSecretsTest::isSensitive)
                .filter(key -> !ENVIRONMENT_PLACEHOLDER.matcher(properties.getProperty(key).strip()).matches())
                .sorted()
                .toList();

        // Assert
        assertThat(literalSecrets)
                .as("these properties must read their value from the environment, e.g. ${RETROFLOW_DB_PASSWORD:}")
                .isEmpty();
    }

    private static boolean isSensitive(String key) {
        String lowerCaseKey = key.toLowerCase(Locale.ROOT);
        return SENSITIVE_KEY_PARTS.stream().anyMatch(lowerCaseKey::contains);
    }

    private static Properties load(String resource) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = ConfigurationSecretsTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as(resource + " on the classpath").isNotNull();
            properties.load(in);
        }
        return properties;
    }

}

