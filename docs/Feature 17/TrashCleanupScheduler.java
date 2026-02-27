package com.revature.passwordmanager.scheduler;

import com.revature.passwordmanager.service.vault.VaultTrashService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrashCleanupScheduler {

  private static final Logger logger = LoggerFactory.getLogger(TrashCleanupScheduler.class);
  private final VaultTrashService vaultTrashService;

  // Run every day at midnight
  @Scheduled(cron = "0 0 0 * * ?")
  public void cleanupExpiredTrash() {
    logger.info("Running scheduled trash cleanup task...");
    vaultTrashService.cleanupExpired();
    logger.info("Trash cleanup task completed.");
  }
}
