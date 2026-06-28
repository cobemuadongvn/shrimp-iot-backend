package com.example.shrimpiot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class DeploymentConfigTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    @Test
    @SuppressWarnings("unchecked")
    void renderBlueprintDefinesTwoSafeDockerServices() throws IOException {
        Path blueprintPath = ROOT.resolve("render.yaml");
        assertTrue(Files.isRegularFile(blueprintPath));

        Map<String, Object> blueprint = new Yaml().load(Files.readString(blueprintPath));
        List<Map<String, Object>> services = (List<Map<String, Object>>) blueprint.get("services");
        assertNotNull(services);
        assertEquals(2, services.size());

        Map<String, Object> backend = requireService(services, "shrimp-iot-backend");
        Map<String, Object> ai = requireService(services, "shrimp-iot-ai");

        assertDockerWebService(backend, "/api/health/ready");
        assertDockerWebService(ai, "/health");
        assertEquals("free", backend.get("plan"));
        assertEquals("free", ai.get("plan"));
        assertEquals("checksPass", backend.get("autoDeployTrigger"));
        assertEquals("checksPass", ai.get("autoDeployTrigger"));

        assertTrue(Files.isRegularFile(ROOT.resolve(stripDot((String) backend.get("dockerfilePath")))));
        assertTrue(Files.isDirectory(ROOT.resolve(stripDot((String) backend.get("dockerContext")))));
        assertTrue(Files.isRegularFile(ROOT.resolve(stripDot((String) ai.get("dockerfilePath")))));
        assertTrue(Files.isDirectory(ROOT.resolve(stripDot((String) ai.get("dockerContext")))));

        List<Map<String, Object>> backendEnv = (List<Map<String, Object>>) backend.get("envVars");
        Set<String> requiredSecrets = Set.of(
                "DB_HOST", "DB_PORT", "DB_NAME", "DB_USER", "DB_PASSWORD", "IOT_API_KEY",
                "CORS_ALLOWED_ORIGINS", "MQTT_BROKER_URL", "MQTT_USERNAME",
                "MQTT_PASSWORD", "AI_SERVICE_URL", "OPENAI_API_KEY"
        );
        for (String key : requiredSecrets) {
            Map<String, Object> variable = backendEnv.stream()
                    .filter(item -> key.equals(item.get("key")))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Missing Render env var: " + key));
            assertEquals(Boolean.FALSE, variable.get("sync"), key + " must be populated in Render Dashboard");
            assertFalse(variable.containsKey("value"), key + " must not be committed with a value");
        }
    }

    @Test
    void dockerfilesUseHealthChecksAndNonRootUsers() throws IOException {
        String backend = Files.readString(ROOT.resolve("Dockerfile"));
        String ai = Files.readString(ROOT.resolve("ai-service/Dockerfile"));

        assertTrue(backend.contains("HEALTHCHECK"));
        assertTrue(backend.contains("/api/health/ready"));
        assertTrue(backend.contains("USER spring:spring"));
        assertTrue(ai.contains("HEALTHCHECK"));
        assertTrue(ai.contains("/health"));
        assertTrue(ai.contains("USER appuser"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void ciAndRenderPortConfigurationArePresent() throws IOException {
        String workflow = Files.readString(ROOT.resolve(".github/workflows/ci.yml"));
        String application = Files.readString(ROOT.resolve("src/main/resources/application.yml"));
        Map<String, Object> workflowYaml = new Yaml().load(workflow);
        Map<String, Object> jobs = (Map<String, Object>) workflowYaml.get("jobs");

        assertEquals(4, jobs.size());
        assertTrue(workflow.contains("mvn -B -ntp test"));
        assertTrue(workflow.contains("docker/build-push-action@v6"));
        assertTrue(workflow.contains("assert all(h['models'].values())"));
        assertTrue(application.contains("${PORT:${SERVER_PORT:8080}}"));
    }

    @Test
    void databaseMigrationIsVersionedAndProductionSafe() throws IOException {
        String pom = Files.readString(ROOT.resolve("pom.xml"));
        String application = Files.readString(ROOT.resolve("src/main/resources/application.yml"));
        String testApplication = Files.readString(ROOT.resolve("src/test/resources/application-test.yml"));
        String baseline = Files.readString(
                ROOT.resolve("src/main/resources/db/migration/V1__baseline_schema.sql"));

        assertTrue(pom.contains("spring-boot-starter-flyway"));
        assertTrue(pom.contains("flyway-database-postgresql"));
        assertTrue(application.contains("ddl-auto: ${JPA_DDL_AUTO:validate}"));
        assertTrue(application.contains("baseline-on-migrate: ${FLYWAY_BASELINE_ON_MIGRATE:false}"));
        assertTrue(application.contains("sslmode=${DB_SSLMODE:disable}"));
        assertTrue(testApplication.contains("flyway:\n    enabled: false"));
        assertEquals(23, occurrences(baseline, "CREATE TABLE public."));
        assertEquals(22, occurrences(baseline, "GENERATED BY DEFAULT AS IDENTITY"));
        assertEquals(63, occurrences(baseline, " ADD CONSTRAINT "));
        assertFalse(baseline.contains("INSERT INTO"));
    }

    private Map<String, Object> requireService(List<Map<String, Object>> services, String name) {
        return services.stream()
                .filter(service -> name.equals(service.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing Render service: " + name));
    }

    private void assertDockerWebService(Map<String, Object> service, String healthPath) {
        assertEquals("web", service.get("type"));
        assertEquals("docker", service.get("runtime"));
        assertEquals("singapore", service.get("region"));
        assertEquals(healthPath, service.get("healthCheckPath"));
    }

    private String stripDot(String value) {
        return value.startsWith("./") ? value.substring(2) : value;
    }

    private int occurrences(String value, String needle) {
        return (value.length() - value.replace(needle, "").length()) / needle.length();
    }
}
