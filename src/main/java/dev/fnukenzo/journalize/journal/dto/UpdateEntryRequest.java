package dev.fnukenzo.journalize.journal.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateEntryRequest(@NotBlank String content){
    
}
