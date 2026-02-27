package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.team.SharedVaultEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Feature 42 – Team/Family Vault Sharing.
 */
@Repository
public interface SharedVaultEntryRepository extends JpaRepository<SharedVaultEntry, Long> {

    List<SharedVaultEntry> findByTeamIdOrderBySharedAtDesc(Long teamId);

    Optional<SharedVaultEntry> findByTeamIdAndVaultEntryId(Long teamId, Long vaultEntryId);

    boolean existsByTeamIdAndVaultEntryId(Long teamId, Long vaultEntryId);

    void deleteByVaultEntryId(Long vaultEntryId);

    List<SharedVaultEntry> findBySharedByIdOrderBySharedAtDesc(Long userId);
}
