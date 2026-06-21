package dev.fnukenzo.journalize.journal.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateEntryRequest(@NotBlank String content) {

    
}
