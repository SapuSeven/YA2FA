package com.sapuseven.ya2fa.utils

import java.text.NumberFormat
import java.util.*

object Tools {
    fun formatTokenString(token: Int, digits: Int): String {
        val numberFormat = NumberFormat.getInstance(Locale.ENGLISH)
        numberFormat.minimumIntegerDigits = digits
        numberFormat.isGroupingUsed = false

        return numberFormat.format(token.toLong())
    }

    fun formatToken(s: String, chunkSize: Int): String = s.chunked(chunkSize).joinToString(" ")
}
