package com.sapuseven.ya2fa

import java.nio.ByteBuffer
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TokenCalculator {
    val TOTP_DEFAULT_PERIOD = 30
    val TOTP_DEFAULT_DIGITS = 6
    val HOTP_INITIAL_COUNTER = 1
    val STEAM_DEFAULT_DIGITS = 5
    val DEFAULT_ALGORITHM = HashAlgorithm.SHA1
    private val STEAMCHARS = "23456789BCDFGHJKMNPQRTVWXY".toCharArray()

    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
    private fun generateHash(algorithm: HashAlgorithm, key: ByteArray, data: ByteArray): ByteArray {
        val algo = "Hmac$algorithm"

        val mac = Mac.getInstance(algo)
        mac.init(SecretKeySpec(key, algo))

        return mac.doFinal(data)
    }

    fun TOTP_RFC6238(
        secret: ByteArray,
        period: Int,
        time: Long,
        digits: Int,
        algorithm: HashAlgorithm
    ): Int {
        val fullToken = TOTP(secret, period, time, algorithm)
        val div = Math.pow(10.0, digits.toDouble()).toInt()

        return fullToken % div
    }

    fun TOTP_RFC6238(
        secret: ByteArray,
        period: Int,
        digits: Int,
        algorithm: HashAlgorithm
    ): String {
        return Tools.formatTokenString(
            TOTP_RFC6238(
                secret,
                period,
                System.currentTimeMillis() / 1000,
                digits,
                algorithm
            ), digits
        )
    }

    fun TOTP_Steam(secret: ByteArray, period: Int, digits: Int, algorithm: HashAlgorithm): String {
        var fullToken = TOTP(secret, period, System.currentTimeMillis() / 1000, algorithm)

        val tokenBuilder = StringBuilder()

        for (i in 0 until digits) {
            tokenBuilder.append(STEAMCHARS[fullToken % STEAMCHARS.size])
            fullToken /= STEAMCHARS.size
        }

        return tokenBuilder.toString()
    }

    fun HOTP(secret: ByteArray, counter: Long, digits: Int, algorithm: HashAlgorithm): String {
        val fullToken = HOTP(secret, counter, algorithm)
        val div = Math.pow(10.0, digits.toDouble()).toInt()

        return Tools.formatTokenString(fullToken % div, digits)
    }

    private fun TOTP(key: ByteArray, period: Int, time: Long, algorithm: HashAlgorithm): Int {
        return HOTP(key, time / period, algorithm)
    }

    private fun HOTP(key: ByteArray, counter: Long, algorithm: HashAlgorithm): Int {
        val data = ByteBuffer.allocate(8).putLong(counter).array()
        return generateHash(algorithm, key, data).truncate()
    }

    private fun ByteArray.truncate(): Int {
        val offset = (this[this.size - 1].toInt() and 0xf)
        return (this[offset].toInt() and 0x7f shl 24
                or (this[offset + 1].toInt() and 0xff shl 16)
                or (this[offset + 2].toInt() and 0xff shl 8)
                or (this[offset + 3].toInt() and 0xff))
    }

    enum class HashAlgorithm {
        SHA1, SHA256, SHA512
    }
}
