package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.security.breach.BreachScanRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BreachScanRecordRepository extends JpaRepository<BreachScanRecord, Long> {

    List<BreachScanRecord> findByUserIdOrderByScannedAtDesc(Long userId);

    Optional<BreachScanRecord> findTopByUserIdOrderByScannedAtDesc(Long userId);

    List<BreachScanRecord> findTop10ByUserIdOrderByScannedAtDesc(Long userId);
}
