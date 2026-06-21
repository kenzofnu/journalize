package dev.fnukenzo.journalize.journal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.fnukenzo.journalize.AbstractIntegrationTest;
import dev.fnukenzo.journalize.ai.EmbeddingService;
import dev.fnukenzo.journalize.ai.MoodService;
import dev.fnukenzo.journalize.user.UserRepository;

/**
 * End-to-end tests for the journal entry endpoints, with a focus on the security
 * guarantees: endpoints require a valid JWT, and users can only ever see or touch
 * their own entries.
 */
class JournalEntryControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JournalEntryRepository entryRepository;

    // Replace the real Gemini-calling services with mocks so tests never hit the network
    @MockitoBean
    private MoodService moodService;

    @MockitoBean
    private EmbeddingService embeddingService;

    @BeforeEach
    void cleanDatabase() {
        entryRepository.deleteAll();
        userRepository.deleteAll();
    }

    /** Registers a user and logs in, returning a usable JWT. */
    private String registerAndLogin(String username, String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", username, "email", email, "password", password))))
                .andExpect(status().isCreated());

        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    /** Creates an entry as the given user and returns its id. */
    private long createEntry(String token, String content) throws Exception {
        String response = mockMvc.perform(post("/api/entries")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", content))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void createEntry_withoutToken_isRejected() throws Exception {
        mockMvc.perform(post("/api/entries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "no token here"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createEntry_withToken_returns201() throws Exception {
        String token = registerAndLogin("alice", "alice@test.com", "password123");

        mockMvc.perform(post("/api/entries")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "my first entry"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("my first entry"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void listEntries_returnsOnlyTheCallersOwnEntries() throws Exception {
        String aliceToken = registerAndLogin("alice", "alice@test.com", "password123");
        String bobToken = registerAndLogin("bob", "bob@test.com", "password123");

        createEntry(aliceToken, "alice entry 1");
        createEntry(aliceToken, "alice entry 2");

        // Alice sees her two entries
        mockMvc.perform(get("/api/entries")
                .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Bob sees none of Alice's entries
        mockMvc.perform(get("/api/entries")
                .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getEntry_belongingToAnotherUser_returns403() throws Exception {
        String aliceToken = registerAndLogin("alice", "alice@test.com", "password123");
        String bobToken = registerAndLogin("bob", "bob@test.com", "password123");

        long aliceEntryId = createEntry(aliceToken, "alice's private thoughts");

        // Bob tries to read Alice's entry by id -> forbidden
        mockMvc.perform(get("/api/entries/" + aliceEntryId)
                .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getEntry_thatDoesNotExist_returns404() throws Exception {
        String token = registerAndLogin("alice", "alice@test.com", "password123");

        mockMvc.perform(get("/api/entries/999999")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
