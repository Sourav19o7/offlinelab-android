package dev.local.androidtools.offlinelab.internal

import dev.local.androidtools.offlinelab.model.RequestMatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchEvaluatorTest {

    @Test
    fun `empty allow and deny lists mean everything is eligible`() {
        assertTrue(MatchEvaluator.isEligible("api.example.com", "/pay", "POST", emptyList(), emptyList()))
    }

    @Test
    fun `non-empty allow list restricts eligibility to matches`() {
        val allow = listOf(RequestMatcher(host = "api.example.com"))
        assertTrue(MatchEvaluator.isEligible("api.example.com", "/pay", "POST", allow, emptyList()))
        assertFalse(MatchEvaluator.isEligible("other.example.com", "/pay", "POST", allow, emptyList()))
    }

    @Test
    fun `deny list wins even if allow list also matches`() {
        val allow = listOf(RequestMatcher(host = "api.example.com"))
        val deny = listOf(RequestMatcher(host = "api.example.com", pathPrefix = "/health"))

        assertFalse(MatchEvaluator.isEligible("api.example.com", "/health/ping", "GET", allow, deny))
        assertTrue(MatchEvaluator.isEligible("api.example.com", "/pay", "POST", allow, deny))
    }

    @Test
    fun `matcher fields are ANDed together`() {
        val matcher = RequestMatcher(host = "api.example.com", pathPrefix = "/pay", method = "POST")
        assertTrue(matcher.matches("api.example.com", "/pay/checkout", "POST"))
        assertFalse(matcher.matches("api.example.com", "/pay/checkout", "GET"))
        assertFalse(matcher.matches("api.example.com", "/search", "POST"))
    }
}
