package com.hms.HospitalManagementSystem;

import com.hms.HospitalManagementSystem.service.JwtService;
import com.hms.HospitalManagementSystem.service.impl.JwtServiceImpl;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class ManualJwtTest {

    @Test
    public void testJwtGenerationAndValidation() {
        System.out.println("--- START MANUAL JWT TEST ---");

        JwtService jwtService = new JwtServiceImpl();
        // Inject values manually since we aren't using Spring Context
        // Secret must be Base64 encoded.
        // "ThisIsASecretKeyThatIsLongEnoughForHS256Algorithm" -> Base64
        String base64Secret = "VGhpc0lzQVNlY3JldEtleVGhhdElzTG9uZ0Vub3VnaEZvckhTMjU2QWxnb3JpdGht";
        ReflectionTestUtils.setField(jwtService, "secretKey", base64Secret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1800000L); // 30 mins
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L); // 7 days

        UserDetails user = new User("admin", "password", new ArrayList<>());

        String token = jwtService.generateToken(user);
        System.out.println("Generated Token: " + token);

        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("target/token.txt"), token);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token, user));

        String username = jwtService.extractUsername(token);
        assertEquals("admin", username);

        System.out.println("--- END MANUAL JWT TEST ---");
    }
}
