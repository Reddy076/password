package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.ai.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Feature 41 – AI Password Assistant.
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    /** Find the active chat session for a user (one per user). */
    Optional<ChatSession> findByUserId(Long userId);
}
