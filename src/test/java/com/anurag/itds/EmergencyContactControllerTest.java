package com.anurag.itds;

import com.anurag.itds.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Emergency contacts: CRUD, ownership, duplicates, and the minimum-two rule.
 */
class EmergencyContactControllerTest extends AbstractApiTest {

    private String contactJson(String name, String phone, String relation) {
        return """
                {"name":"%s","phone":"%s","relation":"%s"}
                """.formatted(name, phone, relation);
    }

    private long createContact(User user, String name, String phone, String relation) throws Exception {
        String body = mockMvc.perform(post("/api/emergency-contacts")
                        .header("Authorization", "Bearer " + tokenFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson(name, phone, relation)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new tools.jackson.databind.ObjectMapper().readTree(body).get("id").asLong();
    }

    @Test
    @DisplayName("create/list/update/delete own contact works")
    void ownCrud() throws Exception {
        User user = createUser("crud-" + java.util.UUID.randomUUID() + "@example.com");
        String token = "Bearer " + tokenFor(user);

        mockMvc.perform(post("/api/emergency-contacts")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson("Mom", "9876500100", "Mother")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Mom"));

        mockMvc.perform(get("/api/emergency-contacts").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // update (need the id from creation)
        String body = mockMvc.perform(post("/api/emergency-contacts")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson("Dad", "9876500101", "Father")))
                .andReturn().getResponse().getContentAsString();
        long id = tools.jackson.databind.JsonNode.class.cast(
                new tools.jackson.databind.ObjectMapper().readTree(body)).get("id").asLong();

        mockMvc.perform(put("/api/emergency-contacts/{id}", id)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson("Daddy", "9876500101", "Father")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Daddy"));

        // Add a third contact so the delete does not violate the minimum-of-two rule.
        mockMvc.perform(post("/api/emergency-contacts")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson("Sis", "9876500102", "Sister")))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/emergency-contacts/{id}", id)
                        .header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("minimum two contacts: delete blocked while user has <= 2 contacts")
    void cannotDropBelowMinimum() throws Exception {
        User user = createUser("min-" + java.util.UUID.randomUUID() + "@example.com");
        String token = "Bearer " + tokenFor(user);

        createContact(user, "C1", "9876500102", "Friend");
        createContact(user, "C2", "9876500103", "Friend");

        // With exactly 2, deleting either would drop below the minimum of 2.
        mockMvc.perform(get("/api/emergency-contacts").header("Authorization", token))
                .andExpect(jsonPath("$", hasSize(2)));

        String body = mockMvc.perform(get("/api/emergency-contacts").header("Authorization", token))
                .andReturn().getResponse().getContentAsString();
        long id = new tools.jackson.databind.ObjectMapper().readTree(body).get(0).get("id").asLong();

        mockMvc.perform(delete("/api/emergency-contacts/{id}", id)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("delete allowed once the user has 3+ contacts")
    void canDeleteWithThreeContacts() throws Exception {
        User user = createUser("three-" + java.util.UUID.randomUUID() + "@example.com");
        String token = "Bearer " + tokenFor(user);

        createContact(user, "C1", "9876500104", "Friend");
        createContact(user, "C2", "9876500105", "Friend");
        createContact(user, "C3", "9876500106", "Friend");

        String body = mockMvc.perform(get("/api/emergency-contacts").header("Authorization", token))
                .andReturn().getResponse().getContentAsString();
        long id = new tools.jackson.databind.ObjectMapper().readTree(body).get(0).get("id").asLong();

        mockMvc.perform(delete("/api/emergency-contacts/{id}", id)
                        .header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("duplicate phone for same user -> 409")
    void duplicatePhoneRejected() throws Exception {
        User user = createUser("dupc-" + java.util.UUID.randomUUID() + "@example.com");
        String token = "Bearer " + tokenFor(user);

        createContact(user, "C1", "9876500107", "Friend");
        mockMvc.perform(post("/api/emergency-contacts")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson("C2", "9876500107", "Friend")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("ownership: another user cannot delete your contact")
    void cannotDeleteOthersContact() throws Exception {
        User owner = createUser("owner-" + java.util.UUID.randomUUID() + "@example.com");
        User attacker = createUser("attacker-" + java.util.UUID.randomUUID() + "@example.com");

        long id = createContact(owner, "C1", "9876500108", "Friend");

        mockMvc.perform(delete("/api/emergency-contacts/{id}", id)
                        .header("Authorization", "Bearer " + tokenFor(attacker)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ownership: another user cannot update your contact")
    void cannotUpdateOthersContact() throws Exception {
        User owner = createUser("owner2-" + java.util.UUID.randomUUID() + "@example.com");
        User attacker = createUser("attacker2-" + java.util.UUID.randomUUID() + "@example.com");

        long id = createContact(owner, "C1", "9876500109", "Friend");

        mockMvc.perform(put("/api/emergency-contacts/{id}", id)
                        .header("Authorization", "Bearer " + tokenFor(attacker))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson("Hacked", "9876500109", "Friend")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("contacts are isolated per user")
    void contactsAreIsolated() throws Exception {
        User a = createUser("iso-a-" + java.util.UUID.randomUUID() + "@example.com");
        User b = createUser("iso-b-" + java.util.UUID.randomUUID() + "@example.com");

        createContact(a, "A1", "9876500110", "Friend");
        createContact(a, "A2", "9876500111", "Friend");
        createContact(b, "B1", "9876500112", "Friend");

        mockMvc.perform(get("/api/emergency-contacts").header("Authorization", "Bearer " + tokenFor(a)))
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/emergency-contacts").header("Authorization", "Bearer " + tokenFor(b)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("B1"));
    }

    @Test
    @DisplayName("validation: missing name/phone/relation -> 400")
    void contactValidation() throws Exception {
        User user = createUser("valc-" + java.util.UUID.randomUUID() + "@example.com");
        String token = "Bearer " + tokenFor(user);

        mockMvc.perform(post("/api/emergency-contacts")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","phone":"123","relation":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.phone").exists())
                .andExpect(jsonPath("$.fieldErrors.relation").exists());
    }
}

