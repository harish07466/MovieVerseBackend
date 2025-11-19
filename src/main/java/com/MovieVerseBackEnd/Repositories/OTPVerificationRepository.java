package com.MovieVerseBackEnd.Repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.MovieVerseBackEnd.Entites.OTPVerification;


public interface OTPVerificationRepository extends JpaRepository<OTPVerification, Long> {
    Optional<OTPVerification> findByEmail(String email);
}
