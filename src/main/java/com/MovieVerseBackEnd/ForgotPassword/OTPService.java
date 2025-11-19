package com.MovieVerseBackEnd.ForgotPassword;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.MovieVerseBackEnd.Entites.OTPVerification;
import com.MovieVerseBackEnd.Entites.User;
import com.MovieVerseBackEnd.Repositories.OTPVerificationRepository;
import com.MovieVerseBackEnd.Repositories.UserRepository;

@Service
public class OTPService {

    @Autowired
    private OTPVerificationRepository otpRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Generate OTP and send to email
    public String sendOTP(String email) {
        Optional<User> userOpt = userRepo.findByEmail(email);
        if (userOpt.isEmpty()) {
            return "No account found with this email.";
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Expiry after 5 minutes
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5);

        OTPVerification otpRecord = otpRepo.findByEmail(email)
                .map(existing -> {
                    existing.setOtp(otp);
                    existing.setExpiryTime(expiryTime);
                    return existing;
                })
                .orElse(new OTPVerification(email, otp, expiryTime));

        otpRepo.save(otpRecord);

        // Send email
        sendEmail(email, otp);

        return "OTP Sent Successfully to Your Registered Email!";
    }

    private void sendEmail(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("MovieVerse - Password Reset OTP");
        message.setText("Your OTP for Password Reset is: " + otp + "\nThis OTP is valid for 5 minutes.");
        mailSender.send(message);
    }

    // Validate OTP and reset password
    public String verifyAndResetPassword(String email, String otp, String newPassword) {
        Optional<OTPVerification> otpRecordOpt = otpRepo.findByEmail(email);

        if (otpRecordOpt.isEmpty()) {
            return "OTP not found for this email.";
        }

        OTPVerification otpRecord = otpRecordOpt.get();

        if (!otpRecord.getOtp().equals(otp)) {
            return "Invalid OTP!";
        }

        if (otpRecord.getExpiryTime().isBefore(LocalDateTime.now())) {
            return "OTP expired! Please request a new one.";
        }

        // Reset password
        Optional<User> userOpt = userRepo.findByEmail(email);
        if (userOpt.isEmpty()) {
            return "User not found.";
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        otpRepo.delete(otpRecord); // Delete OTP after use

        return "Password reset successfully!";
    }
}
