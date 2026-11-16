package com.lapakhunian.app.core.util

import kotlin.math.pow

/** Angsuran anuitas: P * i / (1 - (1+i)^-n); bunga 0% → P/n. */
internal fun monthlyPayment(loan: Long, ratePercent: Double, years: Int): Long {
    if (loan <= 0 || years <= 0) return 0L
    val i = ratePercent / 100.0 / 12.0
    val n = years * 12.0
    val monthly = if (i <= 0.0) loan / n else loan * i / (1 - (1 + i).pow(-n))
    return monthly.toLong()
}

/** Estimasi cicilan untuk pembanding: default sama dengan KprScreen (DP 20%, 7,5%, 15 tahun). */
internal fun estimateInstallment(price: Long): Long =
    monthlyPayment(price - price / 5, 7.5, 15)