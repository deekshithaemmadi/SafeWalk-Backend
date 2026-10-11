package com.anurag.itds.controller;

import com.anurag.itds.dto.ChangePasswordRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.anurag.itds.dto.UpdateProfileRequest;
import com.anurag.itds.dto.UserResponse;
import com.anurag.itds.security.CurrentUserProvider;
import com.anurag.itds.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The authenticated user's own profile. The user id always comes from the JWT
 * principal — never from the client.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    public UserController(UserService userService, CurrentUserProvider currentUserProvider) {
        this.userService = userService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/me")
    public UserResponse me() {
        return userService.getProfile(currentUserProvider.requireCurrentUserId());
    }

    @PutMapping("/me")
    public UserResponse updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(currentUserProvider.requireCurrentUserId(), request);
    }

    /** Changes the signed-in user's password. Existing JWTs stay valid until they expire. */
    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUserProvider.requireCurrentUserId(), request);
    }
}
