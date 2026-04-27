package com.example.demo.service;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.SignupRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.validator.routines.EmailValidator;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthenticationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private JwtTokenService jwtTokenService;

    /**
     * Register a new user with CAPTCHA
     */
    public AuthResponse registerUser(SignupRequest request) {
        // Validate input
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return new AuthResponse(false, "Name is required");
        }

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return new AuthResponse(false, "Email is required");
        }

        if (!isValidEmail(request.getEmail())) {
            return new AuthResponse(false, "Invalid email format");
        }

        if (request.getPassword() == null || request.getPassword().length() < 8) {
            return new AuthResponse(false, "Password must be at least 8 characters long");
        }

        if (request.getCaptchaToken() == null || request.getCaptchaToken().isEmpty()) {
            return new AuthResponse(false, "CAPTCHA validation required");
        }

        // Verify CAPTCHA
        if (!captchaService.verify(request.getCaptchaToken())) {
            return new AuthResponse(false, "CAPTCHA verification failed");
        }

        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return new AuthResponse(false, "Email already registered");
        }

        // Create new user
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(hashPassword(request.getPassword()));
        user.setEmailVerified(false);
        user.setAccountEnabled(true);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        // Generate and send OTP
        if (otpService.generateAndSendOtp(request.getEmail(), request.getPassword(), request.getName())) {
            return new AuthResponse(true,
                    "User registered successfully. Please verify your email with OTP sent to " + request.getEmail());
        } else {
            return new AuthResponse(false, "User registered but failed to send OTP. Please try again.");
        }
    }

    /**
     * Login user with CAPTCHA
     */
    public AuthResponse loginUser(LoginRequest request) {
        try {
            // Validate input
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                return new AuthResponse(false, "Email is required");
            }

            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                return new AuthResponse(false, "Password is required");
            }

            if (request.getCaptchaToken() == null || request.getCaptchaToken().isEmpty()) {
                return new AuthResponse(false, "CAPTCHA validation required");
            }

            // Verify CAPTCHA
            if (!captchaService.verify(request.getCaptchaToken())) {
                return new AuthResponse(false, "CAPTCHA verification failed");
            }

            // Find user by email
            java.util.Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
            if (!userOptional.isPresent()) {
                return new AuthResponse(false, "Invalid email or password");
            }

            User user = userOptional.get();

            // Check if account is enabled
            if (!user.isAccountEnabled()) {
                return new AuthResponse(false, "Account is disabled");
            }

            // Verify password
            if (!verifyPassword(request.getPassword(), user.getPassword())) {
                return new AuthResponse(false, "Invalid email or password");
            }

            // Check if email is verified
            if (!user.isEmailVerified()) {
                return new AuthResponse(false, "Please verify your email first");
            }

            // Generate JWT token
            String token = jwtTokenService.generateToken(user.getEmail());
            UserResponse userResponse = new UserResponse(user);

            return new AuthResponse(true, "Login successful", token, userResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return new AuthResponse(false, "SERVER ERROR DETAIL: " + e.getMessage() + " | Class: " + e.getClass().getSimpleName());
        }
    }

    /**
     * Verify OTP and mark email as verified
     */
    public AuthResponse verifyOtpAndConfirmEmail(String email, String otp) {
        if (!otpService.verifyOtp(email, otp)) {
            int remainingAttempts = otpService.getRemainingAttempts(email);
            if (remainingAttempts <= 0) {
                return new AuthResponse(false, "Maximum OTP verification attempts exceeded. Please request a new OTP.");
            }
            return new AuthResponse(false, "Invalid OTP. Remaining attempts: " + remainingAttempts);
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (!userOptional.isPresent()) {
            return new AuthResponse(false, "User not found");
        }

        User user = userOptional.get();
        user.setEmailVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Delete OTP record
        otpService.deleteOtp(email);

        return new AuthResponse(true, "Email verified successfully. You can now login.");
    }

    /**
     * Hash password using BCrypt
     */
    public String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * Verify password against hash
     */
    public boolean verifyPassword(String password, String hash) {
        try {
            return BCrypt.checkpw(password, hash);
        } catch (IllegalArgumentException e) {
            // This happens if the stored password is not a valid BCrypt hash (e.g. old plain text passwords)
            System.err.println("Invalid BCrypt hash found for password verification: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        return EmailValidator.getInstance().isValid(email);
    }

    /**
     * Get user by email
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Change password
     */
    public AuthResponse changePassword(String email, String oldPassword, String newPassword) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (!userOptional.isPresent()) {
            return new AuthResponse(false, "User not found");
        }

        User user = userOptional.get();

        if (!verifyPassword(oldPassword, user.getPassword())) {
            return new AuthResponse(false, "Current password is incorrect");
        }

        if (newPassword == null || newPassword.length() < 8) {
            return new AuthResponse(false, "New password must be at least 8 characters long");
        }

        user.setPassword(hashPassword(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return new AuthResponse(true, "Password changed successfully");
    }
}
