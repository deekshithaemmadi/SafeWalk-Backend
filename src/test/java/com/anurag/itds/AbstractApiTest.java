package com.anurag.itds;

import com.anurag.itds.entity.User;
import com.anurag.itds.repository.UserRepository;
import com.anurag.itds.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Base for MockMvc integration tests. Runs the FULL application context
 * (security filter chain, JWT filter, controllers, services) against the
 * in-memory H2 test database configured in
 * {@code src/test/resources/application.properties} — never the real MySQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractApiTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    /** Inserts a user directly and returns it (id populated). */
    protected User createUser(String email) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        User user = new User(
                "Test User " + email,
                email,
                suffix, // unique 10-digit phone
                passwordEncoder.encode("password1"));
        return userRepository.save(user);
    }

    /** JWT for the given user (default role USER). */
    protected String tokenFor(User user) {
        return jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
    }

    protected RequestBuilder authed(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder, User user) {
        return builder.header("Authorization", "Bearer " + tokenFor(user));
    }

    protected static final MediaType JSON = MediaType.APPLICATION_JSON;
}
