package com.diluwar.auth.integration;

import com.diluwar.auth.dto.LoginResponse;
import com.diluwar.auth.dto.RegisterResponse;
import com.diluwar.auth.entity.User;
import com.diluwar.auth.repository.UserRepository;
import com.diluwar.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        String payload = """
                {
                    "name": "Integration User",
                    "email": "integration@example.com",
                    "password": "Password123"
                }
                """;

        RegisterResponse response = restClient()
                .post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(RegisterResponse.class);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("Integration User", response.getName());
        assertEquals("integration@example.com", response.getEmail());
        assertEquals("USER", response.getRole());

        assertTrue(userRepository.existsByEmail("integration@example.com"));
    }

    @Test
    void shouldLoginAndReceiveJwtToken() {
        // Register user directly
        User user = new User();
        user.setName("Login Test");
        user.setEmail("login@example.com");
        user.setPassword(passwordEncoder.encode("secretPassword"));
        user.setRole("USER");
        user.setEnabled(true);
        userRepository.save(user);

        String loginPayload = """
                {
                    "email": "login@example.com",
                    "password": "secretPassword"
                }
                """;

        LoginResponse loginResponse = restClient()
                .post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginPayload)
                .retrieve()
                .body(LoginResponse.class);

        assertNotNull(loginResponse);
        assertEquals("login@example.com", loginResponse.getEmail());
        assertEquals("USER", loginResponse.getRole());
        assertNotNull(loginResponse.getToken());
        assertFalse(loginResponse.getToken().isBlank());
    }

    @Test
    void shouldAccessMeEndpointWithValidToken() {
        // Create user
        User user = new User();
        user.setName("Token User");
        user.setEmail("token@example.com");
        user.setPassword(passwordEncoder.encode("secret"));
        user.setRole("USER");
        user.setEnabled(true);
        userRepository.save(user);

        String token = jwtService.generateToken("token@example.com", "USER");

        String response = restClient()
                .get()
                .uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(String.class);

        assertEquals("Authenticated as: token@example.com", response);
    }

    @Test
    void shouldReturn401WhenAccessingAdminWithoutToken() {
        HttpClientErrorException.Unauthorized ex = assertThrows(
                HttpClientErrorException.Unauthorized.class,
                () -> restClient()
                        .get()
                        .uri("/api/v1/auth/admin")
                        .retrieve()
                        .body(String.class)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void shouldReturn403WhenUserRoleAccessesAdminEndpoint() {
        String userToken = jwtService.generateToken("user@example.com", "USER");

        HttpClientErrorException.Forbidden ex = assertThrows(
                HttpClientErrorException.Forbidden.class,
                () -> restClient()
                        .get()
                        .uri("/api/v1/auth/admin")
                        .header("Authorization", "Bearer " + userToken)
                        .retrieve()
                        .body(String.class)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getResponseBodyAsString().contains("Access Denied"));
    }

    @Test
    void shouldReturn200WhenAdminRoleAccessesAdminEndpoint() {
        String adminToken = jwtService.generateToken("admin@example.com", "ADMIN");

        String response = restClient()
                .get()
                .uri("/api/v1/auth/admin")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .body(String.class);

        assertEquals("Admin access granted", response);
    }

    @Test
    void shouldAllowAccessToHealthEndpointWithoutToken() {
        ResponseEntity<String> response = restClient()
                .get()
                .uri("/api/v1/health")
                .retrieve()
                .toEntity(String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Auth Service is running", response.getBody());
    }
}
