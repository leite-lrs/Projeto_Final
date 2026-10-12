package com.flashsale.checkout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ApiKeySecurityFilterTest {
    @Test
    void rejectsUnconfiguredKeys() {
        assertFalse(ApiKeySecurityFilter.isValidApiKey("secret", null));
        assertFalse(ApiKeySecurityFilter.isValidApiKey("secret", ""));
        assertFalse(ApiKeySecurityFilter.isValidApiKey("secret", "unset"));
        assertFalse(ApiKeySecurityFilter.isValidApiKey("secret", "change-me-in-production"));
    }

    @Test
    void acceptsConfiguredKey() {
        assertTrue(ApiKeySecurityFilter.isValidApiKey("secret", "secret"));
    }

    @Test
    void requiresAuthenticationWhenExplicitlyEnabled() {
        assertTrue(ApiKeySecurityFilter.shouldRequireAuthentication(true, "dev"));
    }

    @Test
    void requiresAuthenticationInProduction() {
        assertTrue(ApiKeySecurityFilter.shouldRequireAuthentication(false, "prod"));
    }

    @Test
    void allowsAnonymousRequestsOutsideProductionWhenNotConfigured() {
        assertFalse(ApiKeySecurityFilter.shouldRequireAuthentication(false, "dev"));
    }
}
