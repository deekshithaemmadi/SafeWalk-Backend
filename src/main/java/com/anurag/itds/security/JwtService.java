package com.anurag.itds.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Issues and validates HS-signed JWTs.
 *
 * <p>The signing secret is injected from {@code safewalk.jwt.secret} (which reads
 * the {@code JWT_SECRET} environment variable). It is never hardcoded for real use.
 * The token subject is the user id and it carries the email as a claim.</p>
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${safewalk.jwt.secret}") String secret,
            @Value("${safewalk.jwt.expiration-ms}") long expirationMs) {
        // HS256 requires a key of at least 256 bits (32 bytes).
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // Pad deterministically to satisfy the minimum without changing a valid secret.
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId, String email, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationMs, ChronoUnit.MILLIS);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role == null ? "USER" : role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public String generateToken(Long userId, String email) {
        return generateToken(userId, email, "USER");
    }

    /**
     * Parses and validates a token, returning its claims.
     *
     * @throws JwtException if the token is invalid, tampered with, or expired.
     */
    public Claims parseToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}
