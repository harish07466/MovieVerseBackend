package com.MovieVerseBackEnd.ForgotPassword;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/movie/auth")
public class ForgotPasswordController {
	@Autowired
    private OTPService otpService;

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String message = otpService.sendOTP(email);

        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");

        String message = otpService.verifyAndResetPassword(email, otp, newPassword);

        Map<String, String> response = new HashMap<>();
        response.put("message", message);

        // If OTP verification failed
        if (message.toLowerCase().contains("invalid") || message.toLowerCase().contains("failed")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Otherwise success
        return ResponseEntity.ok(response);
    }
}
