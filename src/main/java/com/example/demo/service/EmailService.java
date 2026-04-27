package com.example.demo.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp, String password, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Verify Your Account - Stackseed Job Intelligence");
            helper.setFrom("iniraz054@gmail.com");

            String htmlMsg = "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; background-color: #f9fafb; padding: 30px; border-radius: 10px; border: 1px solid #e5e7eb;\">"
                    + "<div style=\"text-align: center; margin-bottom: 30px;\">"
                    + "<h1 style=\"color: #1f2937; margin: 0; font-size: 24px;\">Stackseed Job Intelligence</h1>"
                    + "<p style=\"color: #6b7280; font-size: 16px; margin-top: 5px;\">Secure Account Verification</p>"
                    + "</div>"
                    + "<div style=\"background-color: #ffffff; padding: 30px; border-radius: 8px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);\">"
                    + "<h2 style=\"color: #111827; margin-top: 0;\">Hello " + (name != null ? name : "") + "!</h2>"
                    + "<p style=\"color: #4b5563; font-size: 16px; line-height: 1.5;\">Thank you for registering with Stackseed Job Intelligence. To complete your signup process, please use the verification code below:</p>"
                    + "<div style=\"text-align: center; margin: 30px 0;\">"
                    + "<span style=\"display: inline-block; background-color: #f3f4f6; color: #3b82f6; font-size: 32px; font-weight: bold; letter-spacing: 5px; padding: 15px 30px; border-radius: 8px; border: 2px dashed #93c5fd;\">"
                    + otp + "</span>"
                    + "</div>"
                    + "<div style=\"margin-top: 25px; padding: 15px; background-color: #fdf8f6; border-left: 4px solid #f97316; border-radius: 4px;\">"
                    + "<h3 style=\"color: #9a3412; margin-top: 0; font-size: 16px;\">Your Account Details</h3>"
                    + "<p style=\"color: #4b5563; margin-bottom: 5px;\"><strong>Email ID:</strong> " + toEmail + "</p>"
                    + "<p style=\"color: #4b5563; margin-top: 0;\"><strong>Password:</strong> " + password + "</p>"
                    + "</div>"
                    + "<p style=\"color: #4b5563; font-size: 14px; margin-top: 20px;\">This code will expire in <strong>5 minutes</strong>. If you did not request this code, please ignore this email.</p>"
                    + "</div>"
                    + "<div style=\"text-align: center; margin-top: 20px; color: #9ca3af; font-size: 12px;\">"
                    + "<p>&copy; 2026 Stackseed Job Intelligence. All rights reserved.</p>"
                    + "</div>"
                    + "</div>";

            helper.setText(htmlMsg, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send HTML OTP email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
