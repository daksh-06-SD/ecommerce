package com.example.ecommerce.controller;

import com.example.ecommerce.JwtService;
import com.example.ecommerce.model.LoginResponse;
import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public UserController(
            UserRepository userRepository,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }


    // =========================
    // REGISTER USER
    // =========================

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @RequestBody User user) {

        if (user.getName() == null ||
                user.getName().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("Name is required");
        }

        if (user.getEmail() == null ||
                user.getEmail().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("Email is required");
        }

        if (user.getPassword() == null ||
                user.getPassword().length() < 6) {

            return ResponseEntity.badRequest()
                    .body("Password must be at least 6 characters");
        }

        if (userRepository
                .findByEmail(user.getEmail())
                .isPresent()) {

            return ResponseEntity.badRequest()
                    .body("Email already registered");
        }

        // Every new registration is a normal USER
        user.setRole("USER");

        // Encrypt password
        user.setPassword(
                passwordEncoder.encode(
                        user.getPassword()
                )
        );

        User savedUser =
                userRepository.save(user);

        return ResponseEntity.ok(savedUser);
    }


    // =========================
    // USER LOGIN
    // =========================

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(
            @RequestBody User user) {

        if (user.getEmail() == null ||
                user.getEmail().isBlank() ||
                user.getPassword() == null ||
                user.getPassword().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("Email and password are required");
        }

        User existingUser =
                userRepository
                        .findByEmail(user.getEmail())
                        .orElse(null);

        if (existingUser == null) {

            return ResponseEntity.badRequest()
                    .body("Invalid email or password");
        }

        if (!passwordEncoder.matches(
                user.getPassword(),
                existingUser.getPassword())) {

            return ResponseEntity.badRequest()
                    .body("Invalid email or password");
        }

        String token =
                jwtService.generateToken(
                        existingUser.getId(),
                        existingUser.getEmail(),
                        existingUser.getRole()
                );

        return ResponseEntity.ok(
                new LoginResponse(
                        token,
                        existingUser.getId(),
                        existingUser.getName(),
                        existingUser.getEmail(),
                        existingUser.getRole()
                )
        );
    }


    // =========================
    // ADMIN LOGIN
    // =========================

    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(
            @RequestBody User user) {

        if (user.getEmail() == null ||
                user.getEmail().isBlank() ||
                user.getPassword() == null ||
                user.getPassword().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("Email and password are required");
        }

        User existingUser =
                userRepository
                        .findByEmail(user.getEmail())
                        .orElse(null);

        if (existingUser == null) {

            return ResponseEntity.badRequest()
                    .body("Admin not found");
        }

        if (!"ADMIN".equals(existingUser.getRole())) {

            return ResponseEntity.badRequest()
                    .body("Access denied: Not an admin");
        }

        if (!passwordEncoder.matches(
                user.getPassword(),
                existingUser.getPassword())) {

            return ResponseEntity.badRequest()
                    .body("Invalid password");
        }

        String token =
                jwtService.generateToken(
                        existingUser.getId(),
                        existingUser.getEmail(),
                        existingUser.getRole()
                );

        return ResponseEntity.ok(
                new LoginResponse(
                        token,
                        existingUser.getId(),
                        existingUser.getName(),
                        existingUser.getEmail(),
                        existingUser.getRole()
                )
        );
    }


    // =========================
    // GET ALL CUSTOMERS
    // =========================

    @GetMapping("/customers")
    public List<User> getAllCustomers() {

        return userRepository.findByRole("USER");
    }
}