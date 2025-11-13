package com.squad6.deneasybot.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private Key secretKey;

    private static final long VERIFICATION_TOKEN_EXPIRATION  = 15 * 60 * 1000;
    private static final long SESSION_TOKEN_EXPIRATION = 24 * 60 * 60 * 1000;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateVerificationToken(String email, String code) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + VERIFICATION_TOKEN_EXPIRATION);

        return Jwts.builder()
                .setSubject(email)
                .claim("code", code)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateSessionToken() {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + SESSION_TOKEN_EXPIRATION);

        return Jwts.builder()
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateSessionToken(String subject) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + SESSION_TOKEN_EXPIRATION);

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractVerificationCode(String token) {
        return parseClaims(token).get("code", String.class);
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}
