package com.sapuseven.ya2fa.data

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Token(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "secret") val secret: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "issuer") val issuer: String? = null,
    @ColumnInfo(name = "type") val type: String = "totp", // TODO: Enumerate TokenTypes
    @ColumnInfo(name = "algorithm") val algorithm: String = "SHA1", // TODO: Enumerate TokenAlgorithms
    @ColumnInfo(name = "length") val length: Int = 6,
    @ColumnInfo(name = "period") val period: Int? = 30 // period for TOTP tokens in seconds
) {
    companion object {
        @Throws(InvalidUriException::class)
        fun fromUrl(uri: Uri): Token {
            if (uri.scheme != "otpauth") throw InvalidUriException("invalid scheme")

            val uriPath = uri.path

            return Token(
                type = uri.host ?: throw InvalidUriException("missing type"),
                label = with(uriPath ?: throw InvalidUriException("missing label")) {
                    substring(indexOf(":") + 1)
                },
                issuer = uri.getQueryParameter("issuer"),
                secret = uri.getQueryParameter("secret") ?: throw InvalidUriException("missing secret"),
                algorithm = uri.getQueryParameter("algorithm") ?: "SHA1",
                length = uri.getQueryParameter("digits")?.toIntOrNull() ?: 6,
                period = uri.getQueryParameter("period")?.toIntOrNull() ?: 30
            )
        }

        class InvalidUriException(s: String) : Exception(s)
    }
}
