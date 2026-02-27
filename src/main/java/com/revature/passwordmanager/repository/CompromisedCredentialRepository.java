package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.security.breach.CompromisedCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompromisedCredentialRepository extends JpaRepository<CompromisedCredential, Long> {

    List<CompromisedCredential> findByUserIdAndIsResolvedFalseOrderByDetectedAtDesc(Long userId);

    List<CompromisedCredential> findByUserIdOrderByDetectedAtDesc(Long userId);

    Optional<CompromisedCredential> findByUserIdAndVaultEntryIdAndIsResolvedFalse(
            Long userId, Long vaultEntryId);

    long countByUserIdAndIsResolvedFalse(Long userId);

    boolean existsByUserIdAndVaultEntryIdAndIsResolvedFalse(Long userId, Long vaultEntryId);

    @Query("SELECT c FROM CompromisedCredential c WHERE c.user.id = :userId " +
           "AND c.isResolved = false ORDER BY c.pwnedCount DESC")
    List<CompromisedCredential> findActiveByUserIdOrderByPwnedCountDesc(@Param("userId") Long userId);

    void deleteByVaultEntryId(Long vaultEntryId);
}
