package com.anurag.itds;

import com.anurag.itds.entity.User;
import com.anurag.itds.repository.EmergencyContactRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Consistent ErrorResponse bodies for framework-level and infrastructure failures. */
class ErrorHandlingTest extends AbstractApiTest {

    @MockitoBean
    private EmergencyContactRepository contactRepository;

    @Test
    void malformedJsonIs400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(JSON).content("{\"email\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed JSON."))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void missingBodyIs400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed JSON."));
    }

    @Test
    void wrongContentTypeIs415() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("Content-Type must be application/json."));
    }

    @Test
    void unsupportedMethodIs405() throws Exception {
        mockMvc.perform(delete("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void unknownEndpointForAuthenticatedUserIs404() throws Exception {
        User user = createUser("err-404@test.local");
        mockMvc.perform(authed(get("/api/does-not-exist"), user))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Endpoint not found."));
    }

    @Test
    void databaseFailureIs503WithoutLeakingDetails() throws Exception {
        User user = createUser("err-db@test.local");
        when(contactRepository.findByUserIdOrderByCreatedAtAsc(anyLong()))
                .thenThrow(new DataAccessResourceFailureException("Communications link failure to db-host:3306"));
        mockMvc.perform(authed(get("/api/emergency-contacts"), user))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("The database is temporarily unavailable. Please try again later."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void unexpectedExceptionIs500WithGenericMessage() throws Exception {
        User user = createUser("err-500@test.local");
        when(contactRepository.findByUserIdOrderByCreatedAtAsc(anyLong()))
                .thenThrow(new IllegalStateException("secret internal detail"));
        mockMvc.perform(authed(get("/api/emergency-contacts"), user))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again."));
    }
}
