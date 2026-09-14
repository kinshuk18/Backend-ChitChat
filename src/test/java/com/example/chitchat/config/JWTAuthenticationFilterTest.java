package com.example.chitchat.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JWTAuthenticationFilterTest {

    private final JWTAuthenticationFilter filter = new JWTAuthenticationFilter(null);

    @Test
    void shouldSkipPublicLoadTestRoutesWhenUriStartsWithPublicPath() {
        HttpServletRequest feedRequest = new MockHttpServletRequest("GET", "/feed/partition-1");
        HttpServletRequest messageRequest = new MockHttpServletRequest("POST", "/message/123");
        HttpServletRequest healthRequest = new MockHttpServletRequest("GET", "/healthz");

        assertTrue(filter.shouldNotFilter(feedRequest));
        assertTrue(filter.shouldNotFilter(messageRequest));
        assertTrue(filter.shouldNotFilter(healthRequest));
    }

    @Test
    void shouldNotSkipProtectedRoutes() {
        HttpServletRequest securedRequest = new MockHttpServletRequest("GET", "/api/rooms");

        assertFalse(filter.shouldNotFilter(securedRequest));
    }
}
