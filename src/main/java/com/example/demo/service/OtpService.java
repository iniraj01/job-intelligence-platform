package com.example.demo.service;

import com.example.demo.model.OtpData;
import com.example.demo.repository.OtpDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpService {

    @Autowired
    private OtpDataRepository otpDataRepository;

    @Autowired
    private EmailService emailService;

    @Value("${otp.expiration:300000}")
    private long otpExpirationTime;

    @Value("${otp.max-attempts:5}")
    private int maxAttempts;

    private static final Random random = new Random();

    /**
     * Generate and send OTP to email
     */
    public boolean generateAndSendOtp(String email, String password) {
        try {
            // Generate 6-digit OTP
            String otp = String.format("%06d", random.nextInt(1000000));

            // Set expiration time (default 5 minutes)
            LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(otpExpirationTime / 1000);

            // Delete existing OTP for this email if present
            otpDataRepository.deleteByEmail(email);

            // Save new OTP
            OtpData otpData = new OtpData(email, otp, expiryTime);
            otpDataRepository.save(otpData);

            // Send email
            emailService.sendOtpEmail(email, otp, password);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verify OTP
     */
    public boolean verifyOtp(String email, String otp) {
        Optional<OtpData> otpDataOptional = otpDataRepository.findByEmail(email);

        if (!otpDataOptional.isPresent()) {
            return false;
        }

        OtpData otpData = otpDataOptional.get();

        // Check if OTP has expired
        if (LocalDateTime.now().isAfter(otpData.getExpiry())) {
            otpDataRepository.delete(otpData);
            return false;
        }

        // Check if max attempts exceeded
        if (otpData.getAttempts() >= maxAttempts) {
            otpDataRepository.delete(otpData);
            return false;
        }

        // Increment attempts
        otpData.incrementAttempts();

        // Check if OTP matches
        if (otpData.getOtp().equals(otp)) {
            otpData.setVerified(true);
            otpDataRepository.save(otpData);
            return true;
        }

        otpDataRepository.save(otpData);
        return false;
    }

    /**
     * Check if OTP is verified
     */
    public boolean isOtpVerified(String email) {
        Optional<OtpData> otpDataOptional = otpDataRepository.findByEmail(email);
        return otpDataOptional.isPresent() && otpDataOptional.get().isVerified();
    }

    /**
     * Delete OTP record
     */
    public void deleteOtp(String email) {
        otpDataRepository.deleteByEmail(email);
    }

    /**
     * Get remaining attempts
     */
    public int getRemainingAttempts(String email) {
        Optional<OtpData> otpDataOptional = otpDataRepository.findByEmail(email);
        if (otpDataOptional.isPresent()) {
            return maxAttempts - otpDataOptional.get().getAttempts();
        }
        return maxAttempts;
    }
}
