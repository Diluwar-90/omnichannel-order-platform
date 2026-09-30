package com.diluwar.auth.controller;

import com.diluwar.auth.dto.LoginRequest;
import com.diluwar.auth.dto.LoginResponse;
import com.diluwar.auth.dto.RegisterRequest;
import com.diluwar.auth.dto.RegisterResponse;
import com.diluwar.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private com.diluwar.auth.service.JwtService jwtService;

    @Test
    void register_shouldReturn201_whenValid() throws Exception {
        RegisterResponse response = new RegisterResponse(1L, "Diluwar", "diluwar@example.com", "USER");
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Diluwar",
                                          "email": "diluwar@example.com",
                                          "password": "password123"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Diluwar"))
                .andExpect(jsonPath("$.email").value("diluwar@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_shouldReturn400_whenPayloadInvalid() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "",
                                          "email": "not-an-email",
                                          "password": ""
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_shouldReturn200_whenValidCredentials() throws Exception {
        LoginResponse response = new LoginResponse(1L, "Diluwar", "diluwar@example.com", "USER", "mock-jwt-token");
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "diluwar@example.com",
                                          "password": "password123"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("diluwar@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.token").value("mock-jwt-token"));
    }

    @Test
    void login_shouldReturn400_whenEmailInvalid() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "invalid-email",
                                          "password": "pass"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void me_shouldReturn200_whenPrincipalProvided() throws Exception {
        UsernamePasswordAuthenticationToken principal =
                new UsernamePasswordAuthenticationToken("diluwar@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(
                        get("/api/v1/auth/me")
                                .principal(principal)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("Authenticated as: diluwar@example.com"));
    }

    @Test
    void admin_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string("Admin access granted"));
    }
}
