// SPDX-FileCopyrightText: 2022 Sveriges Television AB
//
// SPDX-License-Identifier: EUPL-1.2

package se.svt.oss.encore.model

data class Progress(
    val progress: Int,
    val totalCpuTimeMillis: Long?,
    val currentCpuUsage: Int?,
    val timestamp: Long = System.currentTimeMillis()
)
