package com.anurag.itds.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-0123456789";

    @Test
    void generatedTokenRoundTripsClaims() {
        JwtService jwt = new JwtService(SECRET, 60_000);
        Claims claims = jwt.parseToken(jwt.generateToken(42L, "a@b.c", "MODERATOR"));
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("a@b.c");
        assertThat(claims.get("role", String.class)).isEqualTo("MODERATOR");
    }

    @Test
    void tokenSignedWithDifferentSecretIsInvalid() {
        String token = new JwtService("another-secret-another-secret-0123456789", 60_000).generateToken(1L, "x@y.z", "USER");
        assertThat(new JwtService(SECRET, 60_000).isValid(token)).isFalse();
    }

    @Test
    void expiredTokenIsInvalid() {
        JwtService jwt = new JwtService(SECRET, -1_000);
        assertThat(jwt.isValid(jwt.generateToken(1L, "x@y.z", "USER"))).isFalse();
    }

    @Test
    void garbageTokenIsInvalid() {
        assertThat(new JwtService(SECRET, 60_000).isValid("not.a.jwt")).isFalse();
    }

    @Test
    void shortOrBlankSecretFailsFast() {
        assertThatThrownBy(() -> new JwtService("too-short", 60_000)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService("", 60_000)).isInstanceOf(IllegalStateException.class);
    }
}
