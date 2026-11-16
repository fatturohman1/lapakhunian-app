package com.lapakhunian.app.core.ui

private val focusedFlowBaseRoutes = setOf(
    "thread",
    "listing-wizard",
    "payment",
    "contact",
    "contact-guest",
    "login",
    "register",
    "forgot-password",
    "role-picker",
    "compare",
)

/** Alur fokus (chat, formulir, pembayaran, auth) menyembunyikan nav bawah agar tidak memecah perhatian. */
internal fun shouldShowBottomBar(route: String?): Boolean {
    val base = route?.substringBefore('?')?.substringBefore('/') ?: return true
    return base !in focusedFlowBaseRoutes
}