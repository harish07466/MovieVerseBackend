package com.MovieVerseBackEnd.Controllers;

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

import com.MovieVerseBackEnd.Entites.User;
import com.MovieVerseBackEnd.Services.AuthenticationService;
import com.MovieVerseBackEnd.Services.LoginRequest;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@CrossOrigin(
	    origins = "http://localhost:5173",
	    allowedHeaders = "*",
	    allowCredentials = "true"
	)
@RequestMapping("/api/movie/auth")
public class AuthenticationController {
	@Autowired
	private final AuthenticationService authenticationService ;

	public AuthenticationController(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}
	
	
	  @PostMapping("/login")
	    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
	        try {
	            User user = authenticationService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());
	            String token = authenticationService.generateToken(user);

	            Cookie cookie = new Cookie("authenticatedToken", token);
	            cookie.setHttpOnly(true);
	            cookie.setSecure(false); // Set to true if using HTTPS
	            cookie.setPath("/");
	            cookie.setMaxAge(3600); // 1 hour
	            cookie.setDomain("localhost");
	            response.addCookie(cookie);
	           // Optional but useful
	            
	            response.addHeader("Set-Cookie",
	                    String.format("authToken=%s; HttpOnly; Path=/; Max-Age=3600; SameSite=None", token));

	            
	            Map<String, Object> responseBody = new HashMap<>();
	            responseBody.put("message", "Login successful");
	            responseBody.put("username", user.getUsername());

	            return ResponseEntity.ok(responseBody);
	            
	        } 
	        catch (RuntimeException e) 
	        {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
	        }
	    }
	
	  @PostMapping("/logout")
	    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {
	        try {
	            String token = null;
	            Cookie[] cookies = request.getCookies();
	            if (cookies != null) {
	                for (Cookie cookie : cookies) {
	                    if ("authenticatedToken".equals(cookie.getName())) {
	                        token = cookie.getValue();
	                        break;
	                    }
	                }
	            }

	            if (token != null) {
	                // Delete token directly using the token string
	            	authenticationService.invalidateToken(token);
	            }

	            // Clear cookie
	            Cookie cookie = new Cookie("authToken", null);
	            cookie.setHttpOnly(true);
	            cookie.setMaxAge(0);
	            cookie.setPath("/");
	            response.addCookie(cookie);

	            return ResponseEntity.ok(Map.of("message", "Logout Successful!."));
	        } catch (Exception e) {
	            return ResponseEntity.status(500).body(Map.of("message", "Logout failed: " + e.getMessage()));
	        }
	    }
	

}
