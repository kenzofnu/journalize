package dev.fnukenzo.journalize.journal;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry,Long> {

    List<JournalEntry> findByUserIdOrderByCreatedAtDesc(Long userId);
    
}
