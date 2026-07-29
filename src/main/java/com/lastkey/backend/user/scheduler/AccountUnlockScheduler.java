package com.lastkey.backend.user.scheduler;

import com.lastkey.backend.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Slf4j
public class AccountUnlockScheduler {

    private final UserRepository userRepository;

    public AccountUnlockScheduler(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    /**
     * Runs every minute by default and unlocks accounts
     * whose temporary lock duration has expired.
     */
    @Scheduled(
            fixedDelayString =
                    "${app.security.account-unlock-check-delay-ms:60000}"
    )
    @Transactional
    public void unlockExpiredAccounts() {

        int unlockedAccounts =
                userRepository.unlockExpiredAccounts(
                        LocalDateTime.now()
                );

        if (unlockedAccounts > 0) {

            log.info(
                    "Automatically unlocked {} expired account(s)",
                    unlockedAccounts
            );
        }
    }
}