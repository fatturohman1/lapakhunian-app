package com.lapakhunian.app.core.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Format Rupiah gaya web (Intl.NumberFormat id-ID).
 * formatRupiah(1250000000) = "Rp 1.250.000.000"
 * Pakai non-breaking space agar "Rp" dan nominal tidak terpotong ke baris lain di kartu sempit.
 */
fun formatRupiah(value: Long, includeRp: Boolean = true): String {
    val nf = NumberFormat.getIntegerInstance(Locale("id", "ID"))
    val text = nf.format(value)
    return if (includeRp) "Rp\u00A0$text" else text
}

/**
 * Format ringkas untuk kartu/CTA.
 * compactRupiah(1250000000) = "Rp 1,25 M"; compactRupiah(450000000) = "Rp 450 jt"
 */
fun compactRupiah(value: Long): String {
    val rp = "Rp\u00A0"
    return when {
        value >= 1_000_000_000L -> {
            val m = value / 1_000_000.0 / 1000.0
            rp + trimZero(m) + " M"
        }
        value >= 1_000_000L -> {
            val jt = value / 1_000_000.0
            rp + trimZero(jt) + " jt"
        }
        else -> formatRupiah(value)
    }
}

/**
 * 0812… / 62812… / +62812… → 62812… untuk link wa.me
 */
fun toWaNumber(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return when {
        digits.startsWith("62") -> digits
        digits.startsWith("8") -> "62$digits"
        digits.startsWith("0") -> "62" + digits.drop(1)
        else -> digits
    }
}

private fun trimZero(v: Double): String {
    val rounded = (v * 100).toLong() / 100.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        java.text.DecimalFormat("0.##", java.text.DecimalFormatSymbols(Locale("id", "ID"))).format(rounded)
    }
}
