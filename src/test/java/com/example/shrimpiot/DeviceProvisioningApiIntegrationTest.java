package com.example.shrimpiot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.example.shrimpiot.model.ApprovalStatus;
import com.example.shrimpiot.model.AuthToken;
import com.example.shrimpiot.model.Device;
import com.example.shrimpiot.model.Pond;
import com.example.shrimpiot.model.RoleName;
import com.example.shrimpiot.model.UserAccount;
import com.example.shrimpiot.model.UserPondAccess;
import com.example.shrimpiot.repository.AuthTokenRepository;
import com.example.shrimpiot.repository.DeviceRepository;
import com.example.shrimpiot.repository.DeviceProvisioningRepository;
import com.example.shrimpiot.repository.PondRepository;
import com.example.shrimpiot.repository.UserAccountRepository;
import com.example.shrimpiot.repository.UserPondAccessRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:provisioning_api;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "mqtt.enabled=false",
                "ai.enabled=false",
                "seed.demo-data.enabled=false"
        }
)
class DeviceProvisioningApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserAccountRepository userRepository;
    @Autowired
    private PondRepository pondRepository;
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private DeviceProvisioningRepository provisioningRepository;
    @Autowired
    private UserPondAccessRepository accessRepository;
    @Autowired
    private AuthTokenRepository tokenRepository;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void claimAndStatusContractWorksOverHttp() throws Exception {
        HttpResponse<String> readiness = get("/api/health/ready", null);
        assertEquals(200, readiness.statusCode());
        assertEquals("UP", json.readTree(readiness.body()).path("data").path("database").asText());

        UserAccount admin = saveUser("provision_admin", RoleName.ADMIN);
        UserAccount owner = saveUser("provision_owner", RoleName.USER);
        UserAccount otherOwner = saveUser("provision_other", RoleName.USER);

        Pond pond = new Pond();
        pond.setName("Provisioning API pond");
        pond = pondRepository.save(pond);
        grantOwner(owner, pond);
        grantOwner(otherOwner, pond);

        Device device = new Device();
        device.setDeviceId("device_api_test");
        device.setName("Provisioning API device");
        device.setStatus("ACTIVE");
        device.setConnectionStatus("OFFLINE");
        deviceRepository.save(device);

        String adminToken = saveToken(admin, "integration-admin-token");
        String ownerToken = saveToken(owner, "integration-owner-token");
        String otherToken = saveToken(otherOwner, "integration-other-token");

        HttpResponse<String> issued = post(
                "/api/device-provisioning/devices/device_api_test/claim-code",
                adminToken,
                "{\"expiresInMinutes\":60}"
        );
        assertEquals(201, issued.statusCode());
        String claimCode = json.readTree(issued.body()).path("data").path("claimCode").asText();
        assertEquals(43, claimCode.length());
        assertFalse(issued.body().contains("claim_code_hash"));
        var storedBeforeClaim = provisioningRepository.findByDeviceDeviceId("device_api_test").orElseThrow();
        assertEquals(64, storedBeforeClaim.getClaimCodeHash().length());
        assertNotEquals(claimCode, storedBeforeClaim.getClaimCodeHash());

        String wrongClaim = claimJson("WRONG-CLAIM-CODE-0000000000", pond.getId());
        HttpResponse<String> invalid = post("/api/device-provisioning/claims", ownerToken, wrongClaim);
        assertError(invalid, 422, "INVALID_CLAIM_CODE");

        String validClaim = claimJson(claimCode, pond.getId());
        HttpResponse<String> claimed = post("/api/device-provisioning/claims", ownerToken, validClaim);
        assertEquals(201, claimed.statusCode());
        assertEquals("CLAIMED", json.readTree(claimed.body()).path("data").path("claimStatus").asText());
        assertNull(provisioningRepository.findByDeviceDeviceId("device_api_test").orElseThrow().getClaimCodeHash());

        HttpResponse<String> retried = post("/api/device-provisioning/claims", ownerToken, validClaim);
        assertEquals(201, retried.statusCode());

        HttpResponse<String> status = get(
                "/api/device-provisioning/devices/device_api_test/status",
                ownerToken
        );
        assertEquals(200, status.statusCode());
        JsonNode statusData = json.readTree(status.body()).path("data");
        assertEquals("CLAIMED", statusData.path("claimStatus").asText());
        assertEquals("OFFLINE", statusData.path("connectionStatus").asText());

        HttpResponse<String> conflict = post("/api/device-provisioning/claims", otherToken, validClaim);
        assertError(conflict, 409, "DEVICE_ALREADY_CLAIMED");

        HttpResponse<String> unauthorized = get(
                "/api/device-provisioning/devices/device_api_test/status",
                null
        );
        assertError(unauthorized, 401, "UNAUTHORIZED");
    }

    private UserAccount saveUser(String username, RoleName role) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setFullName(username);
        user.setPasswordHash("not-used-by-this-test");
        user.setRole(role);
        user.setActive(true);
        user.setApprovalStatus(ApprovalStatus.APPROVED);
        return userRepository.save(user);
    }

    private void grantOwner(UserAccount user, Pond pond) {
        UserPondAccess access = new UserPondAccess();
        access.setUser(user);
        access.setPond(pond);
        access.setAccessType("OWNER");
        accessRepository.save(access);
    }

    private String saveToken(UserAccount user, String tokenValue) {
        AuthToken token = new AuthToken();
        token.setUser(user);
        token.setToken(tokenValue);
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        tokenRepository.save(token);
        return tokenValue;
    }

    private String claimJson(String claimCode, Long pondId) throws Exception {
        return json.createObjectNode()
                .put("version", 1)
                .put("deviceId", "device_api_test")
                .put("claimCode", claimCode)
                .put("pondId", pondId)
                .toString();
    }

    private HttpResponse<String> post(String path, String token, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (token != null) request.header("Authorization", "Bearer " + token);
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
        if (token != null) request.header("Authorization", "Bearer " + token);
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private void assertError(HttpResponse<String> response, int status, String code) throws Exception {
        assertEquals(status, response.statusCode());
        JsonNode body = json.readTree(response.body());
        assertTrue(!body.path("success").asBoolean());
        assertEquals(code, body.path("data").path("code").asText());
    }
}
