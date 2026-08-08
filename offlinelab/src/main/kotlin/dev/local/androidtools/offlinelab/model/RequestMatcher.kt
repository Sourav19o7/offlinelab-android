package dev.local.androidtools.offlinelab.model

/**
 * Matches requests by host/path-prefix/method. Any `null` field matches everything for that
 * dimension — `RequestMatcher(host = "api.example.com")` matches every method and path on that
 * host. All non-null fields must match (AND, not OR).
 */
data class RequestMatcher(
    val host: String? = null,
    val pathPrefix: String? = null,
    val method: String? = null,
) {
    fun matches(host: String, path: String, method: String): Boolean {
        if (this.host != null && !this.host.equals(host, ignoreCase = true)) return false
        if (this.pathPrefix != null && !path.startsWith(this.pathPrefix)) return false
        if (this.method != null && !this.method.equals(method, ignoreCase = true)) return false
        return true
    }
}
