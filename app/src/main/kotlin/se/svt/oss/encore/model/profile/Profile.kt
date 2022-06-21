// SPDX-FileCopyrightText: 2020 Sveriges Television AB
//
// SPDX-License-Identifier: EUPL-1.2

package se.svt.oss.encore.model.profile

import se.svt.oss.encore.api.EncodeDto

data class Profile(
    val name: String,
    val description: String,
    val encodes: List<EncodeDto>,
    val scaling: String? = "bicubic",
    val deinterlaceFilter: String = "yadif"
)
