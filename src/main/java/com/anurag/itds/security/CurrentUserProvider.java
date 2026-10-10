package com.anurag.itds.security;

import com.anurag.itds.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Convenience accessor for the currently authenticated {@link AuthenticatedUser}.
 * Controllers/services use this instead of reading the token directly.
 */
@Component
public class CurrentUserProvider {

    /**
     * @return the authenticated user principal.
     * @throws UnauthorizedException if there is no authenticated principal.
     */
    public AuthenticatedUser requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUser principal) {
            return principal;
        }
        throw new UnauthorizedException("Authentication required.");
    }

    /** @return the current user id, or throws if unauthenticated. */
    public Long requireCurrentUserId() {
        return requireCurrentUser().id();
    }
}
