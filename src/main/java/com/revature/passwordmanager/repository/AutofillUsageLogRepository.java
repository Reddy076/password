package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.autofill.AutofillUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Feature 36 – Smart Password Autofill (Backend API).
 */
@Repository
public interface AutofillUsageLogRepository extends JpaRepository<AutofillUsageLog, Long> {

    List<AutofillUsageLog> findByUserIdOrderByUsedAtDesc(Long userId);

    /** Distinct domains used by a user, ordered by most recently used. */
    @Query("SELECT l.domain FROM AutofillUsageLog l WHERE l.user.id = :userId " +
           "GROUP BY l.domain ORDER BY MAX(l.usedAt) DESC")
    List<String> findDistinctDomainsByUserId(@Param("userId") Long userId);

    /** Count how many times a domain was used by a user. */
    long countByUserIdAndDomain(Long userId, String domain);
}
