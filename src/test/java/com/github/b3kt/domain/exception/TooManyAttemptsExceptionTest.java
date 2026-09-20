package com.github.b3kt.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TooManyAttemptsException Tests")
class TooManyAttemptsExceptionTest {

    @Test
    @DisplayName("The wait is carried for the Retry-After header")
    void retainsRetryAfterSeconds() {
        assertEquals(900L, new TooManyAttemptsException(900L).getRetryAfterSeconds());
    }

    /**
     * The message rounds up, so a user is never told to wait less than they actually have to.
     */
    @ParameterizedTest(name = "{0}s is reported as {1} minute(s)")
    @CsvSource({
            "0, 1",
            "1, 1",
            "59, 1",
            "60, 1",
            "61, 2",
            "120, 2",
            "121, 3",
            "900, 15"
    })
    @DisplayName("Seconds are rounded up to whole minutes")
    void roundsUpToMinutes(long seconds, long minutes) {
        assertTrue(new TooManyAttemptsException(seconds).getMessage()
                        .contains("Try again in " + minutes + " minute(s)"),
                new TooManyAttemptsException(seconds).getMessage());
    }

    @Test
    @DisplayName("A non-positive wait still reads as a minute rather than zero or negative")
    void neverReportsZeroMinutes() {
        assertTrue(new TooManyAttemptsException(0).getMessage().contains("1 minute(s)"));
        assertTrue(new TooManyAttemptsException(-30).getMessage().contains("1 minute(s)"));
        assertEquals(-30L, new TooManyAttemptsException(-30).getRetryAfterSeconds());
    }

    @Test
    @DisplayName("It is unchecked, so services need not declare it")
    void isUnchecked() {
        assertInstanceOf(RuntimeException.class, new TooManyAttemptsException(60));
    }
}
