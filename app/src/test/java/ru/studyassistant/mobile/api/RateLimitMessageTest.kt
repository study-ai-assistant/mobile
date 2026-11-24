package ru.studyassistant.mobile.api

import org.junit.Assert.*
import org.junit.Test

class RateLimitMessageTest {
    @Test fun onlyVerifiedProblemBecomesQuotaMessage() {
        val body = """{"status":502,"type":"about:blank","detail":"${ApiMessages.RATE_LIMIT}"}"""
        assertTrue(ApiMessages.knownRateLimit(502, body))
        assertEquals(ApiMessages.RATE_LIMIT, MaterialErrorMessages.message(502, body))
        assertFalse(ApiMessages.knownRateLimit(400, body))
        assertFalse(ApiMessages.knownRateLimit(502, "<html>proxy error</html>"))
        assertFalse(ApiMessages.knownRateLimit(502, body.replace("about:blank", "untrusted")))
        assertFalse(ApiMessages.knownRateLimit(502, body.replace(ApiMessages.RATE_LIMIT, "secret diagnostic")))
    }
}
