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

    @PostMapping
    public ResponseEntity<EntryResponse> create(@Valid @RequestBody CreateEntryRequest request,
            @AuthenticationPrincipal User user) {

        JournalEntry journal = new JournalEntry();
        journal.setContent(request.content());
        journal.setUser(user);

        JournalEntry saved = entryRepository.save(journal);

        return ResponseEntity.status(HttpStatus.CREATED).body(EntryResponse.from(saved));

    }

    @GetMapping
    public List<EntryResponse> list(@AuthenticationPrincipal User user) {
        return entryRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(EntryResponse::from)
                .toList();
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
