package com.example.chitchat.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RequestTrackingFilter extends OncePerRequestFilter {

    private final AtomicInteger activeRequests = new AtomicInteger();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        activeRequests.incrementAndGet();
        try {
            filterChain.doFilter(request, response);
        } finally {
            activeRequests.decrementAndGet();
        }
    }

    public int getActiveRequests() {
        return activeRequests.get();
    }
}