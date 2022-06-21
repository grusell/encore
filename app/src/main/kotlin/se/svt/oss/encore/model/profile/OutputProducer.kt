// SPDX-FileCopyrightText: 2020 Sveriges Television AB
//
// SPDX-License-Identifier: EUPL-1.2

package se.svt.oss.encore.model.profile

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import se.svt.oss.encore.config.AudioMixPreset
import se.svt.oss.encore.model.EncoreJob
import se.svt.oss.encore.api.output.Output


interface OutputProducer {
    fun getOutput(job: EncoreJob, audioMixPresets: Map<String, AudioMixPreset>): Output?
}
