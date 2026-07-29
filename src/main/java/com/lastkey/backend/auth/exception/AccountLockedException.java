package com.lastkey.backend.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;

@ResponseStatus(HttpStatus.LOCKED)
public class AccountLockedException
        extends RuntimeException {

    private final LocalDateTime lockedUntil;

    public AccountLockedException(
            String message,
            LocalDateTime lockedUntil
    ) {
        super(message);
        this.lockedUntil = lockedUntil;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }
}