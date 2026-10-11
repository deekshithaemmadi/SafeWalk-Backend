package com.anurag.itds;

import com.anurag.itds.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends AbstractApiTest {

    @Test
    void profileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required. Please sign in."));
    }

    @Test
    void getProfileReturnsOwnDataWithoutPasswordHash() throws Exception {
        User user = createUser("profile-get@test.local");
        mockMvc.perform(authed(get("/api/users/me"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value("profile-get@test.local"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void updateProfileNormalisesEmailAndPersists() throws Exception {
        User user = createUser("profile-upd@test.local");
        mockMvc.perform(authed(put("/api/users/me"), user).contentType(JSON)
                        .content("{\"name\":\"  New Name \",\"email\":\"Profile-New@Test.Local\",\"phone\":\"9123456780\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.email").value("profile-new@test.local"))
                .andExpect(jsonPath("$.phone").value("9123456780"));
        assertThat(userRepository.findById(user.getId()).orElseThrow().getEmail()).isEqualTo("profile-new@test.local");
    }

    @Test
    void updateProfileKeepingOwnEmailIsAllowed() throws Exception {
        User user = createUser("profile-same@test.local");
        mockMvc.perform(authed(put("/api/users/me"), user).contentType(JSON)
                        .content("{\"name\":\"Same\",\"email\":\"profile-same@test.local\",\"phone\":\"9123456781\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateProfileWithAnotherUsersEmailIs409() throws Exception {
        createUser("profile-taken@test.local");
        User user = createUser("profile-me@test.local");
        mockMvc.perform(authed(put("/api/users/me"), user).contentType(JSON)
                        .content("{\"name\":\"Me\",\"email\":\"profile-taken@test.local\",\"phone\":\"9123456782\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with this email already exists."));
    }

    @Test
    void updateProfileValidationErrorsListFields() throws Exception {
        User user = createUser("profile-invalid@test.local");
        mockMvc.perform(authed(put("/api/users/me"), user).contentType(JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\",\"phone\":\"12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").value("Please enter a valid email address."))
                .andExpect(jsonPath("$.fieldErrors.phone").value("Phone number must be exactly 10 digits."));
    }

    @Test
    void changePasswordThenLoginWithNewPassword() throws Exception {
        User user = createUser("pw-change@test.local");
        mockMvc.perform(authed(put("/api/users/me/password"), user).contentType(JSON)
                        .content("{\"currentPassword\":\"password1\",\"newPassword\":\"brandNew99\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"email\":\"pw-change@test.local\",\"password\":\"password1\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"email\":\"pw-change@test.local\",\"password\":\"brandNew99\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void changePasswordWithWrongCurrentPasswordIs400() throws Exception {
        User user = createUser("pw-wrong@test.local");
        mockMvc.perform(authed(put("/api/users/me/password"), user).contentType(JSON)
                        .content("{\"currentPassword\":\"nope-nope\",\"newPassword\":\"brandNew99\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is incorrect."));
    }

    @Test
    void changePasswordTooLongIs400() throws Exception {
        User user = createUser("pw-long@test.local");
        mockMvc.perform(authed(put("/api/users/me/password"), user).contentType(JSON)
                        .content("{\"currentPassword\":\"password1\",\"newPassword\":\"" + "a".repeat(73) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }
}
