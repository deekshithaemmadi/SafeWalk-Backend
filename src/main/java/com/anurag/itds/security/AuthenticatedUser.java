package com.anurag.itds.security;

/**
 * Lightweight principal placed into the Spring Security context after a valid JWT
 * is verified. Controllers read this (never the raw token) to identify the caller.
 */
public record AuthenticatedUser(Long id, String email) {
}
