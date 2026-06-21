package dev.fnukenzo.journalize.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.fnukenzo.journalize.AbstractIntegrationTest;
import dev.fnukenzo.journalize.journal.JournalEntryRepository;
import dev.fnukenzo.journalize.user.UserRepository;

/**
 * End-to-end tests for the registration and login endpoints.
 * These exercise the full stack: HTTP -> controller -> validation -> security
 * -> BCrypt -> JPA -> PostgreSQL.
 */
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JournalEntryRepository entryRepository;

    @BeforeEach
    void cleanDatabase() {
        // entries reference users (FK), so delete entries first
        entryRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String json(Map<String, String> fields) throws Exception {
        return objectMapper.writeValueAsString(fields);
    }

    @Test
    void register_withValidRequest_returns201AndNeverLeaksPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "username", "alice",
                        "email", "alice@test.com",
                        "password", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.id").exists())
                // critical: the response must NOT contain password material
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void register_withDuplicateUsername_returns409() throws Exception {
        String body = json(Map.of(
                "username", "alice",
                "email", "alice@test.com",
                "password", "password123"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        // second registration with the same username should be rejected
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void register_withInvalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "username", "alice",
                        "email", "not-an-email",
                        "password", "password123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withTooShortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "username", "alice",
                        "email", "alice@test.com",
                        "password", "short"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_withValidCredentials_returns200AndToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "username", "alice",
                        "email", "alice@test.com",
                        "password", "password123"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "username", "alice",
                        "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "username", "alice",
                        "email", "alice@test.com",
                        "password", "password123"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "username", "alice",
                        "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withNonexistentUser_returns401() throws Exception {
        // same 401 as wrong-password: no username enumeration leak
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "username", "ghost",
                        "password", "whatever123"))))
                .andExpect(status().isUnauthorized());
    }
}
