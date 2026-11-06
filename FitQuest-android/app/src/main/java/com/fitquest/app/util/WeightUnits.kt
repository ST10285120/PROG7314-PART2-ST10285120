package com.fitquest.app.util

import java.util.Locale

/**
 * Converts between the user's unit and kilograms. Everything is stored and sent in kg,
 * so changing the setting changes what is displayed and never alters saved data.
 */
object WeightUnits {

    const val POUNDS = "LB"
    private const val LB_PER_KG = 2.2046226218
    private const val COMPACT_FROM = 10_000.0

    fun label(units: String): String = if (units == POUNDS) "lb" else "kg"

    fun fromKg(weightKg: Double, units: String): Double =
        if (units == POUNDS) weightKg * LB_PER_KG else weightKg

    fun toKg(weight: Double, units: String): Double =
        if (units == POUNDS) weight / LB_PER_KG else weight

    /** "60", or "62.5", without trailing zeros. */
    fun number(weightKg: Double, units: String): String {
        val value = fromKg(weightKg, units)
        val rounded = Math.round(value * 10) / 10.0
        return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
        else String.format(Locale.getDefault(), "%.1f", rounded)
    }

    fun format(weightKg: Double, units: String): String = "${number(weightKg, units)} ${label(units)}"

    /** A short total for tight spaces, such as "12.5k kg" instead of "12500 kg". */
    fun compact(weightKg: Double, units: String): String {
        val value = fromKg(weightKg, units)
        if (value < COMPACT_FROM) return format(weightKg, units)
        return String.format(Locale.getDefault(), "%.1fk %s", value / 1000, label(units))
    }

    /** Parses typed input. Accepts a comma, which some keyboards use for decimals. */
    fun parse(input: String): Double? = input.trim().replace(',', '.').toDoubleOrNull()
}
