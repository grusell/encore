package se.svt.oss.encore.model.childjob

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
data class ChildJobProgress(
    val childJobId: UUID,
    val parentId: UUID,
    val progress: Int,
    val status: String,
    val activeChildJobs: Int
)
