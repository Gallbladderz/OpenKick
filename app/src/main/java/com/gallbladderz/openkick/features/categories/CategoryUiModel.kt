/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.categories

data class CategoryUiModel(
    val id: String,
    val name: String,
    val slug: String,
    val viewers: Int,
    val bannerUrl: String,
    val tags: List<String> = emptyList()
)