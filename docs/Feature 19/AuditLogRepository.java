package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.security.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

  List<AuditLog> findByUserIdOrderByTimestampDesc(Long userId);
}
