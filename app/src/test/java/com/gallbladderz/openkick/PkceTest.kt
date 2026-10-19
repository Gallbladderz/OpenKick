/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick

import com.gallbladderz.openkick.core.auth.Pkce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PkceTest {
    @Test fun challengeMatchesRfc7636Example() {
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            Pkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
        )
    }

    @Test fun newRequestHasIndependentUrlSafeValues() {
        val first = Pkce.create()
        val second = Pkce.create()
        assertTrue(first.verifier.length in 43..128)
        assertTrue(first.verifier.matches(Regex("[A-Za-z0-9_-]+")))
        assertEquals(Pkce.challenge(first.verifier), first.challenge)
        assertNotEquals(first.state, first.verifier)
        assertNotEquals(first.state, second.state)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsShortVerifier() {
        Pkce.challenge("short")
    }
}
