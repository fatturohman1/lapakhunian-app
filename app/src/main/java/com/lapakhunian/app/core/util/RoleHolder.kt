package com.lapakhunian.app.core.util

/**
 * Cache role user saat ini — diset SessionManager saat login/logout.
 * Praktis untuk UI yang butuh role sinkron (wizard listedBy, dll).
 */
object RoleHolder {
    var role: String = "owner"
        internal set

    fun set(role: String?) {
        this.role = role ?: "owner"
    }
}
