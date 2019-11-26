package com.sapuseven.ya2fa

data class TokenEntry(
    val secret: String,
    val label: String,
    val issuer: String
)
