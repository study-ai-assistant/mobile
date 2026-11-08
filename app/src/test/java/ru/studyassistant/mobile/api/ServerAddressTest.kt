package ru.studyassistant.mobile.api

import org.junit.Assert.*
import org.junit.Test

class ServerAddressTest {
    @Test fun acceptsLanAndTrimsSlash() {
        assertEquals("http://192.168.1.10:8080", normalizeServerAddress(" http://192.168.1.10:8080/ ", true))
        assertEquals("https://study.example", normalizeServerAddress("https://study.example", false))
        assertEquals("http://[::1]:8080", normalizeServerAddress("http://[::1]:8080", true))
    }
    @Test fun rejectsHttpInRelease() {
        assertThrows(IllegalArgumentException::class.java) { normalizeServerAddress("http://192.168.1.10:8080", false) }
    }
    @Test fun rejectsAmbiguousAndMalformedAddresses() {
        listOf("", "localhost:8080", "http://", "ftp://example.com", "http://example.com/health",
            "http://u:p@example.com", "http://example.com?key=secret", "http://example.com#x",
            "http://example.com:0", "http://example.com:65536", "http://example.com:", "http://bad host").forEach {
            assertThrows(it, IllegalArgumentException::class.java) { normalizeServerAddress(it, true) }
        }
    }
}
