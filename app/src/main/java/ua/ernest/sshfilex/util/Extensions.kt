package ua.ernest.sshfilex.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

fun Long.humanReadable(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val group = (log10(toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#", DecimalFormatSymbols(Locale.US))
        .format(this / 1024.0.pow(group.toDouble())) + " " + units[group]
}
