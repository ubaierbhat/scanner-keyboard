package org.ubaierbhat.android.barcodekeyboard.scanner

class ScanThrottle(private val nowMs: () -> Long = System::currentTimeMillis) {

    private var lastAllowedAt: Long? = null

    fun allow(): Boolean {
        val now = nowMs()
        val lastAllowed = lastAllowedAt
        if (lastAllowed != null && now - lastAllowed < INTERVAL_MS) {
            return false
        }
        lastAllowedAt = now
        return true
    }

    fun reset() {
        lastAllowedAt = null
    }

    private companion object {
        private const val INTERVAL_MS = 1000L
    }
}
