package dev.fnukenzo.journalize.journal;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.web.bind.annotation.RequestParam;

import dev.fnukenzo.journalize.ai.EmbeddingService;
import dev.fnukenzo.journalize.ai.MoodService;
import dev.fnukenzo.journalize.journal.dto.CreateEntryRequest;
import dev.fnukenzo.journalize.journal.dto.EntryResponse;
import dev.fnukenzo.journalize.journal.dto.UpdateEntryRequest;
import dev.fnukenzo.journalize.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/entries")
@RequiredArgsConstructor
public class JournalEntryController {

    private final JournalEntryRepository entryRepository;
    private final MoodService moodService;
    private final EmbeddingService embeddingService;

    @PostMapping
    public ResponseEntity<EntryResponse> create(@Valid @RequestBody CreateEntryRequest request,
            @AuthenticationPrincipal User user) {

        JournalEntry journal = new JournalEntry();
        journal.setContent(request.content());
        journal.setUser(user);
        journal.setMood(moodService.detectMood(request.content()));
        journal.setEmbedding(EmbeddingService.serialize(embeddingService.embed(request.content())));

        JournalEntry saved = entryRepository.save(journal);

        return ResponseEntity.status(HttpStatus.CREATED).body(EntryResponse.from(saved));

    }

    @GetMapping
    public List<EntryResponse> list(@AuthenticationPrincipal User user) {
        return entryRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(EntryResponse::from)
                .toList();
    }

    @GetMapping("/search")
    public List<EntryResponse> search(@RequestParam("q") String q, @AuthenticationPrincipal User user) {
        float[] queryVector = embeddingService.embed(q);
        List<JournalEntry> entries = entryRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (queryVector == null) {
            return List.of();
        }

        record Scored(JournalEntry entry, double score) {
        }

        return entries.stream()
                .filter(e -> e.getEmbedding() != null && !e.getEmbedding().isBlank())
                .map(e -> new Scored(e, cosineSimilarity(queryVector, EmbeddingService.parse(e.getEmbedding()))))
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(10)
                .map(s -> EntryResponse.from(s.entry()))
                .toList();
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return -1;
        }
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return -1;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    @GetMapping("/{id}")
    public EntryResponse get(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return EntryResponse.from(findEntry(id, user));

    }

    @PutMapping("/{id}")
    public EntryResponse update(@PathVariable Long id, @Valid @RequestBody UpdateEntryRequest request,
            @AuthenticationPrincipal User user) {

        JournalEntry entry = findEntry(id, user);
        entry.setContent(request.content());

        JournalEntry saved = entryRepository.save(entry);

        return EntryResponse.from(saved);

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {

        entryRepository.delete(findEntry(id, user));
        return ResponseEntity.noContent().build();
    }

    private JournalEntry findEntry(Long id, User user) {
        JournalEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found"));

        if (!entry.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden user");
        }

        return entry;
    }

}
