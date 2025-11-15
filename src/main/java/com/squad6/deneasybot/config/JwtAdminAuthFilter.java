package com.squad6.deneasybot.config;

import com.squad6.deneasybot.model.SuperAdmin;
import com.squad6.deneasybot.repository.SuperAdminRepository;
import com.squad6.deneasybot.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAdminAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SuperAdminRepository superAdminRepository;

    public JwtAdminAuthFilter(JwtUtil jwtUtil, SuperAdminRepository superAdminRepository) {
        this.jwtUtil = jwtUtil;
        this.superAdminRepository = superAdminRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String adminEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            adminEmail = jwtUtil.extractEmail(jwt);
        } catch (Exception e) {
            logger.warn("Tentativa de acesso com token JWT inválido");
            filterChain.doFilter(request, response);
            return;
        }

        if (adminEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            SuperAdmin admin = this.superAdminRepository.findByEmail(adminEmail).orElse(null);

            if (admin != null && jwtUtil.isTokenValid(jwt)) {
                UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                        admin.getEmail(),
                        "",
                        new ArrayList<>()
                );
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}