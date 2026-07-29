package com.lastkey.backend.email.scheduler;

import com.lastkey.backend.email.repository.OtpVerificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
public class OtpCleanupScheduler {

    /*
     * Used OTP records will remain for this duration before deletion.
     */
    private static final int USED_OTP_RETENTION_HOURS = 24;

    private final OtpVerificationRepository otpRepository;

    public OtpCleanupScheduler(
            OtpVerificationRepository otpRepository
    ) {
        this.otpRepository = otpRepository;
    }

    /*
     * Runs every hour.
     *
     * fixedDelay means the next execution starts one hour after
     * the previous execution has completed.
     */
    @Scheduled(
            fixedDelayString =
                    "${lastkey.otp.cleanup-delay-ms:3600000}",
            initialDelayString =
                    "${lastkey.otp.cleanup-initial-delay-ms:60000}"
    )
    @Transactional
    public void cleanupOtpRecords() {

        LocalDateTime now =
                LocalDateTime.now();

        LocalDateTime usedOtpCutoff =
                now.minusHours(
                        USED_OTP_RETENTION_HOURS
                );

        try {

            int expiredOtpCount =
                    otpRepository.deleteExpiredOtps(
                            now
                    );

            int oldUsedOtpCount =
                    otpRepository.deleteOldUsedOtps(
                            usedOtpCutoff
                    );

            if (expiredOtpCount > 0
                    || oldUsedOtpCount > 0) {

                log.info(
                        "OTP cleanup completed. Expired OTPs deleted: {}, old used OTPs deleted: {}",
                        expiredOtpCount,
                        oldUsedOtpCount
                );

            } else {

                log.debug(
                        "OTP cleanup completed. No OTP records required deletion"
                );
            }

        } catch (RuntimeException exception) {

            log.error(
                    "OTP cleanup failed",
                    exception
            );

            /*
             * Re-throwing ensures the transaction is rolled back.
             */
            throw exception;
        }
    }
}