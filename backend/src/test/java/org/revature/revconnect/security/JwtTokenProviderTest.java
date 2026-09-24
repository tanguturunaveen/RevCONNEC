package org.revature.revconnect.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        String raw32Bytes = "01234567890123456789012345678901";
        String secret = Base64.getEncoder().encodeToString(raw32Bytes.getBytes());
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", 60_000L);
        userDetails = User.withUsername("john").password("x").authorities("ROLE_PERSONAL").build();
    }

    @Test
    void generateToken_andExtractUsername_success() {
        String token = jwtTokenProvider.generateToken(userDetails);

        String username = jwtTokenProvider.extractUsername(token);
        assertEquals("john", username);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void generateToken_withExtraClaims_isValid() {
        String token = jwtTokenProvider.generateToken(Map.of("scope", "api"), userDetails);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertTrue(jwtTokenProvider.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValid_withDifferentUser_returnsFalse() {
        String token = jwtTokenProvider.generateToken(userDetails);
        UserDetails another = User.withUsername("mike").password("x").authorities("ROLE_PERSONAL").build();

        assertFalse(jwtTokenProvider.isTokenValid(token, another));
    }

    @Test
    void validateToken_withMalformedToken_returnsFalse() {
        assertFalse(jwtTokenProvider.validateToken("not-a-jwt"));
    }
}
