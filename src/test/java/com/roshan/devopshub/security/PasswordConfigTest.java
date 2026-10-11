
package com.roshan.devopshub.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordConfigTest {

    private final PasswordEncoder passwordEncoder =
            new PasswordConfig().passwordEncoder();

    @Test
    void shouldEncodePasswordWithoutStoringRawPassword() {
        String rawPassword = "DevOpsHub-Test-Password-123!";

        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
        assertFalse(passwordEncoder.matches(
                "Wrong-Password-123!", encodedPassword));
    }

    @Test
    void shouldGenerateDifferentHashesForSamePassword() {
        String rawPassword = "DevOpsHub-Test-Password-123!";

        String firstHash = passwordEncoder.encode(rawPassword);
        String secondHash = passwordEncoder.encode(rawPassword);

        assertNotEquals(firstHash, secondHash);
        assertTrue(passwordEncoder.matches(rawPassword, firstHash));
        assertTrue(passwordEncoder.matches(rawPassword, secondHash));
    }
}
