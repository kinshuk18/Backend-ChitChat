package com.example.chitchat.config;

import com.example.chitchat.service.JWTService;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;

@Component
public class JWTAuthenticationFilter extends OncePerRequestFilter {
    private final JWTService jwtService;

    public JWTAuthenticationFilter( JWTService jwtService ){
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if( authHeader==null || !authHeader.startsWith("Bearer ") ){
            filterChain.doFilter(request, response);
            return;
        }
        String token = authHeader.substring(7);
        try{
            if(jwtService.validateAccessToken(token)){
                String username = jwtService.getUsernameFromToken(token);
                UsernamePasswordAuthenticationToken authentication = new
                        UsernamePasswordAuthenticationToken(username,null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            else{
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        catch(ParseException | JOSEException j ){
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        filterChain.doFilter(request, response);
    }




}
