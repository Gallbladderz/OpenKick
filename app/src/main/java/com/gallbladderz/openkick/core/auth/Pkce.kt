/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class PkceRequest(val verifier: String, val challenge: String, val state: String)

object Pkce {
    private val random = SecureRandom()

    fun create(): PkceRequest {
        val verifier = randomUrlSafe(32)
        return PkceRequest(verifier, challenge(verifier), randomUrlSafe(32))
    }

    fun challenge(verifier: String): String {
        require(verifier.length in 43..128 && verifier.all { it.isLetterOrDigit() || it in "-._~" }) {
            "Invalid PKCE verifier"
        }
        return encode(
            MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        )
    }

    private fun randomUrlSafe(size: Int): String =
        ByteArray(size).also(random::nextBytes).let(::encode)

    private fun encode(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
