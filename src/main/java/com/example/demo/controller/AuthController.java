package com.example.demo.controller;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.OtpVerificationRequest;
import com.example.demo.dto.SignupRequest;
import com.example.demo.service.AuthenticationService;
import com.example.demo.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private OtpService otpService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
        AuthResponse response = authenticationService.registerUser(request);
        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authenticationService.loginUser(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestBody OtpVerificationRequest request) {
        if (request.getEmail() == null || request.getOtp() == null) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(false, "Email and OTP are required"));
        }

        AuthResponse response = authenticationService.verifyOtpAndConfirmEmail(request.getEmail(), request.getOtp());
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<AuthResponse> resendOtp(@RequestParam String email) {
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(false, "Email is required"));
        }

        java.util.Optional<com.example.demo.model.User> userOpt = authenticationService.getUserByEmail(email);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AuthResponse(false, "User not found"));
        }

        if (otpService.generateAndSendOtp(email, null, userOpt.get().getName())) {
            return ResponseEntity.ok(new AuthResponse(true, "OTP sent successfully to " + email));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse(false, "Failed to send OTP"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<AuthResponse> changePassword(
            @RequestParam String email,
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            HttpServletRequest request) {

        // Check if user is authenticated
        String userEmail = (String) request.getAttribute("userEmail");
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(false, "Unauthorized. Please login first."));
        }

        // Verify user is changing their own password
        if (!userEmail.equals(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AuthResponse(false, "You can only change your own password"));
        }

        AuthResponse response = authenticationService.changePassword(email, oldPassword, newPassword);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Authentication service is running");
    }

    @GetMapping("/protected/profile")
    public ResponseEntity<String> getProtectedResource(HttpServletRequest request) {
        String userEmail = (String) request.getAttribute("userEmail");
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Unauthorized access");
        }
        return ResponseEntity.ok("Welcome " + userEmail + "! This is a protected resource.");
    }
}
