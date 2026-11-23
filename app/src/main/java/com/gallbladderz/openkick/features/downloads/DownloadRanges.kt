/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.downloads

internal fun parseDownloadTime(text: String): Long? {
    val fields = text.trim().split(':')
    if (fields.size !in 2..3 || fields.any { it.isEmpty() || it.any { digit -> digit !in '0'..'9' } }) {
        return null
    }
    if (fields.last().length != 2 || (fields.size == 3 && fields[1].length != 2)) return null
    val values = fields.map { it.toLongOrNull() ?: return null }
    if (values.last() >= 60 || (values.size == 3 && values[1] >= 60)) return null
    return runCatching {
        values.reduce { total, value -> Math.addExact(Math.multiplyExact(total, 60L), value) }
    }.getOrNull()
}

internal fun downloadSegmentIndices(
    durationsUs: List<Long>,
    startSec: Long?,
    endSec: Long?
): List<Int> {
    val startUs = startSec?.let { Math.multiplyExact(it, 1_000_000L) } ?: 0L
    val endUs = endSec?.let { Math.multiplyExact(it, 1_000_000L) } ?: Long.MAX_VALUE
    require(startUs >= 0 && endUs > startUs)
    var positionUs = 0L
    return buildList {
        durationsUs.forEachIndexed { index, durationUs ->
            val next = positionUs + durationUs
            if (durationUs > 0 && positionUs < endUs && next > startUs) add(index)
            positionUs = next
        }
    }
}
