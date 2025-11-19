package com.MovieVerseBackEnd.Entites;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="otpverification")
public class OTPVerification {
	 	@Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    @Column(name="id")
	    private Long id;
	    
	    @Column(name="email")
	    private String email;
	    
	    @Column(name="otp")
	    private String otp;
	    
	    @Column(name="expiry_time")
	    private LocalDateTime expiryTime;

	    public OTPVerification() {}

	    public OTPVerification(String email, String otp, LocalDateTime expiryTime) {
	        this.email = email;
	        this.otp = otp;
	        this.expiryTime = expiryTime;
	    }
	    
	    // Getters and Setters

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getOtp() {
			return otp;
		}

		public void setOtp(String otp) {
			this.otp = otp;
		}

		public LocalDateTime getExpiryTime() {
			return expiryTime;
		}

		public void setExpiryTime(LocalDateTime expiryTime) {
			this.expiryTime = expiryTime;
		}

}
