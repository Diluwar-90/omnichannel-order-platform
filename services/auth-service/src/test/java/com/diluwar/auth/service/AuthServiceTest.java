package com.diluwar.auth.service;

import com.diluwar.auth.dto.LoginRequest;
import com.diluwar.auth.dto.LoginResponse;
import com.diluwar.auth.dto.RegisterRequest;
import com.diluwar.auth.dto.RegisterResponse;
import com.diluwar.auth.entity.User;
import com.diluwar.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldSuccessfullyRegisterUser() {
        // Arrange
        RegisterRequest request = new RegisterRequest("Diluwar", "diluwar@example.com", "secret123");

        when(userRepository.existsByEmail("diluwar@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encodedSecret");

        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setName("Diluwar");
        savedUser.setEmail("diluwar@example.com");
        savedUser.setPassword("encodedSecret");
        savedUser.setRole("USER");
        savedUser.setEnabled(true);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        RegisterResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Diluwar", response.getName());
        assertEquals("diluwar@example.com", response.getEmail());
        assertEquals("USER", response.getRole());

        verify(userRepository).existsByEmail("diluwar@example.com");
        verify(passwordEncoder).encode("secret123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowException_whenEmailAlreadyExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest("Existing", "exists@example.com", "pass");
        when(userRepository.existsByEmail("exists@example.com")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );

        assertEquals("Email is already registered", exception.getMessage());
        verify(userRepository).existsByEmail("exists@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldSuccessfullyAuthenticateAndReturnToken() {
        // Arrange
        LoginRequest request = new LoginRequest("user@example.com", "password123");

        User user = new User();
        user.setId(5L);
        user.setName("Test User");
        user.setEmail("user@example.com");
        user.setPassword("encodedHash");
        user.setRole("USER");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedHash")).thenReturn(true);
        when(jwtService.generateToken("user@example.com", "USER")).thenReturn("mock-jwt-token");

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("USER", response.getRole());
        assertEquals("mock-jwt-token", response.getToken());

        verify(userRepository).findByEmail("user@example.com");
        verify(passwordEncoder).matches("password123", "encodedHash");
        verify(jwtService).generateToken("user@example.com", "USER");
    }

    @Test
    void login_shouldThrowException_whenUserNotFound() {
        // Arrange
        LoginRequest request = new LoginRequest("unknown@example.com", "pass");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());
        verify(userRepository).findByEmail("unknown@example.com");
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_shouldThrowException_whenPasswordDoesNotMatch() {
        // Arrange
        LoginRequest request = new LoginRequest("user@example.com", "wrongPassword");

        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword("encodedHash");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedHash")).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());
        verify(userRepository).findByEmail("user@example.com");
        verify(passwordEncoder).matches("wrongPassword", "encodedHash");
        verifyNoInteractions(jwtService);
    }
}
