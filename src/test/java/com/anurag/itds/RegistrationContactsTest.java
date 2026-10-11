package com.anurag.itds;

import com.anurag.itds.repository.EmergencyContactRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Emergency contacts submitted together with registration. */
class RegistrationContactsTest extends AbstractApiTest {

    @Autowired
    private EmergencyContactRepository contactRepository;

    private static String body(String email, String phone, String contactsJson) {
        return """
                {"name":"Reg User","email":"%s","phone":"%s","password":"secret123","emergencyContacts":%s}
                """.formatted(email, phone, contactsJson);
    }

    private static String email() {
        return "reg-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void contactsArePersistedAndBlankRowsIgnored() throws Exception {
        String email = email();
        mockMvc.perform(post("/api/auth/register").contentType(JSON).content(body(email, "9811100001", """
                        [{"name":"Mom","phone":"9811100002","relation":"Mother"},
                         {"name":" Dad ","phone":"9811100003","relation":"Father"},
                         {"name":"","phone":"","relation":""}]""")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.emergencyContacts.length()").value(2))
                .andExpect(jsonPath("$.user.emergencyContacts[1].name").value("Dad"));
        Long userId = userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();
        assertThat(contactRepository.countByUserId(userId)).isEqualTo(2);
    }

    @Test
    void partiallyFilledContactIs400AndNothingIsSaved() throws Exception {
        String email = email();
        mockMvc.perform(post("/api/auth/register").contentType(JSON).content(body(email, "9811100011",
                        "[{\"name\":\"Mom\",\"phone\":\"\",\"relation\":\"Mother\"}]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Each emergency contact needs a name, phone number and relation."));
        assertThat(userRepository.findByEmailIgnoreCase(email)).isEmpty();
    }

    @Test
    void invalidContactPhoneIs400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(JSON).content(body(email(), "9811100021",
                        "[{\"name\":\"Mom\",\"phone\":\"12ab\",\"relation\":\"Mother\"}]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Emergency contact phone number must be exactly 10 digits."));
    }

    @Test
    void duplicateContactPhonesAre400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(JSON).content(body(email(), "9811100031", """
                        [{"name":"A","phone":"9811100032","relation":"Friend"},
                         {"name":"B","phone":"9811100032","relation":"Friend"}]""")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passwordLongerThanBcryptLimitIs400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(JSON).content("""
                        {"name":"Long","email":"%s","phone":"9811100041","password":"%s"}
                        """.formatted(email(), "p".repeat(73))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").value("Password must be between 6 and 72 characters."));
    }
}
