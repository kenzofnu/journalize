package dev.fnukenzo.journalize.journal.dto;

import java.time.LocalDateTime;

import dev.fnukenzo.journalize.journal.JournalEntry;

public record EntryResponse(Long id, String content, String mood, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static EntryResponse from(JournalEntry entry) {
        return new EntryResponse(entry.getId(), entry.getContent(), entry.getMood(), entry.getCreatedAt(), entry.getUpdatedAt());
    }
    
}
