package org.revature.revconnect.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationEntryPointTest {

    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint();

    @Test
    void commence_setsUnauthorizedStatusAndMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException ex = new AuthenticationException("Invalid token") {};

        entryPoint.commence(request, response, ex);

        assertEquals(401, response.getStatus());
        assertTrue(response.getErrorMessage().contains("Unauthorized"));
        assertTrue(response.getErrorMessage().contains("Invalid token"));
    }
}
